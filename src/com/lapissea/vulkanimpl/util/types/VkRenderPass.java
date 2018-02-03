package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkRenderPass implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu gpu;
	private final long  handle;
	
	public VkRenderPass(VkGpu gpu, long handle){
		this.gpu=gpu;
		this.handle=handle;
	}
	
	public void begin(long frameBuffer){
		try(MemoryStack stack=stackPush()){
			
			VkRenderPassBeginInfo renderPassInfo=VkRenderPassBeginInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
			              .renderPass(handle)
			              .framebuffer(frameBuffer);
		}
	}
	
	@Override
	public void destroy(){
		VK10.vkDestroyRenderPass(getDevice(), handle, null);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public long getHandle(){
		return handle;
	}
}
