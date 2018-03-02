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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain implements VkDestroyable, VkGpuCtx{
	
	public class Frame{
		private      long        frameBuffer;
		private      VkTexture   colorFrame;
		private      VkSemaphore renderFinish;
		public final int         index;
		
		public Frame(int index, VkTexture colorFrame, VkSemaphore renderFinish){
			this.index=index;
			this.colorFrame=colorFrame;
			this.renderFinish=renderFinish;
		}
		
		public void destroy(){
			vkDestroyFramebuffer(getDevice(), frameBuffer, null);
			colorFrame.destroy();
			renderFinish.destroy();
		}
		
		public VkSemaphore getRenderFinish(){
			return renderFinish;
		}
		
		public long getFrameBuffer(){
			return frameBuffer;
		}
	}
	
	private       LongBuffer  handle;
	private final VkGpu       gpu;
	private       int         format;
	private       int         colorSpace;
	private       VkSemaphore imageAviable;
	private       List<Frame> frames;
	private final VkExtent2D extent=VkExtent2D.calloc();
	
	private IntBuffer acquireNextImageMem=memAllocInt(1);
	
	private VkCommandBufferM[] frameBinds;
	
	public VkSwapchain(VkGpu gpu, VkSurface surface, IVec2iR size){
		this.gpu=gpu;
		try(MemoryStack stack=stackPush()){
			create(gpu, surface, size, stack);
		}
		imageAviable=gpu.createSemaphore();
	}
	
	private void create(VkGpu gpu, VkSurface surface, IVec2iR size, MemoryStack stack){
		
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
		chooseSwapExtent(size, stack, surface.getCapabilities(gpu, stack));
		
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
		List<Frame> frames0=new ArrayList<>(colors.length);
		for(int i=0;i<colors.length;i++){
			frames0.add(new Frame(i, colors[i], gpu.createSemaphore()));
		}
		frames=Collections.unmodifiableList(frames0);
	}
	
	
	public void initFrameBuffers(VkRenderPass renderPass){
		
		try(MemoryStack stack=stackPush()){
			VkFramebufferCreateInfo framebufferInfo=VkFramebufferCreateInfo.callocStack(stack);
			framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
			               .renderPass(renderPass.getHandle())
			               .width(extent.width())
			               .height(extent.height())
			               .layers(1);
			
			LongBuffer lb=stack.mallocLong(1), view=stack.mallocLong(1);
			for(Frame frame : frames){
				frame.frameBuffer=Vk.createFrameBuffer(gpu, framebufferInfo.pAttachments(view.put(0, frame.colorFrame.getView())), lb);
			}
			
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
	
	private void chooseSwapExtent(IVec2iR size, MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities){
		if(capabilities.currentExtent().width()==0xFFFFFFFF){
			extent.set(capabilities.currentExtent());
			return;
		}
		
		VkExtent2D min=capabilities.minImageExtent(), max=capabilities.maxImageExtent();
		
		extent.set(MathUtil.snap(size.x(), min.width(), max.width()), MathUtil.snap(size.y(), min.height(), max.height()));
	}
	
	private int choosePresentMode(IntBuffer availablePresentModes){
		
		int[] ids=new int[availablePresentModes.capacity()];
		for(int i=0;i<availablePresentModes.capacity();i++) ids[i]=availablePresentModes.get(i);
		VulkanCore.Settings settings=gpu.getInstance().getSettings();
		
		if(settings.tripleBufferingEnabled.get()&&UtilL.contains(ids, VK_PRESENT_MODE_MAILBOX_KHR)) return VK_PRESENT_MODE_MAILBOX_KHR;
		if(settings.vSyncEnabled.get()&&UtilL.contains(ids, VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;
		return VK_PRESENT_MODE_IMMEDIATE_KHR;
	}
	
	public Frame acquireNextFrame(){
		int id=Vk.acquireNextImageKHR(getDevice(), this, imageAviable.getHandle(), VK_NULL_HANDLE, acquireNextImageMem);
		return id==-1?null:frames.get(id);
	}
	
	@Override
	public void destroy(){
		if(DevelopmentInfo.DEV_ON&&handle==null) throw new IllegalStateException("Swapchain already destroyed");
		
		memFree(acquireNextImageMem);
		imageAviable.destroy();
		frames.forEach(Frame::destroy);
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
	
	
	public List<Frame> getFrames(){
		return frames;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public VkSemaphore getImageAviable(){
		return imageAviable;
	}
	
	public VkExtent2D getSize(){
		return extent;
	}
}
