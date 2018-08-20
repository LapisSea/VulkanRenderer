package com.lapissea.vulkanimpl.exceptions;

import com.lapissea.util.PairM;
import com.lapissea.util.TextUtil;
import com.lapissea.util.ZeroArrays;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.function.Function;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkException extends RuntimeException{
	
	public static class OutOfHostMemory extends VkException{
		private OutOfHostMemory(String message){
			super(message);
		}
	}
	
	public static class OutOfDeviceMemory extends VkException{
		private OutOfDeviceMemory(String message){
			super(message);
		}
	}
	
	public static class InitializationFailed extends VkException{
		private InitializationFailed(String message){
			super(message);
		}
	}
	
	public static class DeviceLost extends VkException{
		private DeviceLost(String message){
			super(message);
		}
	}
	
	public static class MemoryMapFailed extends VkException{
		private MemoryMapFailed(String message){
			super(message);
		}
	}
	
	public static class LayerNotPresent extends VkException{
		private LayerNotPresent(String message){
			super(message);
		}
	}
	
	public static class ExtensionNotPresent extends VkException{
		private ExtensionNotPresent(String message){
			super(message);
		}
	}
	
	public static class FeatureNotPresent extends VkException{
		private FeatureNotPresent(String message){
			super(message);
		}
	}
	
	public static class IncompatibleDriver extends VkException{
		private IncompatibleDriver(String message){
			super(message);
		}
	}
	
	public static class TooManyObjects extends VkException{
		private TooManyObjects(String message){
			super(message);
		}
	}
	
	public static class FormatNotSupported extends VkException{
		private FormatNotSupported(String message){
			super(message);
		}
	}
	
	public static class SurfaceLostKHR extends VkException{
		private SurfaceLostKHR(String message){
			super(message);
		}
	}
	
	public static class NativeWindowInUseKHR extends VkException{
		private NativeWindowInUseKHR(String message){
			super(message);
		}
	}
	
	public static class OutOfDateKHR extends VkException{
		private OutOfDateKHR(String message){
			super(message);
		}
	}
	
	public static class IncompatibleDisplayKHR extends VkException{
		private IncompatibleDisplayKHR(String message){
			super(message);
		}
	}
	
	public static class ValidationFailedEXT extends VkException{
		private ValidationFailedEXT(String message){
			super(message);
		}
	}
	
	
	private static final TIntObjectMap<PairM<String, Function<String, VkException>>> CODES=new TIntObjectHashMap<>();
	
	static{
//		if(DEV_ON) Generator_Vk.windowEventPoolThread();
		
		CODES.put(VK_SUCCESS, new PairM<>("Command successfully completed.", VkException::new));
		CODES.put(VK_NOT_READY, new PairM<>("A fence or query has not yet completed.", VkException::new));
		CODES.put(VK_TIMEOUT, new PairM<>("A wait operation has not completed in the specified time.", VkException::new));
		CODES.put(VK_EVENT_SET, new PairM<>("An event is signaled.", VkException::new));
		CODES.put(VK_EVENT_RESET, new PairM<>("An event is unsignaled.", VkException::new));
		CODES.put(VK_INCOMPLETE, new PairM<>("A return array was too small for the result.", VkException::new));
		CODES.put(VK_SUBOPTIMAL_KHR, new PairM<>("A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.", VkException::new));
		
		CODES.put(VK_ERROR_OUT_OF_HOST_MEMORY, new PairM<>("A host memory allocation has failed.", OutOfHostMemory::new));
		CODES.put(VK_ERROR_OUT_OF_DEVICE_MEMORY, new PairM<>("A device memory allocation has failed.", OutOfDeviceMemory::new));
		CODES.put(VK_ERROR_INITIALIZATION_FAILED, new PairM<>("Initialization of an bool could not be completed for implementation-specific reasons.", InitializationFailed::new));
		CODES.put(VK_ERROR_DEVICE_LOST, new PairM<>("The logical or physical device has been lost.", DeviceLost::new));
		CODES.put(VK_ERROR_MEMORY_MAP_FAILED, new PairM<>("Mapping of a memory bool has failed.", MemoryMapFailed::new));
		CODES.put(VK_ERROR_LAYER_NOT_PRESENT, new PairM<>("A requested layer is not present or could not be loaded.", LayerNotPresent::new));
		CODES.put(VK_ERROR_EXTENSION_NOT_PRESENT, new PairM<>("A requested extension is not supported.", ExtensionNotPresent::new));
		CODES.put(VK_ERROR_FEATURE_NOT_PRESENT, new PairM<>("A requested feature is not supported.", FeatureNotPresent::new));
		CODES.put(VK_ERROR_INCOMPATIBLE_DRIVER, new PairM<>("The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.", IncompatibleDriver::new));
		CODES.put(VK_ERROR_TOO_MANY_OBJECTS, new PairM<>("Too many objects of the type have already been created.", TooManyObjects::new));
		CODES.put(VK_ERROR_FORMAT_NOT_SUPPORTED, new PairM<>("A requested format is not supported on this device.", FormatNotSupported::new));
		CODES.put(VK_ERROR_SURFACE_LOST_KHR, new PairM<>("A surface is no longer available.", SurfaceLostKHR::new));
		CODES.put(VK_ERROR_NATIVE_WINDOW_IN_USE_KHR, new PairM<>("The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.", NativeWindowInUseKHR::new));
		CODES.put(VK_ERROR_OUT_OF_DATE_KHR, new PairM<>("A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue presenting to the surface.", OutOfDateKHR::new));
		CODES.put(VK_ERROR_INCOMPATIBLE_DISPLAY_KHR, new PairM<>("The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an image.", IncompatibleDisplayKHR::new));
		CODES.put(VK_ERROR_VALIDATION_FAILED_EXT, new PairM<>("A validation layer found an error.", ValidationFailedEXT::new));
	}
	
	private static String decode(int code){
		List<String> msg=TextUtil.wrapLongString(translateCode(code), 100);
		return msg.size()==1?msg.get(0):"\n"+TextUtil.wrappedString(msg.toArray(ZeroArrays.ZERO_STRING));
	}
	
	public VkException(int code){
		this(decode(code));
	}
	
	public static VkException throwByCode(int code){
		var p=CODES.get(code);
		if(p==null) throw new VkException("Unknown ["+code+"]");
		throw p.obj2.apply(p.obj1);
	}
	
	public static String translateCode(int code){
		var message=CODES.get(code);
		return message==null?"Unknown ["+code+"]":message.obj1;
	}
	
	public VkException(){ }
	
	public VkException(String message){
		super(message);
	}
	
	public VkException(String message, Throwable cause){
		super(message, cause);
	}
	
	public VkException(Throwable cause){
		super(cause);
	}
	
}
