package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.IMemoryAddressable;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.DevelopmentMemorySafety;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkBuffer extends ExtendableLong implements IMemoryAddressable, VkDestroyable, VkGpuCtx{
	
	private long alignment=-1;
	private       long       size;
	private final VkGpu      gpu;
	private       LongBuffer pointer;
	
	public VkBuffer(VkGpuCtx gpuCtx, LongBuffer val, long size){
		super(val.get(0));
		this.size=size;
		pointer=val;
		gpu=gpuCtx.getGpu();
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpu);
	}
	
	@Override
	public void destroy(){
		vkDestroyBuffer(getGpuDevice(), get(), null);
		alignment=-1;
		val=0;
		memFree(pointer);
		pointer=null;
	}
	
	public long getAlignment(){
		return alignment;
	}
	
	@Override
	public VkDeviceMemory alocateMem(VkGpu gpu, int properties){
		
		VkDeviceMemory mem;
		try(MemoryStack stack=stackPush()){
			mem=gpu.alocateMem(size, getMemRequirements(gpu, stack), properties);
		}
		
		bind(gpu.getDevice(), mem);
		return mem;
	}
	
	@Override
	public VkMemoryRequirements getMemRequirements(VkGpu gpu, VkMemoryRequirements dest){
		vkGetBufferMemoryRequirements(gpu.getDevice(), get(), dest);
		return dest;
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory){
		bind(device, memory, 0);
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory, int offset){
		Vk.bindBufferMemory(device, this, memory, offset);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public LongBuffer pointer(){
		return pointer;
	}
	
	private final StackTraceElement[] _ctorStack=Vk.DEVELOPMENT?DevelopmentMemorySafety.make():null;
	
	@Override
	protected void finalize() throws Throwable{
		if(Vk.DEVELOPMENT) DevelopmentMemorySafety.check(this, pointer);
	}
}
