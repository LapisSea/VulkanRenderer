package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.types.VkCommandPool;
import com.lapissea.vulkanimpl.util.types.VkSemaphore;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkDestroyable, VkGpuCtx{
	
	
	public class Queue{
		
		protected    VkQueue queue;
		public final int     id;
		
		public Queue(int id){
			this.id=id;
		}
		
		protected void init(){
			if(DevelopmentInfo.DEV_ON&&getDevice()==null) throw new IllegalStateException("Gpu not initialized");
			try(MemoryStack stack=stackPush()){
				queue=Vk.createDeviceQueue(getDevice(), id, 0, stack.callocPointer(1));
			}
		}
		
		public VkCommandPool createCommandPool(){
			try(MemoryStack stack=stackPush()){
				/* VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT: Allow command buffers to be rerecorded individually, without this flag they all have to be reset together
				 * VK_COMMAND_POOL_CREATE_TRANSIENT_BIT: Hint that command buffers are rerecorded with new commands very often (may change memory allocation behavior)
				 */
				VkCommandPoolCreateInfo poolInfo=VkCommandPoolCreateInfo.mallocStack(stack);
				poolInfo.pNext(0)
				        .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
				        .queueFamilyIndex(id)
				        .flags(0);
				return Vk.createCommandPool(getGpu(), poolInfo, stack.mallocLong(1));
			}
		}
		
		public Queue submit(VkSubmitInfo submitInfo){
			Vk.queueSubmit(queue, submitInfo, 0);
			return this;
		}
		
		public Queue waitIdle(){
			Vk.queueWaitIdle(queue);
			return this;
		}
		
	}
	
	public class SurfaceQu extends Queue{
		
		public SurfaceQu(int id){
			super(id);
		}
		
		public boolean present(VkPresentInfoKHR presentInfo){
			return Vk.queuePresentKHR(queue, presentInfo);
		}
	}
	
	private final VulkanRenderer instance;
	
	private final VkPhysicalDevice physicalDevice;
	private       VkDevice         logicalDevice;
	
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	private VkPhysicalDeviceFeatures         physicalFeatures;
	private VkPhysicalDeviceProperties       physicalProperties;
	private List<String>                     deviceExtensionProperties;
	
	private VkQueueFamilyProperties.Buffer queueFamilyProperties;
	private Queue                          graphicsQueue;
	private SurfaceQu                      surfaceQueue;
	private Queue                          transferQueue;
	
	
	public VkGpu(VulkanRenderer instance, VkPhysicalDevice physicalDevice){
		this.instance=instance;
		this.physicalDevice=physicalDevice;
		
		memoryProperties=VkPhysicalDeviceMemoryProperties.malloc();
		vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		
		physicalFeatures=VkPhysicalDeviceFeatures.malloc();
		vkGetPhysicalDeviceFeatures(physicalDevice, physicalFeatures);
		physicalProperties=VkPhysicalDeviceProperties.malloc();
		vkGetPhysicalDeviceProperties(physicalDevice, physicalProperties);
		
		
		try(MemoryStack stack=stackPush()){
			deviceExtensionProperties=Vk.getDeviceExtensionProperties(stack, getPhysicalDevice(), stack.mallocInt(1))
			                            .stream()
			                            .map(VkExtensionProperties::extensionNameString)
			                            .collect(Collectors.toList());
			
		}
		
		findQueueFamilies();
		
	}
	
	private void findQueueFamilies(){
		try(MemoryStack stack=stackPush()){
			VkQueueFamilyProperties.Buffer props=getQueueFamilyProperties();
			
			IntBuffer ip=stack.callocInt(1);
			
			for(int i=0;i<props.capacity();i++){
				int flags=props.get(i).queueFlags();
				
				if(graphicsQueue==null&&(flags&VK_QUEUE_GRAPHICS_BIT)!=0){
					graphicsQueue=new Queue(i);
				}
				if(surfaceQueue==null&&Vk.getPhysicalDeviceSurfaceSupportKHR(this, i, ip)){
					surfaceQueue=new SurfaceQu(i);
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
		physicalFeatures.free();
		
		memoryProperties=null;
		physicalFeatures=null;
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
	
	public VkPhysicalDeviceFeatures getPhysicalFeatures(){
		return physicalFeatures;
	}
	
	public VkPhysicalDeviceProperties getPhysicalProperties(){
		return physicalProperties;
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
	
	public SurfaceQu getSurfaceQueue(){
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
	
	public VkSemaphore createSemaphore(){
		try(VkSemaphoreCreateInfo info=VkSemaphoreCreateInfo.calloc()){
			info.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			return Vk.createSemaphore(this, info);
		}
	}
	
	public void waitIdle(){
		vkDeviceWaitIdle(getDevice());
	}
	
}
