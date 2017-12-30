package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkShaderModule implements VkDestroyable, VkGpuCtx{
	private       long        id;
	public final  Shader.Type type;
	private final VkGpu       gpu;
	
	public VkShaderModule(VkGpu gpu, Shader.Type type){
		this.type=type;
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpu);
		this.gpu=gpu;
	}
	
	
	public VkPipelineShaderStageCreateInfo pipelineShaderStageCreate(MemoryStack stack){
		VkPipelineShaderStageCreateInfo shaderStageInfo=VkPipelineShaderStageCreateInfo.callocStack(stack);
		shaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
		shaderStageInfo.stage(type.stageBit);
		shaderStageInfo.module(id);
		shaderStageInfo.pName(stack.ASCII("main"));
		shaderStageInfo.pSpecializationInfo(null);
		return shaderStageInfo;
	}
	
	public void create(byte[] code){
		VkShaderModuleCreateInfo createInfo=VkShaderModuleCreateInfo.calloc();
		createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
		
		ByteBuffer buff=memAlloc(code.length);
		for(int i=0;i<code.length;i++){
			buff.put(i, code[i]);
		}
		createInfo.pCode(buff);
		
		LongBuffer res=memAllocLong(1);
		id=Vk.createShaderModule(getGpuDevice(), createInfo, res);
		
		memFree(buff);
		memFree(res);
	}
	
	@Override
	public void destroy(){
		vkDestroyShaderModule(getGpuDevice(), id, null);
		id=0;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
