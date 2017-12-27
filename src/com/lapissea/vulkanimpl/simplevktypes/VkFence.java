package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkFence extends ExtendableLong implements VkDestroyable, VkGpuCtx{
	
	public static final VkFence NULL;
	
	static{
		NULL=new VkFence(memAllocLong(1).put(0, VK_NULL_HANDLE)){
			@Override
			public void destroy(){
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void waitFor(VkDevice device){
				throw new UnsupportedOperationException();
			}
		};
		
	}
	
	private       LongBuffer buff;
	private final VkGpu      gpu;
	
	public VkFence(VkGpuCtx gpuCtx, LongBuffer buff){
		super(buff.get(0));
		this.buff=buff;
		gpu=gpuCtx.getGpu();
		if(Vk.DEBUG) Objects.requireNonNull(gpu);
	}
	
	private VkFence(LongBuffer buff){
		super(buff.get(0));
		this.buff=buff;
		gpu=null;
	}
	
	
	public void waitFor(VkGpu gpu){
		waitFor(gpu.getDevice());
	}
	
	public void waitFor(VkDevice device){
		Vk.waitForFences(device, buff);
	}
	
	@Override
	public void destroy(){
		vkDestroyFence(getGpuDevice(), val, null);
		val=0;
		if(buff!=null){
			memFree(buff);
			buff=null;
		}
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
