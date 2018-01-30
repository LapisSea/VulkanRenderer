package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static com.lapissea.vulkanimpl.VulkanRenderer.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkDestroyable, VkGpuCtx{
	
	
	public class Queue{
		
		private      VkQueue queue;
		public final int     id;
		
		public Queue(int id){
			this.id=id;
		}
		
		private void init(){
			if(DEVELOPMENT&&getDevice()==null) throw new IllegalStateException("Gpu not initialized");
			try(MemoryStack stack=stackPush()){
				queue=Vk.createDeviceQueue(getDevice(), id, 0, stack.callocPointer(1));
			}
		}
	}
	
	private final VulkanRenderer instance;
	
	private final VkPhysicalDevice physicalDevice;
	private       VkDevice         logicalDevice;
	
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	private VkPhysicalDeviceFeatures         features;
	private List<String>                     deviceExtensionProperties;
	
	private VkQueueFamilyProperties.Buffer queueFamilyProperties;
	private VkGpu.Queue                    graphicsQueue;
	private VkGpu.Queue                    surfaceQueue;
	private VkGpu.Queue                    transferQueue;
	
	
	public VkGpu(VulkanRenderer instance, VkPhysicalDevice physicalDevice){
		this.instance=instance;
		this.physicalDevice=physicalDevice;
		
		memoryProperties=VkPhysicalDeviceMemoryProperties.malloc();
		vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		
		features=VkPhysicalDeviceFeatures.malloc();
		vkGetPhysicalDeviceFeatures(physicalDevice, features);
		
		
		try(MemoryStack stack=stackPush()){
			deviceExtensionProperties=Vk.getDeviceExtensionProperties(stack, getPhysicalDevice(), stack.mallocInt(1))
			                            .stream()
			                            .map(VkExtensionProperties::extensionNameString)
			                            .collect(Collectors.toList());
			
		}
		
		initQus();
		
	}
	
	private void initQus(){
		try(MemoryStack stack=stackPush()){
			VkQueueFamilyProperties.Buffer props=getQueueFamilyProperties();
			
			IntBuffer ip=stack.callocInt(1);
			
			for(int i=0;i<props.capacity();i++){
				int flags=props.get(i).queueFlags();
				
				if(graphicsQueue==null&&(flags&VK_QUEUE_GRAPHICS_BIT)!=0){
					graphicsQueue=new Queue(i);
				}
				if(surfaceQueue==null&&Vk.getPhysicalDeviceSurfaceSupportKHR(this, i, ip)){
					surfaceQueue=new Queue(i);
				}
				if(transferQueue==null&&(flags&VK_QUEUE_TRANSFER_BIT)!=0&&(flags&VK_QUEUE_GRAPHICS_BIT)==0){
					transferQueue=new Queue(i);
				}
			}
			
			if(transferQueue==null) transferQueue=graphicsQueue;
		}
	}
	
	public boolean init(PointerBuffer layers, PointerBuffer extensions){
		if(logicalDevice!=null) return false;
		
		try(MemoryStack stack=stackPush()){
			
			TIntList ids=new TIntArrayList(3){
				@Override
				public boolean add(int val){
					return !contains(val)&&super.add(val);
				}
			};
			if(getGraphicsQueue()!=null) ids.add(getGraphicsQueue().id);
			if(getGraphicsQueue()!=null) ids.add(getSurfaceQueue().id);
			if(getGraphicsQueue()!=null) ids.add(getTransferQueue().id);
			
			VkDeviceQueueCreateInfo.Buffer queue=VkDeviceQueueCreateInfo.callocStack(ids.size(), stack);
			
			for(int i=0;i<queue.capacity();i++){
				queue.get(i)
				     .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
				     .pNext(MemoryUtil.NULL)
				     .flags(0)
				     .queueFamilyIndex(ids.get(i))
				     .pQueuePriorities(stack.floats(0));
			}
			
			VkPhysicalDeviceFeatures deviceFeatures=VkPhysicalDeviceFeatures.callocStack(stack);
			deviceFeatures.samplerAnisotropy(false);
			
			VkDeviceCreateInfo info=VkDeviceCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			    .pQueueCreateInfos(queue)
			    .pEnabledFeatures(deviceFeatures)
			    .ppEnabledExtensionNames(extensions)
			    .ppEnabledLayerNames(layers);
			logicalDevice=Vk.createDevice(physicalDevice, info, null, stack.mallocPointer(1));
		}
		
		if(graphicsQueue!=null) graphicsQueue.init();
		if(surfaceQueue!=null) surfaceQueue.init();
		if(transferQueue!=null) transferQueue.init();
		
		return true;
	}
	
	@Override
	public void destroy(){
		
		if(logicalDevice!=null){
			vkDestroyDevice(logicalDevice, null);
			
		}
		memoryProperties.free();
		features.free();
		
		memoryProperties=null;
		features=null;
		logicalDevice=null;
	}
	
	@Override
	public VkGpu getGpu(){
		return this;
	}
	
	
	public int findMemoryType(int typeBits, int properties){
		
		int bits=typeBits;
		for(int i=0;i<VK_MAX_MEMORY_TYPES;i++){
			if((bits&1)==1){
				if((memoryProperties.memoryTypes(i).propertyFlags()&properties)==properties){
					return i;
				}
			}
			bits>>=1;
		}
		return -1;
	}
	
	public VkPhysicalDeviceFeatures getFeatures(){
		return features;
	}
	
	public VkPhysicalDeviceMemoryProperties getMemoryProperties(){
		return memoryProperties;
	}
	
	public VulkanRenderer getInstance(){
		return instance;
	}
	
	public VkPhysicalDevice getPhysicalDevice(){
		return physicalDevice;
	}
	
	public VkGpu.Queue getGraphicsQueue(){
		return graphicsQueue;
	}
	
	public VkGpu.Queue getSurfaceQueue(){
		return surfaceQueue;
	}
	
	public VkGpu.Queue getTransferQueue(){
		return transferQueue;
	}
	
	public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(){
		if(queueFamilyProperties==null){
			try(MemoryStack stack=stackPush()){
				IntBuffer dest =stack.mallocInt(1);
				int       count=Vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, null);
				
				queueFamilyProperties=VkQueueFamilyProperties.calloc(count);
				
				Vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice, dest, queueFamilyProperties);
			}
		}
		return queueFamilyProperties;
	}
	
	public List<String> getDeviceExtensionProperties(){
		return deviceExtensionProperties;
	}
	
	@Override
	public VkDevice getDevice(){
		return logicalDevice;
	}
}