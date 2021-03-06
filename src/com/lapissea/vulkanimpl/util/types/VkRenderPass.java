package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vec.color.IColorMSolid;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkRenderPass implements VkDestroyable, VkGpuCtx{
	
	private final VkGpu gpu;
	private final long  handle;
	public final  int   samples;
	
	public VkRenderPass(VkGpu gpu, long handle, int samples){
		this.gpu=gpu;
		this.handle=handle;
		this.samples=samples;
	}
	
	public void begin(VkCommandBufferM cmd, long frameBuffer, VkExtent2D size, IColorMSolid clearColor){
		try(MemoryStack stack=stackPush()){
			
			VkRenderPassBeginInfo renderPassInfo=VkRenderPassBeginInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
			              .renderPass(handle)
			              .framebuffer(frameBuffer);
			renderPassInfo.renderArea().offset().set(0, 0);
			renderPassInfo.renderArea().extent(size);
			VkClearValue.Buffer clearColorBuff=VkClearValue.callocStack(2, stack);
			clearColorBuff.get(0).color()
			              .float32(0, clearColor.r())
			              .float32(1, clearColor.g())
			              .float32(2, clearColor.b())
			              .float32(3, 1);
			
			clearColorBuff.get(1).depthStencil().set(1, 0);
			renderPassInfo.pClearValues(clearColorBuff);
			
			vkCmdBeginRenderPass(cmd, renderPassInfo, cmd.isPrimary()?VK_SUBPASS_CONTENTS_INLINE:VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS);
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
