package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRender implements VkDestroyable{
	
	private IntBuffer        imageIndex;
	private IntBuffer        waitDstStageMask;
	private PointerBuffer    commandBuffers;
	private VkSubmitInfo     mainRenderSubmitInfo;
	private VkPresentInfoKHR presentInfo;
	
	public VulkanRender(){
		imageIndex=memAllocInt(1);
		commandBuffers=memAllocPointer(1);
		waitDstStageMask=memAllocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		mainRenderSubmitInfo=VkConstruct.submitInfo().pWaitDstStageMask(waitDstStageMask).pCommandBuffers(commandBuffers);
		presentInfo=VkConstruct.presentInfoKHR().pImageIndices(imageIndex);
	}
	
	public boolean render(SurfaceContext ctx, VkSwapchain.Frame frame){
		
		commandBuffers.put(0,ctx.getSceneCommandBuffers()[frame.index]);
		
		LongBuffer imgAvailable=ctx.getSwapchain().getImageAviable().getBuffer();
		LongBuffer renderFinish=frame.getRenderFinish().getBuffer();
		LongBuffer swapchains  =ctx.getSwapchain().getBuffer();
		
		mainRenderSubmitInfo.pWaitSemaphores(imgAvailable)
		                    .waitSemaphoreCount(imgAvailable.limit())
		                    .pSignalSemaphores(renderFinish);
		
		imageIndex.put(0, frame.index);
		presentInfo.pWaitSemaphores(renderFinish)
		           .pSwapchains(swapchains)
		           .swapchainCount(swapchains.limit());
		
		VkGpu gpu=ctx.getSwapchain().getGpu();
		gpu.getGraphicsQueue().submit(mainRenderSubmitInfo);
		return gpu.getSurfaceQueue().present(presentInfo);
	}
	
	@Override
	public void destroy(){
		mainRenderSubmitInfo.free();
		presentInfo.free();
		memFree(imageIndex);
		memFree(waitDstStageMask);
		memFree(commandBuffers);
	}
}
