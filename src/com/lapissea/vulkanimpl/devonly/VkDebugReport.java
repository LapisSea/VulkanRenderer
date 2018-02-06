package com.lapissea.vulkanimpl.devonly;

import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VulkanRenderer;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;

public class VkDebugReport{
	
	static{ DevelopmentInfo.checkOnLoad(); }
	
	public VkDebugReport(VulkanRenderer renderer, List<Type> errTypes, List<Type> outTypes){
		this(renderer, Stream.concat(errTypes.stream(), outTypes.stream()).collect(Collectors.toList()), (type, prefix, code, message)->{
			if(message.startsWith("Device Extension")) return;
			
			List<String>  msgLin=TextUtil.wrapLongString(message, 100);
			StringBuilder msg   =new StringBuilder().append(type).append(": [").append(prefix).append("] Code ").append(code).append(": ");
			
			if(msgLin.size()>1) msg.append("\n").append(TextUtil.wrappedString(UtilL.array(msgLin)));
			else msg.append(msgLin.get(0));
			
			if(errTypes.contains(type)) throw new RuntimeException(msg.toString());
			LogUtil.println(msg);
		});
	}
	
	public enum Type{
		INFORMATION(VK_DEBUG_REPORT_INFORMATION_BIT_EXT),
		DEBUG(VK_DEBUG_REPORT_DEBUG_BIT_EXT),
		WARNING(VK_DEBUG_REPORT_WARNING_BIT_EXT),
		PERFORMANCE_WARNING(VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT),
		ERROR(VK_DEBUG_REPORT_ERROR_BIT_EXT),
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
	
	public VkDebugReport(VulkanRenderer instance, Iterable<Type> reportTypes, VkDebugReport.Hook hook){
		this(instance, reportTypes, (flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData)->{
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
	
	public VkDebugReport(VulkanRenderer instance, Iterable<Type> reportTypes, VkDebugReportCallbackEXTI hook){
		this.instance=instance;
		try(MemoryStack stack=stackPush()){
			int flags=0;
			for(Type reportType : reportTypes){
				flags|=reportType.id;
			}
			VkDebugReportCallbackCreateInfoEXT createInfo=VkDebugReportCallbackCreateInfoEXT.mallocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
			          .pNext(NULL)
			          .flags(flags)
			          .pfnCallback(VkDebugReportCallbackEXT.create(hook))
			          .pUserData(NULL);
			callback=createDebugReportCallbackEXT(instance.getInstance(), createInfo, null, stack.mallocLong(1));
			
		}
	}
	
	public VkDebugReport(VulkanRenderer instance, VkDebugReportCallbackCreateInfoEXT createInfo){
		this.instance=instance;
		try(MemoryStack stack=stackPush()){
			callback=createDebugReportCallbackEXT(instance.getInstance(), createInfo, null, stack.mallocLong(1));
		}
	}
	
	
	public void destroy(){
		destroyDebugReportCallbackEXT(instance.getInstance(), callback, null);
	}
	
	
	public static long createDebugReportCallbackEXT(VkInstance instance, VkDebugReportCallbackCreateInfoEXT dbgCreateInfo, VkAllocationCallbacks allocator, LongBuffer dest){
		int code=vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, allocator, dest);
		if(DevelopmentInfo.DEV_ON) Vk.check(code);
		return dest.get(0);
	}
	
	public static void destroyDebugReportCallbackEXT(VkInstance instance, long callback, VkAllocationCallbacks allocator){
		vkDestroyDebugReportCallbackEXT(instance, callback, allocator);
	}
}
