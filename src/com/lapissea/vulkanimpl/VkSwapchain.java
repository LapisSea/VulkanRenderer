package com.lapissea.vulkanimpl;

import com.lapissea.util.MathUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkRenderPass;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import com.lapissea.vulkanimpl.util.types.VkTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.stream.Stream;

import static com.lapissea.util.UtilL.*;
import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain implements VkDestroyable{
	
	private       long        handle;
	private final VkGpu       gpu;
	private       VkTexture[] colorFrames;
	private       int         format;
	private       int         colorSpace;
	private final VkSurface   surface;
	private       long[]      framebuffers;
	
	private VkCommandBufferM[] frameBinds;
	
	public VkSwapchain(VkGpu gpu, VkSurface surface){
		this.surface=surface;
		this.gpu=gpu;
		try(MemoryStack stack=stackPush()){
			create(gpu, surface, stack);
		}
	}
	
	private void create(VkGpu gpu, VkSurface surface, MemoryStack stack){
		
		//acquire currency
		IntBuffer count=stack.mallocInt(1);
		Vk.getPhysicalDeviceSurfacePresentModesKHR(gpu.getPhysicalDevice(), surface, count, null);
		IntBuffer presentModes=MemoryUtil.memAllocInt(count.get(0));
		
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
		
		handle=Vk.createSwapchainKHR(gpu, createInfo, stack.mallocLong(1));
		
		colorFrames=UtilL.convert(Vk.getSwapchainImagesKHR(gpu, this, stack), VkTexture[]::new,
		                          image->new VkTexture(image).createView(format.format(), VkImageAspect.COLOR));
	}
	
	
	public void initSwapchain(VkRenderPass renderPass){
		
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
	
	@Override
	public void destroy(){
		if(DEVELOPMENT&&handle==-1) throw new IllegalStateException("Swapchain already destroyed");
		
		for(long framebuffer : framebuffers){
			vkDestroyFramebuffer(gpu.getDevice(), framebuffer, null);
		}
		forEach(colorFrames, VkTexture::destroy);
		vkDestroySwapchainKHR(gpu.getDevice(), handle, null);
		handle=-1;
	}
	
	public long getHandle(){
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
}
