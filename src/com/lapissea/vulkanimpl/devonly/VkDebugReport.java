package com.lapissea.vulkanimpl.devonly;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VulkanRenderer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.lapissea.vulkanimpl.VulkanRenderer.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;

public class VkDebugReport{
	
	static{ if(!DEVELOPMENT) throw new RuntimeException(); }
	
	public enum Type{
		INFORMATION(EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT),
		DEBUG(EXTDebugReport.VK_DEBUG_REPORT_DEBUG_BIT_EXT),
		WARNING(EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT),
		PERFORMANCE_WARNING(EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT),
		ERROR(EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT),
		UNKNOWN(Integer.MIN_VALUE);
		
		public final int id;
		
		Type(int id){
			this.id=id;
		}
		
		public boolean is(int flags){
			return (flags&id)!=0;
		}
	}
	
	public interface Hook{
		void invoke(Type type, String prefix, int code, String message);
	}
	
	private final long           callback;
	private final VulkanRenderer instance;
	
	public VkDebugReport(VulkanRenderer instance, MemoryStack stack, VkDebugReport.Hook hook){
		this(instance, stack.mallocLong(1), stack, (flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData)->{
			Type type=Type.UNKNOWN;
			for(Type t : Type.values()){
				if(t.is(flags)){
					type=t;
					break;
				}
			}
			
			hook.invoke(type, MemoryUtil.memASCII(pLayerPrefix), messageCode, VkDebugReportCallbackEXT.getString(pMessage));
		
			/*
			 * false indicates that layer should not bail-out of an
			 * API call that had validation failures. This may mean that the
			 * app dies inside the driver due to invalid parameter(s).
			 * That's what would happen without validation layers, so we'll
			 * keep that behavior here.
			 */
			return VK10.VK_FALSE;
		});
	}
	
	public VkDebugReport(VulkanRenderer instance, LongBuffer lb, MemoryStack stack, VkDebugReportCallbackEXTI hook){
		this(instance, lb, VkDebugReportCallbackCreateInfoEXT.mallocStack(stack).sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
		                                                     .pNext(NULL)
		                                                     .flags(VK_DEBUG_REPORT_INFORMATION_BIT_EXT|
		                                                            VK_DEBUG_REPORT_WARNING_BIT_EXT|
		                                                            VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT|
		                                                            VK_DEBUG_REPORT_ERROR_BIT_EXT|
		                                                            VK_DEBUG_REPORT_DEBUG_BIT_EXT)
		                                                     .pfnCallback(VkDebugReportCallbackEXT.create(hook))
		                                                     .pUserData(NULL));
	}
	
	public VkDebugReport(VulkanRenderer instance, LongBuffer lb, VkDebugReportCallbackCreateInfoEXT createInfo){
		this.instance=instance;
		callback=createDebugReportCallbackEXT(instance.getInstance(), createInfo, null, lb);
	}
	
	public void destroy(){
		destroyDebugReportCallbackEXT(instance.getInstance(), callback, null);
	}
	
	
	public static long createDebugReportCallbackEXT(VkInstance instance, VkDebugReportCallbackCreateInfoEXT dbgCreateInfo, VkAllocationCallbacks allocator, LongBuffer dest){
		int code=vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, allocator, dest);
		if(DEVELOPMENT) Vk.check(code);
		return dest.get(0);
	}
	
	public static void destroyDebugReportCallbackEXT(VkInstance instance, long callback, VkAllocationCallbacks allocator){
		vkDestroyDebugReportCallbackEXT(instance, callback, allocator);
	}
}
