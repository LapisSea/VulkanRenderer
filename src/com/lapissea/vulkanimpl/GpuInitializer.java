package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.exceptions.VkException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class GpuInitializer implements AutoCloseable{
	
	private static final String NO_GPU_FIND="No Vulkan compatible devices can display to a screen or do not meet minimal requirements";
	
	protected final MemoryStack stack=stackPush();
	protected final List<VkGpu> gpus;
	
	public GpuInitializer(VulkanCore instance){
		gpus=Arrays.stream(Vk.getPhysicalDevices(stack, instance.getInstance())).map(d->new VkGpu(instance, d)).collect(Collectors.toList());
		if(gpus.isEmpty()) throw new VkException("No devices support Vulkan");
	}
	
	public VkGpu find(List<String> extensions){
		return find(null, extensions);
	}
	
	public VkGpu find(List<String> layers, List<String> extensions){
		if(gpus.isEmpty()) return null;
		
		VkGpu gpu=gpus.stream()
		              .filter(g->isGpuUsable(g, layers, extensions))
		              .sorted((g1, g2)->-Long.compare(getGpuVRam(g1), getGpuVRam(g2)))
		              .findFirst()
		              .orElse(null);
		
		if(gpu==null)throw new VkException(NO_GPU_FIND);
		gpus.remove(gpu);
		
		return gpu.init(Vk.stringsToPP(layers, stack), Vk.stringsToPP(extensions, stack));
	}
	
	private long getGpuVRam(VkGpu g){
		return g.getMemoryProperties()
		        .memoryHeaps()
		        .stream()
		        .filter(heap->(heap.flags()&VK_MEMORY_HEAP_DEVICE_LOCAL_BIT)==1)
		        .mapToLong(VkMemoryHeap::size)
		        .max()
		        .orElse(0);
	}
	
	private boolean isGpuUsable(VkGpu gpu, List<String> layers, List<String> extensions){
		if(!gpu.getDeviceExtensionProperties().containsAll(extensions)) return false;
		
		if(gpu.getGraphicsQueue()==null||
		   gpu.getSurfaceQueue()==null||
		   gpu.getTransferQueue()==null) return false;
		
		VkPhysicalDeviceFeatures features=gpu.getPhysicalFeatures();
		
		return features.geometryShader()&&
		       features.shaderClipDistance()&&
		       features.shaderTessellationAndGeometryPointSize()&&
		       features.tessellationShader();
	}
	
	@Override
	public void close(){
		gpus.forEach(VkGpu::destroy);
		stack.close();
	}
}
