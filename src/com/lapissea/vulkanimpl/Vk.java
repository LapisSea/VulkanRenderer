/*GEN*\
VK = org.lwjgl.vulkan.VK10
START
VK vkEnumeratePhysicalDevices basic
VK vkGetDeviceQueue basic
VK vkEnumeratePhysicalDevices basic optional=3,null
\*GEN*/

package com.lapissea.vulkanimpl;

import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.exceptions.VkException;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.format.VkDescriptor;
import com.lapissea.vulkanimpl.util.format.VkFormat;
import com.lapissea.vulkanimpl.util.types.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static com.lapissea.vulkanimpl.util.types.VkFence.Status.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk{
	
	
	public static VkApplicationInfo initAppInfo(MemoryStack stack, String applicationName, String applicationVersion, String engineName, String engineVersion){
		
		Function<String, Integer> version=s->{
			String[] parts=s.split("\\.");
			return VK_MAKE_VERSION(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		};
		
		VkApplicationInfo info=VkApplicationInfo.callocStack(stack);
		info.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		info.pApplicationName(stack.UTF8(applicationName));
		info.pEngineName(stack.UTF8(engineName));
		info.applicationVersion(version.apply(applicationVersion));
		info.engineVersion(version.apply(engineVersion));
		info.apiVersion(VK_API_VERSION_1_0);
		return info;
	}
	
	public static VkInstance createInstance(VkInstanceCreateInfo instanceInfo, VkAllocationCallbacks allocator){
		PointerBuffer pp=memAllocPointer(1);
		try{
			int err=vkCreateInstance(instanceInfo, allocator, pp);
			if(err==0) return new VkInstance(pp.get(0), instanceInfo);
			if(err==VK_ERROR_INCOMPATIBLE_DRIVER) throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD).");
			if(err==VK_ERROR_EXTENSION_NOT_PRESENT) throw new IllegalStateException("Cannot find a specified extension library. Make sure your layers path is set appropriately.");
			throw new IllegalStateException("vkCreateInstance failed. Do you have a compatible Vulkan installable client driver (ICD) installed?");
		}finally{
			memFree(pp);
		}
	}
	
	public static void check(int code){
		if(!DEV_ON) throw UtilL.uncheckedThrow(new IllegalAccessException("\n\n"+
		                                                                  "\"check(int)\" can be used only for development debugging. \n"+
		                                                                  "Calling this in production mode is a sign of bad design/performance. \n"+
		                                                                  "Use \"if(DEV_ON){...}\" to debug. (When DEV_ON is false, it and its code will be removed by compiler optimization)\n"));
		
		if(code==VK_SUCCESS) return;
		throw VkException.throwByCode(code);
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest){
		return enumeratePhysicalDevices(instance, dest, null);
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest, PointerBuffer physicalDevices){
		int code=vkEnumeratePhysicalDevices(instance, dest, physicalDevices);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static VkPhysicalDevice[] getPhysicalDevices(MemoryStack stack, VkInstance instance){
		IntBuffer ib         =stack.callocInt(1);
		int       deviceCount=Vk.enumeratePhysicalDevices(instance, ib);
		if(deviceCount==0) return new VkPhysicalDevice[deviceCount];
		PointerBuffer physicalDevices=stack.callocPointer(deviceCount);
		Vk.enumeratePhysicalDevices(instance, ib, physicalDevices);
		
		return getPhysicalDevices(instance, physicalDevices);
	}
	
	public static VkPhysicalDevice[] getPhysicalDevices(VkInstance instance, PointerBuffer physicalDevices){
		VkPhysicalDevice[] devices=new VkPhysicalDevice[physicalDevices.capacity()];
		
		for(int i=0;i<physicalDevices.capacity();i++){
			devices[i]=new VkPhysicalDevice(physicalDevices.get(i), instance);
		}
		return devices;
	}
	
	public static VkExtensionProperties.Buffer getDeviceExtensionProperties(MemoryStack stack, VkPhysicalDevice physicalDevice, IntBuffer dest){
		int count=enumerateDeviceExtensionProperties(physicalDevice, dest);
		
		VkExtensionProperties.Buffer properties=VkExtensionProperties.callocStack(count, stack);
		enumerateDeviceExtensionProperties(physicalDevice, null, dest, properties);
		
		return properties;
	}
	
	public static int enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, IntBuffer dest){
		return enumerateDeviceExtensionProperties(physicalDevice, null, dest, null);
	}
	
	public static int enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, String layerName, IntBuffer dest, VkExtensionProperties.Buffer properties){
		int code=vkEnumerateDeviceExtensionProperties(physicalDevice, layerName, dest, properties);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static int getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, IntBuffer dest, VkQueueFamilyProperties.Buffer pQueueFamilyProperties){
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, pQueueFamilyProperties);
		return dest.get(0);
	}
	
	public static VkExtensionProperties.Buffer enumerateInstanceExtensionProperties(MemoryStack stack){
		IntBuffer ib=stack.mallocInt(1);
		
		VkExtensionProperties.Buffer extensions=VkExtensionProperties.callocStack(enumerateInstanceExtensionProperties(null, ib, null), stack);
		enumerateInstanceExtensionProperties(null, ib, extensions);
		return extensions;
	}
	
	public static int enumerateInstanceExtensionProperties(String layerName, IntBuffer dest, VkExtensionProperties.Buffer properties){
		int code=vkEnumerateInstanceExtensionProperties(layerName, dest, properties);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static VkLayerProperties.Buffer enumerateInstanceLayerProperties(MemoryStack stack){
		IntBuffer ib=stack.mallocInt(1);
		
		VkLayerProperties.Buffer layers=VkLayerProperties.callocStack(enumerateInstanceLayerProperties(ib, null), stack);
		enumerateInstanceLayerProperties(ib, layers);
		return layers;
	}
	
	public static int enumerateInstanceLayerProperties(IntBuffer dest, VkLayerProperties.Buffer properties){
		int code=vkEnumerateInstanceLayerProperties(dest, properties);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static VkDevice createDevice(VkPhysicalDevice physicalDevice, VkDeviceCreateInfo deviceCreateInfo, VkAllocationCallbacks allocator, PointerBuffer pp){
		int code=vkCreateDevice(physicalDevice, deviceCreateInfo, allocator, pp);
		if(DEV_ON) check(code);
		return new VkDevice(pp.get(0), physicalDevice, deviceCreateInfo);
	}
	
	public static VkQueue createDeviceQueue(VkDevice device, int queueFamilyIndex, int queueIndex, PointerBuffer dest){
		return new VkQueue(Vk.getDeviceQueue(device, queueFamilyIndex, queueIndex, dest), device);
	}
	
	
	public static long getDeviceQueue(VkDevice device, int queueFamilyIndex, int queueIndex, PointerBuffer dest){
		vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, dest);
		return dest.get(0);
	}
	
	public static boolean getPhysicalDeviceSurfaceSupportKHR(VkGpu gpu, int queueFamilyIndex, IntBuffer dest){
		VkSurface surface=gpu.getInstance().getSurface();
		if(DEV_ON){
			Objects.requireNonNull(gpu.getPhysicalDevice());
			Objects.requireNonNull(surface);
		}
		
		int code=vkGetPhysicalDeviceSurfaceSupportKHR(gpu.getPhysicalDevice(), queueFamilyIndex, surface.handle, dest);
		if(DEV_ON) check(code);
		return dest.get(0)==VK_TRUE;
	}
	
	
	public static PointerBuffer stringsToPP(Iterable<? extends CharSequence> strings, MemoryStack stack){
		if(strings==null) return null;
		
		int size;
		if(strings instanceof Collection) size=((Collection)strings).size();
		else{
			size=0;
			for(CharSequence s : strings){
				size++;
			}
		}
		return size==0?null:stringsToPP(strings, stack.callocPointer(size));
	}
	
	public static PointerBuffer stringsToPP(Iterable<? extends CharSequence> strings, PointerBuffer dest){
		int i=0;
		for(CharSequence s : strings){
			dest.put(i++, memASCII(s));
		}
		return dest;
	}
	
	public static void getPhysicalDeviceSurfaceCapabilitiesKHR(VkGpu gpu, VkSurface surface, VkSurfaceCapabilitiesKHR caps){
		int code=vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu.getPhysicalDevice(), surface.handle, caps);
		if(DEV_ON) check(code);
	}
	
	public static VkSurfaceFormatKHR.Buffer getPhysicalDeviceSurfaceFormatsKHR(VkGpu gpu, MemoryStack stack){
		IntBuffer count  =stack.mallocInt(1);
		long      surface=gpu.getInstance().getSurface().handle;
		getPhysicalDeviceSurfaceFormatsKHR(gpu, surface, count, null);
		VkSurfaceFormatKHR.Buffer surfaceFormats=VkSurfaceFormatKHR.mallocStack(count.get(0));
		getPhysicalDeviceSurfaceFormatsKHR(gpu, surface, count, surfaceFormats);
		return surfaceFormats;
	}
	
	public static void getPhysicalDeviceSurfaceFormatsKHR(VkGpuCtx gpu, long surface, IntBuffer count, VkSurfaceFormatKHR.Buffer surfaceFormats){
		int code=vkGetPhysicalDeviceSurfaceFormatsKHR(gpu.getGpu().getPhysicalDevice(), surface, count, surfaceFormats);
		if(DEV_ON) check(code);
	}
	
	public static void getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, VkSurface surface, IntBuffer count, IntBuffer modes){
		int code=vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.handle, count, modes);
		if(DEV_ON) check(code);
	}
	
	public static long createSwapchainKHR(VkGpuCtx gpu, VkSwapchainCreateInfoKHR info, LongBuffer dest){
		int code=vkCreateSwapchainKHR(gpu.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static int getSwapchainImagesKHR(VkGpuCtx gpu, VkSwapchain vkSwapchain, IntBuffer imageCount, LongBuffer images){
		int code=vkGetSwapchainImagesKHR(gpu.getDevice(), vkSwapchain.getHandle(), imageCount, images);
		if(DEV_ON) check(code);
		return imageCount.get(0);
	}
	
	public static VkImage[] getSwapchainImagesKHR(VkGpu gpu, VkSwapchain swapchain, MemoryStack stack){
		IntBuffer ib   =stack.mallocInt(1);
		int       count=getSwapchainImagesKHR(gpu, swapchain, ib, null);
		
		LongBuffer lb=stack.mallocLong(count);
		getSwapchainImagesKHR(gpu, swapchain, ib, lb);
		
		VkImage[] images=new VkImage[count];
		for(int i=0;i<lb.limit();i++){
			images[i]=new VkImage(memAllocLong(1).put(0, lb.get(i)),
			                      gpu,
			                      VK_IMAGE_LAYOUT_UNDEFINED,
			                      VkExtent3D.calloc().set(swapchain.getExtent().width(), swapchain.getExtent().height(), 1),
			                      VkFormat.get(swapchain.getColorFormat())){
				@Override
				public void destroy(){
					memFree(getBuff());
				}
				
				@Override
				public void transitionLayout(VkGpu.Queue queue, int newLayout, int srcStageMask, int dstStageMask, int srcAccessMask, int dstAccessMask, int aspectMask){
					throw new RuntimeException();
				}
			};
		}
		
		return images;
	}
	
	public static long createImageView(VkGpuCtx gpu, VkImageViewCreateInfo info, LongBuffer dest){
		int code=vkCreateImageView(gpu.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static long createPipelineLayout(VkGpuCtx gpu, VkPipelineLayoutCreateInfo info, LongBuffer dest){
		int code=vkCreatePipelineLayout(gpu.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static VkRenderPass createRenderPass(VkGpuCtx gpu, VkRenderPassCreateInfo info, LongBuffer dest){
		int code=vkCreateRenderPass(gpu.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		
		return new VkRenderPass(gpu.getGpu(), dest.get(0), info.pAttachments().get(0).samples());
	}
	
	public static long createGraphicsPipelines(VkGpuCtx gpu, long cache, VkGraphicsPipelineCreateInfo.Buffer pipelineInfo, LongBuffer dest){
		if(DEV_ON&&pipelineInfo.limit()!=dest.limit()) throw new IllegalArgumentException();
		
		int code=vkCreateGraphicsPipelines(gpu.getDevice(), cache, pipelineInfo, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static void getPipelineCacheData(VkGpuCtx gpu, long handle, PointerBuffer size, ByteBuffer data){
		int code=vkGetPipelineCacheData(gpu.getDevice(), handle, size, data);
		if(DEV_ON) check(code);
	}
	
	public static ByteBuffer writeBytes(ByteBuffer data, BufferedOutputStream out) throws IOException{
		for(int i=data.position();i<data.limit();i++){
			out.write(data.get(i));
		}
		return data;
	}
	
	public static ByteBuffer readBytes(ByteBuffer data, BufferedInputStream in) throws IOException{
		
		while(data.hasRemaining()){
			data.put((byte)in.read());
		}
		
		return data;
	}
	
	public static long createFrameBuffer(VkGpuCtx gpuCtx, VkFramebufferCreateInfo framebufferInfo, LongBuffer dest){
		int code=VK10.vkCreateFramebuffer(gpuCtx.getDevice(), framebufferInfo, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static VkCommandPool createCommandPool(VkGpuCtx gpuCtx, VkCommandPoolCreateInfo poolInfo, LongBuffer dest){
		int code=vkCreateCommandPool(gpuCtx.getDevice(), poolInfo, null, dest);
		if(DEV_ON) check(code);
		return new VkCommandPool(gpuCtx.getGpu(), dest.get(0));
	}
	
	public static VkCommandBufferM[] allocateCommandBuffers(VkGpu gpu, VkCommandPool pool, VkCommandBufferAllocateInfo allocInfo, PointerBuffer dest){
		allocInfo.commandPool(pool.getHandle());
		
		int code=vkAllocateCommandBuffers(gpu.getDevice(), allocInfo, dest);
		if(DEV_ON) check(code);
		VkCommandBufferM[] res=new VkCommandBufferM[allocInfo.commandBufferCount()];
		for(int i=0;i<res.length;i++) res[i]=new VkCommandBufferM(dest.get(i), pool, allocInfo.level()==VK_COMMAND_BUFFER_LEVEL_PRIMARY);
		return res;
	}
	
	public static void beginCommandBuffer(VkCommandBuffer commandBuffer, VkCommandBufferBeginInfo beginInfo){
		int code=vkBeginCommandBuffer(commandBuffer, beginInfo);
		if(DEV_ON) check(code);
	}
	
	public static void endCommandBuffer(VkCommandBuffer commandBuffer){
		int code=vkEndCommandBuffer(commandBuffer);
		if(DEV_ON) check(code);
	}
	
	public static VkSemaphore createSemaphore(VkGpuCtx gpuCtx, VkSemaphoreCreateInfo semaphoreInfo){
		LongBuffer dest=memAllocLong(1);
		
		int code=VK10.vkCreateSemaphore(gpuCtx.getDevice(), semaphoreInfo, null, dest);
		if(DEV_ON) check(code);
		return new VkSemaphore(gpuCtx.getGpu(), dest);
	}
	
	public static int acquireNextImageKHR(VkDevice device, VkSwapchain swapChain, long semaphore, long fence, IntBuffer dest){
		int code=vkAcquireNextImageKHR(device, swapChain.getHandle(), Long.MAX_VALUE, semaphore, fence, dest);
		if(code==VK_ERROR_OUT_OF_DATE_KHR) return -1;
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static void queueSubmit(VkQueue queue, VkSubmitInfo info, long fence){
		int code=vkQueueSubmit(queue, info, fence);
		if(DEV_ON) check(code);
	}
	
	public static boolean queuePresentKHR(VkQueue queue, VkPresentInfoKHR presentInfo){
		int code=vkQueuePresentKHR(queue, presentInfo);
		if(code==VK_SUBOPTIMAL_KHR||code==VK_ERROR_OUT_OF_DATE_KHR) return true;
		if(DEV_ON) check(code);
		return false;
	}
	
	public static void queueWaitIdle(VkQueue queue){
		int code=vkQueueWaitIdle(queue);
		if(DEV_ON) check(code);
	}
	
	public static VkBuffer createBuffer(VkGpuCtx gpuCtx, VkBufferCreateInfo bufferInfo){
		LongBuffer handle=memAllocLong(1);
		int        code  =vkCreateBuffer(gpuCtx.getDevice(), bufferInfo, null, handle);
		if(DEV_ON) check(code);
		return new VkBuffer(gpuCtx, handle, bufferInfo.size());
	}
	
	public static VkDeviceMemory allocateMemory(VkGpuCtx gpuCtx, VkMemoryAllocateInfo allocInfo){
		LongBuffer handle=memAllocLong(1);
		int        code  =vkAllocateMemory(gpuCtx.getDevice(), allocInfo, null, handle);
		if(DEV_ON) check(code);
		return new VkDeviceMemory(gpuCtx.getGpu(), handle, allocInfo.allocationSize());
	}
	
	public static PointerBuffer mapMemory(VkDevice device, long memory, long offset, long size, int flags, PointerBuffer dest){
		int code=vkMapMemory(device, memory, offset, size, flags, dest);
		if(DEV_ON) check(code);
		return dest;
	}
	
	public static void flushMappedMemoryRanges(VkDevice device, VkMappedMemoryRange info){
		int code=vkFlushMappedMemoryRanges(device, info);
		if(DEV_ON) check(code);
	}
	
	public static void invalidateMappedMemoryRanges(VkDevice device, VkMappedMemoryRange info){
		int code=vkInvalidateMappedMemoryRanges(device, info);
		if(DEV_ON) check(code);
	}
	
	public static void waitForFence(VkGpuCtx ctx, long fence){
		int code=vkWaitForFences(ctx.getDevice(), fence, true, Long.MAX_VALUE);
		if(DEV_ON) check(code);
	}
	
	public static VkFence.Status getFenceStatus(VkGpuCtx ctx, long fence){
		int code=vkGetFenceStatus(ctx.getDevice(), fence);
		if(DEV_ON) check(code);
		return code==VK_NOT_READY?NOT_READY:SUCCESS;
	}
	
	public static VkFence createFence(VkGpuCtx ctx, VkFenceCreateInfo info, LongBuffer dest){
		int code=vkCreateFence(ctx.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		return new VkFence(ctx, dest.get(0));
	}
	
	public static VkDescriptorSetLayout createDescriptorSetLayout(VkGpuCtx ctx, VkDescriptorSetLayoutCreateInfo info){
		LongBuffer dest=memAllocLong(1);
		
		int code=vkCreateDescriptorSetLayout(ctx.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		
		VkDescriptorSetLayoutBinding.Buffer bindings=info.pBindings();
		Objects.requireNonNull(bindings);
		
		VkDescriptor[] poolParts=new VkDescriptor[bindings.capacity()];
		
		for(int i=0;i<poolParts.length;i++){
			VkDescriptorSetLayoutBinding kek=bindings.get(i);
			poolParts[i]=new VkDescriptor(kek.descriptorType(), kek.stageFlags());
		}
		
		return new VkDescriptorSetLayout(dest, ctx.getGpu(), poolParts);
	}
	
	public static VkDescriptorPool createDescriptorPool(VkGpuCtx ctx, VkDescriptorPoolCreateInfo info, LongBuffer dest){
		int code=vkCreateDescriptorPool(ctx.getDevice(), info, null, dest);
		if(DEV_ON) check(code);
		return new VkDescriptorPool(ctx.getGpu(), dest.get(0));
	}
	
	public static VkDescriptorSet[] allocateDescriptorSets(VkGpuCtx ctx, VkDescriptorSetAllocateInfo info){
		try(MemoryStack stack=stackPush()){
			LongBuffer dest=stack.mallocLong(info.descriptorSetCount());
//			LongBuffer dest=memAllocLong(info.descriptorSetCount());
			
			int code=vkAllocateDescriptorSets(ctx.getDevice(), info, dest);
			if(DEV_ON) check(code);
			VkDescriptorSet[] dst=new VkDescriptorSet[dest.limit()];
			for(int i=0;i<dest.limit();i++){
				dst[i]=new VkDescriptorSet(ctx.getGpu(), memAllocLong(1).put(0, dest.get(i)));
			}
			return dst;
		}
	}
	
	public static long createSampler(VkDevice device, VkSamplerCreateInfo info, LongBuffer dest){
		int code=vkCreateSampler(device, info, null, dest);
		if(DEV_ON) check(code);
		return dest.get(0);
	}
	
	public static void resetCommandBuffer(VkCommandBuffer commandBuffer, int flags){
		int code=vkResetCommandBuffer(commandBuffer, flags);
		if(DEV_ON) check(code);
	}
	
	
	
	/*/START_GEN/*/
	//lel
	
	
	
	/*/END_GEN/*/
}
