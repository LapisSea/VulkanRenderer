package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRender implements VkDestroyable{
	
	private IntBuffer        imageIndex;
	private IntBuffer        waitDstStageMask;
	private PointerBuffer    singlePointerBuffer;
	private VkSubmitInfo     mainRenderSubmitInfo;
	private VkPresentInfoKHR presentInfo;
	
	public VulkanRender(){
		imageIndex=memAllocInt(1);
		singlePointerBuffer=memAllocPointer(1);
		waitDstStageMask=memAllocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
		mainRenderSubmitInfo=VkConstruct.submitInfo().pWaitDstStageMask(waitDstStageMask);
		presentInfo=VkConstruct.presentInfoKHR().pImageIndices(imageIndex);
	}
	
	public boolean render(VkSwapchain swapchain, VkSwapchain.Frame frame, Pointer commandBuffer){
		singlePointerBuffer.put(0, commandBuffer);
		return render(swapchain, frame, singlePointerBuffer);
	}
	
	public boolean render(VkSwapchain swapchain, VkSwapchain.Frame frame, PointerBuffer commandBuffers){
		
		LongBuffer imgAvailable=swapchain.getImageAviable().getBuffer();
		LongBuffer renderFinish=frame.getRenderFinish().getBuffer();
		LongBuffer swapchains  =swapchain.getBuffer();
		
		mainRenderSubmitInfo.pWaitSemaphores(imgAvailable)
		                    .waitSemaphoreCount(imgAvailable.limit())
		                    .pSignalSemaphores(renderFinish)
		                    .pCommandBuffers(commandBuffers);
		
		imageIndex.put(0, frame.index);
		presentInfo.pWaitSemaphores(renderFinish)
		           .pSwapchains(swapchains)
		           .swapchainCount(swapchains.limit());
		
		VkGpu gpu=swapchain.getGpu();
		gpu.getGraphicsQueue().submit(mainRenderSubmitInfo);
		return gpu.getSurfaceQueue().present(presentInfo);
	}
	
	@Override
	public void destroy(){
		mainRenderSubmitInfo.free();
		presentInfo.free();
		memFree(imageIndex);
		memFree(waitDstStageMask);
		memFree(singlePointerBuffer);
	}
}
