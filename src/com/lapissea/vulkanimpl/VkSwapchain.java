package com.lapissea.vulkanimpl;

import com.lapissea.util.MathUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.types.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.stream.Stream;

import static com.lapissea.util.UtilL.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain implements VkDestroyable, VkGpuCtx{
	
	public class Image{
		private int         index;
		private long        framebuffer;
		private VkTexture   colorFrame;
		private VkSemaphore imageAviable;
		
		public Image(int index, VkTexture colorFrame, VkSemaphore imageAviable){
			this.index=index;
			this.colorFrame=colorFrame;
			this.imageAviable=imageAviable;
		}
	}
	
	private       LongBuffer  handle;
	private final VkGpu       gpu;
	private       int         format;
	private       int         colorSpace;
	private final VkSurface   surface;
	private       VkSemaphore renderFinish;
	private       Image[]     images;
	
	private IntBuffer acquireNextImageMem=memAllocInt(1);
	
	private VkCommandBufferM[] frameBinds;
	
	public VkSwapchain(VkGpu gpu, VkSurface surface){
		this.surface=surface;
		this.gpu=gpu;
		try(MemoryStack stack=stackPush()){
			create(gpu, surface, stack);
		}
		renderFinish=gpu.createSemaphore();
	}
	
	private void create(VkGpu gpu, VkSurface surface, MemoryStack stack){
		
		//acquire currency
		IntBuffer count=stack.mallocInt(1);
		Vk.getPhysicalDeviceSurfacePresentModesKHR(gpu.getPhysicalDevice(), surface, count, null);
		IntBuffer presentModes=memAllocInt(count.get(0));
		
		Vk.getPhysicalDeviceSurfacePresentModesKHR(gpu.getPhysicalDevice(), surface, count, presentModes);
		
		VkSurfaceCapabilitiesKHR caps=VkSurfaceCapabilitiesKHR.callocStack(stack);
		Vk.getPhysicalDeviceSurfaceCapabilitiesKHR(gpu, surface, caps);
		
		//regard females
		int                presentMode=choosePresentMode(presentModes);
		VkSurfaceFormatKHR format     =chooseSwapSurfaceFormat(Vk.getPhysicalDeviceSurfaceFormatsKHR(gpu, stack));
		VkExtent2D         extent     =chooseSwapExtent(surface.getSize(), stack, surface.getCapabilities(gpu, stack));
		
		int imageCount=caps.minImageCount()+1;
		if(caps.maxImageCount()>0) imageCount=Math.min(imageCount, caps.maxImageCount());
		
		
		this.format=format.format();
		colorSpace=format.colorSpace();
		
		VkSwapchainCreateInfoKHR createInfo=VkSwapchainCreateInfoKHR.callocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
		          .surface(surface.handle)
		          .minImageCount(imageCount)
		          .imageFormat(format.format())
		          .imageColorSpace(format.colorSpace())
		          .imageExtent(extent)
		          .imageArrayLayers(1)
		          .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
		
		int graphics=gpu.getGraphicsQueue().id, present=gpu.getSurfaceQueue().id;
		
		if(graphics!=present){
			createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
			          .pQueueFamilyIndices(stack.ints(graphics, present));
		}else{
			createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
		}
		
		createInfo.preTransform(caps.currentTransform())
		          .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
		          .presentMode(presentMode)
		          .clipped(true)
		          .oldSwapchain(VK_NULL_HANDLE);
		
		handle=memAllocLong(1);
		Vk.createSwapchainKHR(gpu, createInfo, handle);
		
		VkTexture[] colors=UtilL.convert(Vk.getSwapchainImagesKHR(gpu, this, stack), VkTexture[]::new,
		                                 image->new VkTexture(image).createView(format.format(), VkImageAspect.COLOR));
		images=new Image[colors.length];
		for(int i=0;i<colors.length;i++){
			images[i]
		}
	}
	
	
	public void initFrameBuffers(VkRenderPass renderPass){
		
		try(MemoryStack stack=stackPush()){
			VkFramebufferCreateInfo framebufferInfo=VkFramebufferCreateInfo.callocStack(stack);
			framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
			               .renderPass(renderPass.getHandle())
			               .width(surface.getSize().x())
			               .height(surface.getSize().y())
			               .layers(1);
			
			LongBuffer lb=stack.mallocLong(1), view=stack.mallocLong(1);
			framebuffers=Stream.of(colorFrames)
			                   .mapToLong(c->Vk.createFrameBuffer(gpu, framebufferInfo.pAttachments(view.put(0, c.getView())), lb))
			                   .toArray();
			
		}
	}
	
	
	private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats){
		if(availableFormats.capacity()==1&&availableFormats.position(0).format()==VK_FORMAT_UNDEFINED) return availableFormats.get(0);
		
		for(int i=0;i<availableFormats.capacity();i++){
			VkSurfaceFormatKHR format=availableFormats.get(i);
			
			int f=format.format();
			if((f==VK_FORMAT_B8G8R8A8_UNORM||f==VK_FORMAT_UNDEFINED)&&format.colorSpace()==VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) return format;
		}
		
		return availableFormats.get(0);
	}
	
	private VkExtent2D chooseSwapExtent(IVec2iR size, MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities){
		if(capabilities.currentExtent().width()==0xFFFFFFFF) return capabilities.currentExtent();
		
		VkExtent2D min=capabilities.minImageExtent(), max=capabilities.maxImageExtent();
		
		return VkExtent2D.callocStack(stack).set(MathUtil.snap(size.x(), min.width(), max.width()), MathUtil.snap(size.y(), min.height(), max.height()));
	}
	
	private int choosePresentMode(IntBuffer availablePresentModes){
		
		int[] ids=new int[availablePresentModes.capacity()];
		for(int i=0;i<availablePresentModes.capacity();i++) ids[i]=availablePresentModes.get(i);
		VulkanRenderer.Settings settings=gpu.getInstance().getSettings();
		
		if(settings.enableTrippleBuffering.get()&&UtilL.contains(ids, VK_PRESENT_MODE_MAILBOX_KHR)) return VK_PRESENT_MODE_MAILBOX_KHR;
		if(settings.enableVsync.get()&&UtilL.contains(ids, VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;
		return VK_PRESENT_MODE_IMMEDIATE_KHR;
	}
	
	public IntBuffer acquireNextImage(){
		Vk.acquireNextImageKHR(getDevice(), this, imageAviable.getHandle(), VK_NULL_HANDLE, acquireNextImageMem);
		return acquireNextImageMem;
	}
	
	@Override
	public void destroy(){
		if(DevelopmentInfo.DEV_ON&&handle==null) throw new IllegalStateException("Swapchain already destroyed");
		
		memFree(acquireNextImageMem);
		imageAviable.destroy();
		renderFinish.destroy();
		
		for(long framebuffer : framebuffers){
			vkDestroyFramebuffer(gpu.getDevice(), framebuffer, null);
		}
		forEach(colorFrames, VkTexture::destroy);
		vkDestroySwapchainKHR(gpu.getDevice(), handle.get(0), null);
		
		handle=null;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public LongBuffer getBuffer(){
		return handle;
	}
	
	public int getFormat(){
		return format;
	}
	
	public int getFramebufferCount(){
		return framebuffers.length;
	}
	
	public long getFramebuffer(int i){
		return framebuffers[i];
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public VkSemaphore getImageAviable(){
		return imageAviable;
	}
	
	public VkSemaphore getRenderFinish(){
		return renderFinish;
	}
}
