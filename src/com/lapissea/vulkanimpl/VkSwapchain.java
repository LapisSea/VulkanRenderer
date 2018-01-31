package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.MathUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain implements VkDestroyable{
	
	private final VkGpu gpu;
	private long handle=VK_NULL_HANDLE;
	
	public VkSwapchain(VkGpu gpu, VkSurface surface){
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
		
		//
		int                presentMode=choosePresentMode(presentModes);
		VkSurfaceFormatKHR format     =chooseSwapSurfaceFormat(Vk.getPhysicalDeviceSurfaceFormatsKHR(gpu, stack));
		VkExtent2D         extent     =chooseSwapExtent(surface.getSize(), stack, surface.getCapabilities(gpu, stack));
		
		int imageCount=caps.minImageCount()+1;
		if(caps.maxImageCount()>0) imageCount=Math.min(imageCount, caps.maxImageCount());
		
		
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
		
		handle=Vk.createSwapchainKHR(gpu.getDevice(), createInfo, stack.mallocLong(1));
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
		vkDestroySwapchainKHR(gpu.getDevice(), handle, null);
	}
}