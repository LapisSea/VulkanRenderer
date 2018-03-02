package com.lapissea.vulkanimpl;

import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkCommandPool;
import com.lapissea.vulkanimpl.util.types.VkFence;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class SingleUseCommandBuffer implements AutoCloseable{
	
	public  VkCommandBufferM commandBuffer;
	private VkCommandPool    pool;
	private VkGpu.Queue      queue;
	private boolean          closed;
	
	public SingleUseCommandBuffer(VkGpu.Queue queue, VkCommandPool pool){
		this.queue=queue;
		this.pool=pool;
		
		commandBuffer=pool.allocateCommandBuffer();
		try(VkCommandBufferBeginInfo info=VkConstruct.commandBufferBeginInfo()){
			vkBeginCommandBuffer(commandBuffer, info.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
		}
	}
	
	@Override
	public void close(){
		
		vkEndCommandBuffer(commandBuffer);
		
		VkFence       fence=pool.getGpu().createFence();
		PointerBuffer pp   =memAllocPointer(1);
		try(VkSubmitInfo info=VkConstruct.submitInfo()){
			queue.submit(info.pCommandBuffers(pp.put(0, commandBuffer)), fence);
		}finally{
			memFree(pp);
		}
		queue.waitIdle();
		
		fence.waitFor();
		
		pool.freeCmd(commandBuffer);
		fence.destroy();
		
		commandBuffer=null;
		closed=true;
	}
	
	@Override
	protected void finalize(){
		if(!closed){
			UtilL.exitWithErrorMsg("SingleUseCommandBuffer not closed!");
		}
	}
}