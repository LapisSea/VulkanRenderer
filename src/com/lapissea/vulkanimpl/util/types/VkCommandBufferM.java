package com.lapissea.vulkanimpl.util.types;

import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkCommandBufferM extends VkCommandBuffer implements VkGpuCtx{
	
	private final VkCommandPool pool;
	public final  boolean       isPrimary;
	
	public VkCommandBufferM(long handle, VkCommandPool pool, boolean isPrimary){
		super(handle, pool.getGpu().getDevice());
		this.pool=pool;
		this.isPrimary=isPrimary;
	}
	
	public VkCommandBufferM[] createSecondary(int count){
		return pool.allocateCommandBuffer(count, this);
	}
	
	@Override
	public VkGpu getGpu(){
		return pool.getGpu();
	}
	
	public void begin(){
		try(MemoryStack stack=stackPush()){
			
			VkCommandBufferBeginInfo beginInfo=VkCommandBufferBeginInfo.callocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
			         .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
			
			if(!isPrimary){
				VkCommandBufferInheritanceInfo inheritanceInfo=VkCommandBufferInheritanceInfo.callocStack(stack);
				inheritanceInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO);
				beginInfo.pInheritanceInfo(inheritanceInfo);
			}
			
			vkBeginCommandBuffer(this, beginInfo);
		}
	}
}
