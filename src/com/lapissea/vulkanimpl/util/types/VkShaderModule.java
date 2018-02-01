package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkShaderModule implements VkDestroyable, VkGpuCtx{
	
	
	public static VkShaderModule create(VkShader shader, ByteBuffer spvCode, VkShader.Type type, MemoryStack stack){
		VkShaderModuleCreateInfo createInfo=VkShaderModuleCreateInfo.callocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
		          .pCode(spvCode);
		return create(shader, createInfo, type);
	}
	
	public static VkShaderModule create(VkShader shader, VkShaderModuleCreateInfo createInfo, VkShader.Type type){
		LongBuffer handle=memAllocLong(1);
		try{
			int code=vkCreateShaderModule(shader.getGpu().getDevice(), createInfo, null, handle);
			if(DEVELOPMENT) Vk.check(code);
			return new VkShaderModule(shader, handle, type);
		}catch(Throwable t){
			memFree(handle);
			throw t;
		}
	}
	
	private final LongBuffer    handle;
	private final VkShader      parent;
	public final  VkShader.Type type;
	
	public VkShaderModule(VkShader parent, LongBuffer handle, VkShader.Type type){
		this.parent=parent;
		this.handle=handle;
		this.type=type;
	}
	
	@Override
	public void destroy(){
		vkDestroyShaderModule(getGpu().getDevice(), getHandle(), null);
		memFree(handle);
	}
	
	@Override
	public VkGpu getGpu(){
		return parent.getGpu();
	}
	
	public long getHandle(){
		return handle.get(0);
	}
}
