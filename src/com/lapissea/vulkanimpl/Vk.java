/*GEN*\
VK = org.lwjgl.vulkan.VK10
START
VK vkEnumeratePhysicalDevices basic
VK vkGetDeviceQueue basic
VK vkEnumeratePhysicalDevices basic optional=3,null
\*GEN*/

package com.lapissea.vulkanimpl;

import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.types.*;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk{
	
	private static final TIntObjectMap<String> CODES=new TIntObjectHashMap<>();
	
	static{
//		if(DEVELOPMENT) Generator_Vk.run();
		
		CODES.put(VK_SUCCESS, "Command successfully completed.");
		CODES.put(VK_NOT_READY, "A fence or query has not yet completed.");
		CODES.put(VK_TIMEOUT, "A wait operation has not completed in the specified time.");
		CODES.put(VK_EVENT_SET, "An event is signaled.");
		CODES.put(VK_EVENT_RESET, "An event is unsignaled.");
		CODES.put(VK_INCOMPLETE, "A return array was too small for the result.");
		CODES.put(VK_SUBOPTIMAL_KHR, "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.");
		
		CODES.put(VK_ERROR_OUT_OF_HOST_MEMORY, "A host memory allocation has failed.");
		CODES.put(VK_ERROR_OUT_OF_DEVICE_MEMORY, "A device memory allocation has failed.");
		CODES.put(VK_ERROR_INITIALIZATION_FAILED, "Initialization of an bool could not be completed for implementation-specific reasons.");
		CODES.put(VK_ERROR_DEVICE_LOST, "The logical or physical device has been lost.");
		CODES.put(VK_ERROR_MEMORY_MAP_FAILED, "Mapping of a memory bool has failed.");
		CODES.put(VK_ERROR_LAYER_NOT_PRESENT, "A requested layer is not present or could not be loaded.");
		CODES.put(VK_ERROR_EXTENSION_NOT_PRESENT, "A requested extension is not supported.");
		CODES.put(VK_ERROR_FEATURE_NOT_PRESENT, "A requested feature is not supported.");
		CODES.put(VK_ERROR_INCOMPATIBLE_DRIVER, "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.");
		CODES.put(VK_ERROR_TOO_MANY_OBJECTS, "Too many objects of the type have already been created.");
		CODES.put(VK_ERROR_FORMAT_NOT_SUPPORTED, "A requested format is not supported on this device.");
		CODES.put(VK_ERROR_SURFACE_LOST_KHR, "A surface is no longer available.");
		CODES.put(VK_ERROR_NATIVE_WINDOW_IN_USE_KHR, "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.");
		CODES.put(VK_ERROR_OUT_OF_DATE_KHR, "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"+"presenting to the surface.");
		CODES.put(VK_ERROR_INCOMPATIBLE_DISPLAY_KHR, "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"+" image.");
		CODES.put(VK_ERROR_VALIDATION_FAILED_EXT, "A validation layer found an error.");
	}
	
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
	
	public static String translateCode(int code){
		String message=CODES.get(code);
		return message==null?"Unknown ["+code+"]":message;
	}
	
	public static void check(int code){
		if(!DEVELOPMENT) throw UtilL.uncheckedThrow(new IllegalAccessException("\n\n"+
		                                                                       "\"check(int)\" can be used only for development debugging. \n"+
		                                                                       "Calling this in production mode is a sign of bad design/performance. \n"+
		                                                                       "Use \"if(DEVELOPMENT){...}\" to debug. (When DEVELOPMENT is false, it and its code will be removed by compiler optimization)\n"));
		
		if(code==VK_SUCCESS) return;
		
		List<String> msg=TextUtil.wrapLongString(translateCode(code), 100);
		
		throw new IllegalStateException(msg.size()==1?msg.get(0):"\n"+TextUtil.wrappedString(UtilL.array(msg)));
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest){
		return enumeratePhysicalDevices(instance, dest, null);
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest, PointerBuffer physicalDevices){
		int code=vkEnumeratePhysicalDevices(instance, dest, physicalDevices);
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static VkDevice createDevice(VkPhysicalDevice physicalDevice, VkDeviceCreateInfo deviceCreateInfo, VkAllocationCallbacks allocator, PointerBuffer pp){
		int code=vkCreateDevice(physicalDevice, deviceCreateInfo, allocator, pp);
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT){
			Objects.requireNonNull(gpu.getPhysicalDevice());
			Objects.requireNonNull(surface);
		}
		
		int code=vkGetPhysicalDeviceSurfaceSupportKHR(gpu.getPhysicalDevice(), queueFamilyIndex, surface.handle, dest);
		if(DEVELOPMENT) check(code);
		return dest.get(0)==VK_TRUE;
	}
	
	
	public static PointerBuffer stringsToPP(Iterable<? extends CharSequence> strings, MemoryStack stack){
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
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT) check(code);
	}
	
	public static void getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, VkSurface surface, IntBuffer count, IntBuffer modes){
		int code=vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.handle, count, modes);
		if(DEVELOPMENT) check(code);
	}
	
	public static long createSwapchainKHR(VkGpuCtx gpu, VkSwapchainCreateInfoKHR info, LongBuffer dest){
		int code=vkCreateSwapchainKHR(gpu.getDevice(), info, null, dest);
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static int getSwapchainImagesKHR(VkGpuCtx gpu, VkSwapchain vkSwapchain, IntBuffer imageCount, LongBuffer images){
		int code=vkGetSwapchainImagesKHR(gpu.getDevice(), vkSwapchain.getHandle(), imageCount, images);
		if(DEVELOPMENT) check(code);
		return imageCount.get(0);
	}
	
	public static VkImage[] getSwapchainImagesKHR(VkGpu gpu, VkSwapchain vkSwapchain, MemoryStack stack){
		IntBuffer ib   =stack.mallocInt(1);
		int       count=getSwapchainImagesKHR(gpu, vkSwapchain, ib, null);
		
		LongBuffer lb=stack.mallocLong(count);
		getSwapchainImagesKHR(gpu, vkSwapchain, ib, lb);
		
		VkImage[] images=new VkImage[count];
		for(int i=0;i<lb.limit();i++){
			images[i]=new VkImage(memAllocLong(1).put(0, lb.get(i)), gpu){
				@Override
				public void destroy(){
					memFree(getBuff());
				}
			};
		}
		
		return images;
	}
	
	public static long createImageView(VkGpuCtx gpu, VkImageViewCreateInfo info, LongBuffer dest){
		int code=vkCreateImageView(gpu.getDevice(), info, null, dest);
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static long createPipelineLayout(VkGpuCtx gpu, VkPipelineLayoutCreateInfo info, LongBuffer dest){
		int code=vkCreatePipelineLayout(gpu.getDevice(), info, null, dest);
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static VkRenderPass createRenderPass(VkGpuCtx gpu, VkRenderPassCreateInfo info, LongBuffer dest){
		int code=vkCreateRenderPass(gpu.getDevice(), info, null, dest);
		if(DEVELOPMENT) check(code);
		return new VkRenderPass(gpu.getGpu(), dest.get(0));
	}
	
	public static long createGraphicsPipelines(VkGpuCtx gpu, long cache, VkGraphicsPipelineCreateInfo.Buffer pipelineInfo, LongBuffer dest){
		if(DEVELOPMENT&&pipelineInfo.limit()!=dest.limit()) throw new IllegalArgumentException();
		
		int code=vkCreateGraphicsPipelines(gpu.getDevice(), cache, pipelineInfo, null, dest);
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static void getPipelineCacheData(VkGpuCtx gpu, long handle, PointerBuffer size, ByteBuffer data){
		int code=vkGetPipelineCacheData(gpu.getDevice(), handle, size, data);
		if(DEVELOPMENT) check(code);
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
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static VkCommandPool createCommandPool(VkGpuCtx gpuCtx, VkCommandPoolCreateInfo poolInfo, LongBuffer dest){
		int code=vkCreateCommandPool(gpuCtx.getDevice(), poolInfo, null, dest);
		if(DEVELOPMENT) check(code);
		return new VkCommandPool(gpuCtx.getGpu(), dest.get(0));
	}
	
	public static VkCommandBufferM[] allocateCommandBuffers(VkGpu gpu, VkCommandPool pool, VkCommandBufferAllocateInfo allocInfo, PointerBuffer dest){
		allocInfo.commandPool(pool.getHandle());
		
		int code=vkAllocateCommandBuffers(gpu.getDevice(), allocInfo, dest);
		if(DEVELOPMENT) check(code);
		VkCommandBufferM[] res=new VkCommandBufferM[allocInfo.commandBufferCount()];
		for(int i=0;i<res.length;i++) res[i]=new VkCommandBufferM(dest.get(i), pool, allocInfo.level()==VK_COMMAND_BUFFER_LEVEL_PRIMARY);
		return res;
	}
	
	public static void beginCommandBuffer(VkCommandBuffer commandBuffer, VkCommandBufferBeginInfo beginInfo){
		int code=vkBeginCommandBuffer(commandBuffer, beginInfo);
		if(DEVELOPMENT) check(code);
	}
	
	public static void endCommandBuffer(VkCommandBuffer commandBuffer){
		int code=vkEndCommandBuffer(commandBuffer);
		if(DEVELOPMENT) check(code);
	}
	
	public static VkSemaphore createSemaphore(VkGpuCtx gpuCtx, VkSemaphoreCreateInfo semaphoreInfo){
		LongBuffer dest=memAllocLong(1);
		
		int code=VK10.vkCreateSemaphore(gpuCtx.getDevice(), semaphoreInfo, null, dest);
		if(DEVELOPMENT) check(code);
		return new VkSemaphore(gpuCtx.getGpu(), dest);
	}
	
	/*/START_GEN/*/
	//lel
	
	
	
	/*/END_GEN/*/
}
