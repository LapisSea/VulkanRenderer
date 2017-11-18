package com.lapissea.vulkanimpl;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class VkShaderModule{
	private      long        id;
	public final Shader.Type type;
	
	public VkShaderModule(Shader.Type type){
		this.type=type;
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
	
	public void create(VkDevice device, byte[] code){
		VkShaderModuleCreateInfo createInfo=VkShaderModuleCreateInfo.calloc();
		createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
		
		ByteBuffer buff=memAlloc(code.length);
		for(int i=0;i<code.length;i++){
			buff.put(i, code[i]);
		}
		createInfo.pCode(buff);
		
		LongBuffer res=memAllocLong(1);
		id=Vk.createShaderModule(device, createInfo, res);
		
		memFree(buff);
		memFree(res);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		VK10.vkDestroyShaderModule(device, id, null);
		id=0;
	}
}
