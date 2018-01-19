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
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.lapissea.vulkanimpl.VulkanRenderer.*;
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
		CODES.put(VK_ERROR_INITIALIZATION_FAILED, "Initialization of an object could not be completed for implementation-specific reasons.");
		CODES.put(VK_ERROR_DEVICE_LOST, "The logical or physical device has been lost.");
		CODES.put(VK_ERROR_MEMORY_MAP_FAILED, "Mapping of a memory object has failed.");
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
	
	public static VkApplicationInfo initAppInfo(MemoryStack stack, String name){
		ByteBuffer        nam       =stack.UTF8(name);
		ByteBuffer        engineName=stack.UTF8("LapisEngine");
		VkApplicationInfo info      =VkApplicationInfo.callocStack(stack);
		info.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		info.pNext(MemoryUtil.NULL);
		info.pApplicationName(nam);
		info.pEngineName(engineName);
		info.applicationVersion(VK_MAKE_VERSION(0, 1, 0));
		info.engineVersion(VK_MAKE_VERSION(0, 1, 0));
		info.apiVersion(VK_API_VERSION_1_0);
		return info;
	}
	
	public static VkInstanceCreateInfo initInstanceInfo(MemoryStack stack, VkApplicationInfo app){
		VkInstanceCreateInfo info=VkInstanceCreateInfo.callocStack(stack);
		info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		info.pNext(MemoryUtil.NULL);
		info.flags(0);
		info.pApplicationInfo(app);
		info.ppEnabledExtensionNames(null);
		info.ppEnabledLayerNames(null);
		return info;
	}
	
	public static VkInstance createInstance(VkInstanceCreateInfo instanceInfo, PointerBuffer pp){
		int err=vkCreateInstance(instanceInfo, null, pp);
		if(err==0) return new VkInstance(pp.get(0), instanceInfo);
		if(err==VK_ERROR_INCOMPATIBLE_DRIVER) throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD).");
		if(err==VK_ERROR_EXTENSION_NOT_PRESENT) throw new IllegalStateException("Cannot find a specified extension library. Make sure your layers path is set appropriately.");
		throw new IllegalStateException("vkCreateInstance failed. Do you have a compatible Vulkan installable client driver (ICD) installed?");
		
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
		int code=vkEnumerateDeviceExtensionProperties(physicalDevice, layerName, dest, properties);
		if(DEVELOPMENT) check(code);
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
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, pQueueFamilyProperties);
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
		int code=vkEnumerateInstanceExtensionProperties(layerName, dest, properties);
		if(DEVELOPMENT) check(code);
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
		int code=vkEnumerateInstanceLayerProperties(dest, properties);
		if(DEVELOPMENT) check(code);
		return dest.get(0);
	}
	
	public static VkDevice createDevice(VkPhysicalDevice physicalDevice, VkDeviceCreateInfo deviceCreateInfo, PointerBuffer pp){
		int code=vkCreateDevice(physicalDevice, deviceCreateInfo, null, pp);
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
	
	public static boolean getPhysicalDeviceSurfaceSupportKHR(VkPhysicalDevice physicalDevice, int queueFamilyIndex, GlfwWindowVk window, IntBuffer dest){
		if(DEVELOPMENT){
			Objects.requireNonNull(physicalDevice);
			Objects.requireNonNull(window);
			if(window.getSurface()<=0) throw new NullPointerException();
		}
		int code=KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamilyIndex, window.getSurface(), dest);
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
		return stringsToPP(strings, stack.callocPointer(size));
	}
	
	public static PointerBuffer stringsToPP(Iterable<? extends CharSequence> strings, PointerBuffer dest){
		int i=0;
		for(CharSequence s : strings){
			dest.put(i++, MemoryUtil.memASCII(s));
		}
		return dest;
	}
	
	/*/START_GEN/*/
	//lel
	
	
	
	/*/END_GEN/*/
}
