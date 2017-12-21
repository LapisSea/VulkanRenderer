package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkFence extends ExtendableLong{
	
	public static final VkFence NULL;
	
	static{
		NULL=new VkFence(memAllocLong(1).put(0, VK_NULL_HANDLE)){
			@Override
			public void destroy(VkDevice device){
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void waitFor(VkDevice device){
				throw new UnsupportedOperationException();
			}
		};
		
	}
	
	private LongBuffer buff;
	
	public VkFence(LongBuffer buff){
		super(buff.get(0));
		this.buff=buff;
	}
	
	public void waitFor(VkGpu gpu){
		waitFor(gpu.getDevice());
	}
	
	public void waitFor(VkDevice device){
		Vk.waitForFences(device, buff);
	}
	
	public void destroy(VkGpu gpu){
		destroy(gpu.getDevice());
	}
	
	public void destroy(VkDevice device){
		vkDestroyFence(device, val, null);
		val=0;
		if(buff!=null){
			memFree(buff);
			buff=null;
		}
	}
}
