package com.lapissea.vulkanimpl;

import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.color.IColorM;
import com.lapissea.vulkanimpl.simplevktypes.*;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageFormat;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk{
	
	public static final boolean DEBUG=true;
	
	public static VkApplicationInfo initAppInfo(MemoryStack stack, String name){
		ByteBuffer        nam       =stack.UTF8(name);
		ByteBuffer        engineName=stack.UTF8("LapisEngine");
		VkApplicationInfo info      =VkApplicationInfo.callocStack(stack);
		info.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
		info.pNext(MemoryUtil.NULL);
		info.pApplicationName(nam);
		info.pEngineName(engineName);
		info.applicationVersion(VK_MAKE_VERSION(0, 1, 0));
		info.engineVersion(VK_MAKE_VERSION(0, 1, 0));
		info.apiVersion(VK10.VK_API_VERSION_1_0);
		return info;
	}
	
	public static VkInstanceCreateInfo initInstanceInfo(MemoryStack stack, VkApplicationInfo app){
		VkInstanceCreateInfo info=VkInstanceCreateInfo.callocStack(stack);
		info.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		info.pNext(MemoryUtil.NULL);
		info.flags(0);
		info.pApplicationInfo(app);
		info.ppEnabledExtensionNames(null);
		info.ppEnabledLayerNames(null);
		return info;
	}
	
	public static VkInstance createInstance(VkInstanceCreateInfo instanceInfo, PointerBuffer pp){
		int err=VK10.vkCreateInstance(instanceInfo, null, pp);
		
		if(err==0) return new VkInstance(pp.get(0), instanceInfo);
		if(err==VK10.VK_ERROR_INCOMPATIBLE_DRIVER) throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD).");
		if(err==VK10.VK_ERROR_EXTENSION_NOT_PRESENT) throw new IllegalStateException("Cannot find a specified extension library. Make sure your layers path is set appropriately.");
		throw new IllegalStateException("vkCreateInstance failed. Do you have a compatible Vulkan installable client driver (ICD) installed?");
		
	}
	
	public static String translateCode(int code){
		switch(code){
		// Success codes
		case VK_SUCCESS:
			return "Command successfully completed.";
		case VK_NOT_READY:
			return "A fence or query has not yet completed.";
		case VK_TIMEOUT:
			return "A wait operation has not completed in the specified time.";
		case VK_EVENT_SET:
			return "An event is signaled.";
		case VK_EVENT_RESET:
			return "An event is unsignaled.";
		case VK_INCOMPLETE:
			return "A return array was too small for the result.";
		case VK_SUBOPTIMAL_KHR:
			return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";
		
		// Error codes
		case VK_ERROR_OUT_OF_HOST_MEMORY:
			return "A host memory allocation has failed.";
		case VK_ERROR_OUT_OF_DEVICE_MEMORY:
			return "A device memory allocation has failed.";
		case VK_ERROR_INITIALIZATION_FAILED:
			return "Initialization of an object could not be completed for implementation-specific reasons.";
		case VK_ERROR_DEVICE_LOST:
			return "The logical or physical device has been lost.";
		case VK_ERROR_MEMORY_MAP_FAILED:
			return "Mapping of a memory object has failed.";
		case VK_ERROR_LAYER_NOT_PRESENT:
			return "A requested layer is not present or could not be loaded.";
		case VK_ERROR_EXTENSION_NOT_PRESENT:
			return "A requested extension is not supported.";
		case VK_ERROR_FEATURE_NOT_PRESENT:
			return "A requested feature is not supported.";
		case VK_ERROR_INCOMPATIBLE_DRIVER:
			return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
		case VK_ERROR_TOO_MANY_OBJECTS:
			return "Too many objects of the type have already been created.";
		case VK_ERROR_FORMAT_NOT_SUPPORTED:
			return "A requested format is not supported on this device.";
		case VK_ERROR_SURFACE_LOST_KHR:
			return "A surface is no longer available.";
		case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
			return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
		case VK_ERROR_OUT_OF_DATE_KHR:
			return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
			       +"swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"+"presenting to the surface.";
		case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
			return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"+" image.";
		case VK_ERROR_VALIDATION_FAILED_EXT:
			return "A validation layer found an error.";
		default:
			return "Unknown ["+code+"]";
		}
	}
	
	public static void check(int code){
		if(!DEBUG) throw new IllegalStateException();
		
		if(code!=VK10.VK_SUCCESS){
			List<String> msg=TextUtil.wrapLongString(translateCode(code), 100);
			
			throw new IllegalStateException(msg.size()==1?msg.get(0):"\n"+TextUtil.wrappedString(UtilL.array(msg)));
		}
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest){
		return enumeratePhysicalDevices(instance, dest, null);
	}
	
	public static int enumeratePhysicalDevices(VkInstance instance, IntBuffer dest, PointerBuffer physicalDevices){
		int code=VK10.vkEnumeratePhysicalDevices(instance, dest, physicalDevices);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkPhysicalDevice[] getPhysicalDevices(MemoryStack stack, VkInstance instance){
		IntBuffer     ib             =stack.callocInt(1);
		int           deviceCount    =Vk.enumeratePhysicalDevices(instance, ib);
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
		
		VkExtensionProperties.Buffer properties=stack==null?VkExtensionProperties.calloc(count):VkExtensionProperties.callocStack(count, stack);
		enumerateDeviceExtensionProperties(physicalDevice, null, dest, properties);
		
		return properties;
	}
	
	public static int enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, IntBuffer dest){
		return enumerateDeviceExtensionProperties(physicalDevice, null, dest, null);
	}
	
	public static int enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, String layerName, IntBuffer dest, VkExtensionProperties.Buffer properties){
		int code=VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, layerName, dest, properties);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, IntBuffer dest){
		
		int count=getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, null);
		
		VkQueueFamilyProperties.Buffer result=VkQueueFamilyProperties.calloc(count);
		
		if(getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, result)==0) throw new IllegalStateException();
		return result;
	}
	
	public static VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(MemoryStack stack, VkPhysicalDevice physicalDevice, IntBuffer dest){
		
		int count=getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, null);
		
		VkQueueFamilyProperties.Buffer result=VkQueueFamilyProperties.callocStack(count, stack);
		
		if(getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, result)==0) throw new IllegalStateException();
		return result;
	}
	
	public static int getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, IntBuffer dest, VkQueueFamilyProperties.Buffer pQueueFamilyProperties){
		VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, pQueueFamilyProperties);
		return dest.get(0);
	}
	
	public static VkExtensionProperties.Buffer enumerateInstanceExtensionProperties(MemoryStack stack, IntBuffer dest){
		int count=enumerateInstanceExtensionProperties(null, dest, null);
		if(count==0) return null;
		
		VkExtensionProperties.Buffer extensions=VkExtensionProperties.callocStack(count, stack);
		enumerateInstanceExtensionProperties(null, dest, extensions);
		return extensions;
	}
	
	public static int enumerateInstanceExtensionProperties(String layerName, IntBuffer dest, VkExtensionProperties.Buffer properties){
		int code=VK10.vkEnumerateInstanceExtensionProperties(layerName, dest, properties);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkLayerProperties.Buffer enumerateInstanceLayerProperties(MemoryStack stack, IntBuffer dest){
		int count=enumerateInstanceLayerProperties(dest, null);
		if(count==0) return null;
		
		VkLayerProperties.Buffer layers=stack==null?VkLayerProperties.calloc(count):VkLayerProperties.callocStack(count, stack);
		enumerateInstanceLayerProperties(dest, layers);
		return layers;
	}
	
	public static int enumerateInstanceLayerProperties(IntBuffer dest, VkLayerProperties.Buffer properties){
		int code=VK10.vkEnumerateInstanceLayerProperties(dest, properties);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkDevice createDevice(VkPhysicalDevice physicalDevice, VkDeviceCreateInfo deviceCreateInfo, PointerBuffer pp){
		int code=VK10.vkCreateDevice(physicalDevice, deviceCreateInfo, null, pp);
		if(DEBUG) check(code);
		return new VkDevice(pp.get(0), physicalDevice, deviceCreateInfo);
	}
	
	public static VkQueue createDeviceQueue(VkDevice device, int queueFamilyIndex, int queueIndex, PointerBuffer dest){
		return new VkQueue(Vk.getDeviceQueue(device, queueFamilyIndex, queueIndex, dest), device);
	}
	
	public static void destroyInstance(VkInstance instance){
		VK10.vkDestroyInstance(instance, null);
	}
	
	public static void destroyDevice(VkDevice device){
		VK10.vkDestroyDevice(device, null);
	}
	
	public static long getDeviceQueue(VkDevice device, int queueFamilyIndex, int queueIndex, PointerBuffer dest){
		VK10.vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, dest);
		return dest.get(0);
	}
	
	public static boolean getPhysicalDeviceSurfaceSupportKHR(VkPhysicalDevice physicalDevice, int queueFamilyIndex, GlfwWindow window, IntBuffer dest){
		if(DEBUG){
			Objects.requireNonNull(physicalDevice);
			Objects.requireNonNull(window);
			if(window.getSurface()<=0) throw new NullPointerException();
		}
		int code=KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamilyIndex, window.getSurface(), dest);
		if(DEBUG) check(code);
		return dest.get(0)==VK10.VK_TRUE;
	}
	
	public static void getPhysicalDeviceSurfaceCapabilitiesKHR(VkPhysicalDevice physicalDevice, GlfwWindow window, VkSurfaceCapabilitiesKHR surfaceCapabilities){
		int code=KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, window.getSurface(), surfaceCapabilities);
		if(DEBUG) check(code);
	}
	
	public static int getPhysicalDeviceSurfaceFormatsKHR(VkPhysicalDevice physicalDevice, GlfwWindow window, IntBuffer dest){
		return getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window, dest, null);
	}
	
	public static int getPhysicalDeviceSurfaceFormatsKHR(VkPhysicalDevice physicalDevice, GlfwWindow window, IntBuffer dest, VkSurfaceFormatKHR.Buffer surfaceFormat){
		int code=KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window.getSurface(), dest, surfaceFormat);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static int getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, GlfwWindow window, IntBuffer dest){
		return getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window, dest, null);
	}
	
	public static int getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, GlfwWindow window, IntBuffer dest, IntBuffer presentModes){
		int code=KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window.getSurface(), dest, presentModes);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static long createSwapchainKHR(VkDevice device, VkSwapchainCreateInfoKHR info, LongBuffer dest){
		int code=KHRSwapchain.vkCreateSwapchainKHR(device, info, null, dest);
		if(DEBUG) check(code);
		return dest.get(0);
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
		return stringsToPP(strings, stack.callocPointer(size));
	}
	
	public static PointerBuffer stringsToPP(Iterable<? extends CharSequence> strings, PointerBuffer dest){
		int i=0;
		for(CharSequence s : strings){
			dest.put(i++, MemoryUtil.memASCII(s));
		}
		return dest;
	}
	
	public static void destroySwapchainKHR(VkDevice device, VkSwapchain swapchain){
		vkDestroySwapchainKHR(device, swapchain.getId(), null);
	}
	
	public static int getSwapchainImagesKHR(VkDevice device, VkSwapchain swapChain, IntBuffer dest, LongBuffer images){
		int code=vkGetSwapchainImagesKHR(device, swapChain.getId(), dest, images);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static int getSwapchainImagesKHR(VkDevice device, VkSwapchain swapChain, IntBuffer dest){
		return getSwapchainImagesKHR(device, swapChain, dest, null);
	}
	
	public static void stack(Consumer<MemoryStack> run){
		try(MemoryStack stack=stackPush()){
			run.accept(stack);
		}
	}
	
	public static VkImageView createImageView(VkGpuCtx gpuCtx, VkImageViewCreateInfo createInfo, LongBuffer dest){
		int code=VK10.vkCreateImageView(gpuCtx.getGpuDevice(), createInfo, null, dest);
		if(DEBUG) check(code);
		return new VkImageView(gpuCtx, dest.get(0));
	}
	
	public static VkShaderModule createShaderModule(VkGpu gpu, byte[] code, Shader.Type type){
		VkShaderModule res=new VkShaderModule(gpu, type);
		res.create(code);
		return res;
	}
	
	public static long createShaderModule(VkDevice device, VkShaderModuleCreateInfo info, LongBuffer dest){
		int code=VK10.vkCreateShaderModule(device, info, null, dest);
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkPipelineLayout createPipelineLayout(VkGpuCtx gpuCtx, VkPipelineLayoutCreateInfo createInfo, LongBuffer dest){
		int code=VK10.vkCreatePipelineLayout(gpuCtx.getGpuDevice(), createInfo, null, dest);
		if(DEBUG) check(code);
		return new VkPipelineLayout(gpuCtx, dest.get(0));
	}
	
	public static VkRenderPass createRenderPass(VkGpuCtx gpuCtx, VkRenderPassCreateInfo renderPassInfo, LongBuffer dest){
		int code=VK10.vkCreateRenderPass(gpuCtx.getGpuDevice(), renderPassInfo, null, dest);
		if(DEBUG) check(code);
		return new VkRenderPass(gpuCtx, dest.get(0));
	}
	
	public static VkGraphicsPipeline[] createGraphicsPipelines(VkGpuCtx gpuCtx, VkGraphicsPipelineCreateInfo.Buffer pipelineInfo, LongBuffer dest){
		int code=VK10.vkCreateGraphicsPipelines(gpuCtx.getGpuDevice(), 0, pipelineInfo, null, dest);
		if(DEBUG) check(code);
		VkGraphicsPipeline[] res=new VkGraphicsPipeline[pipelineInfo.remaining()];
		for(int i=0, j=pipelineInfo.capacity();i<j;i++){
			res[i]=new VkGraphicsPipeline(gpuCtx, dest.get(i));
		}
		return res;
	}
	
	public static VkFramebuffer createFrameBuffer(VkGpuCtx gpuCtx, VkFramebufferCreateInfo framebufferInfo, LongBuffer dest){
		int code=VK10.vkCreateFramebuffer(gpuCtx.getGpuDevice(), framebufferInfo, null, dest);
		if(DEBUG) check(code);
		return new VkFramebuffer(gpuCtx.getGpu(), dest.get(0));
	}
	
	public static VkCommandPool createCommandPool(VkGpuCtx gpuCtx, VkCommandPoolCreateInfo poolInfo, LongBuffer dest){
		int code=VK10.vkCreateCommandPool(gpuCtx.getGpuDevice(), poolInfo, null, dest);
		if(DEBUG) check(code);
		return new VkCommandPool(gpuCtx, dest.get(0));
	}
	
	public static VkCommandBuffer[] allocateCommandBuffers(VkDevice device, VkCommandBufferAllocateInfo allocInfo, PointerBuffer dest){
		int code=vkAllocateCommandBuffers(device, allocInfo, dest);
		if(DEBUG) check(code);
		VkCommandBuffer[] res=new VkCommandBuffer[allocInfo.commandBufferCount()];
		for(int i=0;i<res.length;i++) res[i]=new VkCommandBuffer(dest.get(i), device);
		return res;
	}
	
	public static void beginCommandBuffer(VkCommandBuffer commandBuffer, VkCommandBufferBeginInfo beginInfo){
		int code=vkBeginCommandBuffer(commandBuffer, beginInfo);
		if(DEBUG) check(code);
	}
	
	public static void endCommandBuffer(VkCommandBuffer commandBuffer){
		int code=vkEndCommandBuffer(commandBuffer);
		if(DEBUG) check(code);
	}
	
	public static VkSemaphore createSemaphore(VkGpuCtx gpuCtx, VkSemaphoreCreateInfo semaphoreInfo){
		LongBuffer dest=memAllocLong(1);
		
		int code=VK10.vkCreateSemaphore(gpuCtx.getGpuDevice(), semaphoreInfo, null, dest);
		if(DEBUG) check(code);
		return new VkSemaphore(gpuCtx.getGpu(), dest);
	}
	
	public static int acquireNextImageKHR(VkDevice device, VkSwapchain swapChain, VkSemaphore semaphore, IntBuffer dest){
		int code=vkAcquireNextImageKHR(device, swapChain.getId(), Long.MAX_VALUE, semaphore.get(), 0, dest);
		if(code==VK_ERROR_OUT_OF_DATE_KHR) return -1;
		if(DEBUG) check(code);
		return dest.get(0);
	}
	
	public static VkBuffer createBuffer(VkGpuCtx gpuCtx, VkBufferCreateInfo bufferInfo){
		LongBuffer dest=memAllocLong(1);
		int        code=vkCreateBuffer(gpuCtx.getGpuDevice(), bufferInfo, null, dest);
		if(DEBUG) check(code);
		return new VkBuffer(gpuCtx, dest, bufferInfo.size());
	}
	
	public static VkDeviceMemory allocateMemory(VkGpuCtx gpuCtx, VkMemoryAllocateInfo allocInfo, LongBuffer dest){
		int code=vkAllocateMemory(gpuCtx.getGpuDevice(), allocInfo, null, dest);
		if(DEBUG) check(code);
		return new VkDeviceMemory(gpuCtx, dest.get(0));
		
	}
	
	public static void bindBufferMemory(VkDevice device, VkBuffer buffer, VkDeviceMemory mem, int offset){
		int code=vkBindBufferMemory(device, buffer.get(), mem.get(), offset);
		if(DEBUG) check(code);
	}
	
	public static void bindImageMemory(VkDevice device, VkImage image, VkDeviceMemory mem, int offset){
		int code=vkBindImageMemory(device, image.get(), mem.get(), offset);
		if(DEBUG) check(code);
	}
	
	public static PointerBuffer mapMemory(VkDevice device, VkDeviceMemory memory, int offset, long size, int flags, PointerBuffer dest){
		int code=vkMapMemory(device, memory.get(), offset, size, flags, dest);
		if(DEBUG) check(code);
		return dest;
	}
	
	public static void unmapMemory(VkDevice device, VkDeviceMemory memory){
		vkUnmapMemory(device, memory.get());
	}
	
	public static void queuePresentKHR(VkQueue queue, VkPresentInfoKHR presentInfo){
		int code=vkQueuePresentKHR(queue, presentInfo);
		if(DEBUG) check(code);
	}
	
	public static VkClearValue.Buffer clearCol(IColorM src, VkClearValue.Buffer dest){
		dest.color()
		    .float32(0, src.r())
		    .float32(1, src.g())
		    .float32(2, src.b())
		    .float32(3, src.a());
		return dest;
	}
	
	public static VkClearValue.Buffer clearDepth(VkClearValue.Buffer dest){
//		dest.depthStencil().depth(1).stencil(0);
		return dest;
	}
	
	public static VkBufferCreateInfo bufferInfo(MemoryStack stack, int size, int usage){
		return VkBufferCreateInfo.callocStack(stack)
		                         .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
		                         .size(size)
		                         .usage(usage)
		                         .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
	}
	
	public static void queueWaitIdle(VkQueue queue){
		int code=vkQueueWaitIdle(queue);
		if(DEBUG) check(code);
	}
	
	public static void queueSubmit(VkQueue queue, VkSubmitInfo info){
		queueSubmit(queue, info, VkFence.NULL);
	}
	
	public static void queueSubmit(VkQueue queue, VkSubmitInfo info, VkFence fence){
		int code=vkQueueSubmit(queue, info, fence.get());
		if(DEBUG) check(code);
	}
	
	public static VkDescriptorSetLayout createDescriptorSetLayout(VkGpuCtx gpuCtx, VkDescriptorSetLayoutCreateInfo info, MemoryStack stack){
		return createDescriptorSetLayout(gpuCtx, info, stack.callocLong(1));
	}
	
	public static VkDescriptorSetLayout createDescriptorSetLayout(VkGpuCtx gpuCtx, VkDescriptorSetLayoutCreateInfo info, LongBuffer dest){
		int code=vkCreateDescriptorSetLayout(gpuCtx.getGpuDevice(), info, null, dest);
		if(DEBUG) check(code);
		return new VkDescriptorSetLayout(gpuCtx, dest.get(0));
	}
	
	public static VkDescriptorPool createDescriptorPool(VkGpuCtx gpuCtx, VkDescriptorPoolCreateInfo info, MemoryStack stack){
		return createDescriptorPool(gpuCtx, info, stack.callocLong(1));
	}
	
	public static VkDescriptorPool createDescriptorPool(VkGpuCtx gpuCtx, VkDescriptorPoolCreateInfo info, LongBuffer dest){
		int code=vkCreateDescriptorPool(gpuCtx.getGpuDevice(), info, null, dest);
		if(DEBUG) check(code);
		return new VkDescriptorPool(gpuCtx, dest.get(0));
	}
	
	public static VkDescriptorSet allocateDescriptorSets(VkDevice device, VkDescriptorSetAllocateInfo info, MemoryStack stack){
		return allocateDescriptorSets(device, info, stack.callocLong(1));
	}
	
	public static VkDescriptorSet allocateDescriptorSets(VkDevice device, VkDescriptorSetAllocateInfo info, LongBuffer dest){
		int code=vkAllocateDescriptorSets(device, info, dest);
		if(DEBUG) check(code);
		return new VkDescriptorSet(dest.get(0));
		
	}
	
	public static void flushMappedMemoryRanges(VkDevice device, VkMappedMemoryRange info){
		int code=vkFlushMappedMemoryRanges(device, info);
		if(DEBUG) check(code);
	}
	
	public static void invalidateMappedMemoryRanges(VkDevice device, VkMappedMemoryRange info){
		int code=vkInvalidateMappedMemoryRanges(device, info);
		if(DEBUG) check(code);
	}
	
	public static VkFence createFence(VkGpuCtx gpuCtx, VkFenceCreateInfo fenceInfo){
		LongBuffer buff=memAllocLong(1);
		
		int code=vkCreateFence(gpuCtx.getGpuDevice(), fenceInfo, null, buff);
		if(DEBUG) check(code);
		return new VkFence(gpuCtx, buff);
	}
	
	public static void waitForFences(VkDevice device, LongBuffer fence){
		int code=vkWaitForFences(device, fence, true, Integer.MAX_VALUE);
		if(DEBUG) check(code);
	}
	
	public static VkImage createImage(VkGpuCtx gpuCtx, VkImageCreateInfo imageInfo, LongBuffer dest){
		int code=vkCreateImage(gpuCtx.getGpuDevice(), imageInfo, null, dest);
		if(DEBUG) check(code);
		return new VkImage(gpuCtx, dest.get(0), imageInfo.extent().width(), imageInfo.extent().height(), VkImageFormat.fromValue(imageInfo.format()));
	}
	
	public static VkSampler createSampler(VkGpuCtx gpuCtx, VkSamplerCreateInfo samplerInfo, LongBuffer dest){
		int code=vkCreateSampler(gpuCtx.getGpuDevice(), samplerInfo, null, dest);
		if(DEBUG) check(code);
		return new VkSampler(gpuCtx, dest.get(0));
	}
	
	public static VkImageCreateInfo imageCreateInfo(MemoryStack stack, int width, int height, int format, VkGpu.Feature feature, int usage){
		return imageCreateInfo(VkImageCreateInfo.callocStack(stack), width, height, format, feature, usage);
	}
	
	public static VkImageCreateInfo imageCreateInfo(VkImageCreateInfo imageInfo, int width, int height, int format, VkGpu.Feature feature, int usage){
		imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
		         .imageType(VK_IMAGE_TYPE_2D)
		         .mipLevels(1)
		         .arrayLayers(1)
		         .format(format)
		         .tiling(feature.tiling)
		         .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
		         .usage(usage)
		         .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
		         .samples(VK_SAMPLE_COUNT_1_BIT);
		imageInfo.extent().set(width, height, 1);
		return imageInfo;
	}
}
