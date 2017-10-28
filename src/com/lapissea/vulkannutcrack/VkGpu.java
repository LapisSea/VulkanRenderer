package com.lapissea.vulkannutcrack;

import com.lapissea.vulkannutcrack.simplevktypes.VkCommandPool;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class VkGpu{
	
	public static final int QUEUE_FAIL=-1, QUEUE_NULL=-2;
	
	public final VkPhysicalDevice physicalDevice;
	private      MemoryStack      stack;
	private      GlfwWindow       window;
	
	private VkPhysicalDeviceFeatures         features;
	private VkPhysicalDeviceProperties       properties;
	private VkPhysicalDeviceMemoryProperties memoryProperties;
	
	private VkQueueFamilyProperties.Buffer queueFamilyProperties;
	
	private int queueTransferId=QUEUE_NULL;
	private int queueGraphicsId=QUEUE_NULL;
	private int queueSurfaceId =QUEUE_NULL;
	
	private VkSurfaceCapabilitiesKHR  surfaceCapabilities;
	private VkSurfaceFormatKHR.Buffer formats;
	private IntBuffer                 presentModes;
	
	private VkDevice device;
	
	private VkQueue queueGraphics;
	private VkQueue queueSurface;
	private VkQueue queueTransfer;
	private final List<VkQueue> queues=new ArrayList<>();
	
	private VkCommandPool graphicsPool;
	private VkCommandPool transferPool;
	
	public VkGpu(GlfwWindow window, VkInstance instance, long pointer){
		this(window, null, new VkPhysicalDevice(pointer, instance));
	}
	
	public VkGpu(GlfwWindow window, MemoryStack stack, VkInstance instance, long pointer){
		this(window, stack, new VkPhysicalDevice(pointer, instance));
	}
	
	public VkGpu(GlfwWindow window, VkPhysicalDevice physicalDevice){
		this(window, null, physicalDevice);
	}
	
	public VkGpu(GlfwWindow window, MemoryStack stack, VkPhysicalDevice physicalDevice){
		this.physicalDevice=physicalDevice;
		setStack(stack);
		setWindow(window);
	}
	
	public void destroy(){
		if(this.stack==null){
			if(features!=null) features.free();
			if(properties!=null) properties.free();
			if(memoryProperties!=null) memoryProperties.free();
			
			if(queueFamilyProperties!=null) queueFamilyProperties.free();
			
			if(surfaceCapabilities!=null) surfaceCapabilities.free();
			if(formats!=null) formats.free();
			MemoryUtil.memFree(presentModes);
			
			if(graphicsPool!=null) graphicsPool.destroy(getDevice());
			if(transferPool!=null) transferPool.destroy(getDevice());
			
			if(device!=null) Vk.destroyDevice(device);
		}
		
		features=null;
		properties=null;
		memoryProperties=null;
		
		queueFamilyProperties=null;
		queues.clear();
		queueGraphicsId=QUEUE_NULL;
		queueSurfaceId=QUEUE_NULL;
		queueTransferId=QUEUE_NULL;
		
		surfaceCapabilities=null;
		surfaceCapabilities=null;
		presentModes=null;
		
		device=null;
	}
	
	public VkGpu setStack(MemoryStack stack){
		if(this.stack==stack) return this;
		destroy();
		this.stack=stack;
		return this;
	}
	
	public VkGpu setWindow(GlfwWindow window){
		if(this.window==window) return this;
		destroy();
		this.window=window;
		return this;
	}
	
	public VkPhysicalDeviceFeatures getFeatures(){
		if(features==null){
			features=stack==null?VkPhysicalDeviceFeatures.calloc():VkPhysicalDeviceFeatures.callocStack(stack);
			VK10.vkGetPhysicalDeviceFeatures(physicalDevice, features);
		}
		return features;
	}
	
	public VkPhysicalDeviceProperties getProperties(){
		if(properties==null){
			properties=stack==null?VkPhysicalDeviceProperties.calloc():VkPhysicalDeviceProperties.callocStack(stack);
			vkGetPhysicalDeviceProperties(physicalDevice, properties);
		}
		
		return properties;
	}
	
	public VkPhysicalDeviceMemoryProperties getMemoryProperties(){
		if(memoryProperties==null){
			memoryProperties=stack==null?VkPhysicalDeviceMemoryProperties.calloc():VkPhysicalDeviceMemoryProperties.callocStack(stack);
			vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		}
		
		return memoryProperties;
	}
	
	public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(){
		if(queueFamilyProperties==null){
			IntBuffer i=Buff.i.get(1);
			queueFamilyProperties=Vk.getPhysicalDeviceQueueFamilyProperties(stack, physicalDevice, i);
			Buff.i.done(i);
		}
		return queueFamilyProperties;
	}
	
	public VkSurfaceFormatKHR.Buffer getFormats(){
		if(formats==null){
			IntBuffer i=Buff.i.get(1);
			
			int count=Vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window, i);
			formats=stack==null?VkSurfaceFormatKHR.mallocStack(count, stack):VkSurfaceFormatKHR.malloc(count);
			
			if(count>0) Vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window, i, formats);
			Buff.i.done(i);
		}
		return formats;
	}
	
	public IntBuffer getPresentModes(){
		if(presentModes==null){
			IntBuffer i=Buff.i.get(1);
			
			int count=Vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window, i);
			presentModes=MemoryUtil.memAllocInt(count);
			
			if(count>0) Vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window, i, presentModes);
			Buff.i.done(i);
		}
		return presentModes;
	}
	
	public VkSurfaceCapabilitiesKHR getSurfaceCapabilities(){
		if(surfaceCapabilities==null){
			surfaceCapabilities=stack==null?VkSurfaceCapabilitiesKHR.malloc():VkSurfaceCapabilitiesKHR.mallocStack(stack);
			Vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, window, surfaceCapabilities);
		}
		return surfaceCapabilities;
	}
	
	
	public int getQueueGraphicsId(){
		if(queueGraphicsId==QUEUE_NULL){
			VkQueueFamilyProperties.Buffer props=getQueueFamilyProperties();
			for(int i=0;i<props.capacity();i++){
				if((props.get(i).queueFlags()&VK_QUEUE_GRAPHICS_BIT)!=0){
					return queueGraphicsId=i;
				}
			}
			queueGraphicsId=QUEUE_FAIL;
		}
		return queueGraphicsId;
	}
	
	public int getQueueSurfaceId(){
		if(queueSurfaceId==QUEUE_NULL){
			VkQueueFamilyProperties.Buffer props=getQueueFamilyProperties();
			
			IntBuffer ip=Buff.i.get(1);
			for(int i=0;i<props.capacity();i++){
				if(Vk.getPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, window, ip)){
					Buff.i.done(ip);
					return queueSurfaceId=i;
				}
			}
			Buff.i.done(ip);
			queueSurfaceId=QUEUE_FAIL;
		}
		return queueSurfaceId;
	}
	
	public int getQueueTransferId(){
		if(queueTransferId==QUEUE_NULL){
			VkQueueFamilyProperties.Buffer props=getQueueFamilyProperties();
			for(int i=0;i<props.capacity();i++){
				int flags=props.get(i).queueFlags();
				if((flags&VK_QUEUE_TRANSFER_BIT)!=0&&
				   (flags&VK_QUEUE_GRAPHICS_BIT)==0){
					return queueTransferId=i;
				}
			}
			queueTransferId=getQueueGraphicsId();
		}
		return queueTransferId;
	}
	
	public GlfwWindow getWindow(){
		return window;
	}
	
	public VkDevice getDevice(){
		return device;
	}
	
	public VkDevice createDevice(Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		if(device==null){
			try(MemoryStack stack=MemoryStack.stackPush()){
				
				TIntList ids=new TIntArrayList(3){
					@Override
					public boolean add(int val){
						return !contains(val)&&super.add(val);
					}
				};
				ids.add(getQueueGraphicsId());
				ids.add(getQueueSurfaceId());
				ids.add(getQueueTransferId());
				
				VkDeviceQueueCreateInfo.Buffer queue=VkDeviceQueueCreateInfo.mallocStack(ids.size(), stack);
				
				for(int i=0;i<queue.capacity();i++){
					queue.get(i)
					     .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
					     .pNext(MemoryUtil.NULL)
					     .flags(0)
					     .queueFamilyIndex(ids.get(i))
					     .pQueuePriorities(stack.floats(0));
				}
				
				VkPhysicalDeviceFeatures deviceFeatures=VkPhysicalDeviceFeatures.callocStack(stack);
				//TODO
				
				VkDeviceCreateInfo deviceCreateInfo=VkDeviceCreateInfo.mallocStack(stack);
				deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
				deviceCreateInfo.pQueueCreateInfos(queue);
				deviceCreateInfo.pEnabledFeatures(deviceFeatures);
				deviceCreateInfo.ppEnabledExtensionNames(Vk.stringsToPP(deviceExtensions, stack));
				deviceCreateInfo.ppEnabledLayerNames(layers);
				
				device=Vk.createDevice(physicalDevice, deviceCreateInfo, stack.mallocPointer(1));
			}
			
		}
		return device;
	}
	
	private VkQueue createQueue(Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers, int id, int queueIndex){
		PointerBuffer pp=Buff.p.create(1);
		
		VkQueue q=Vk.createDeviceQueue(createDevice(deviceExtensions, layers), id, queueIndex, pp);
		queues.add(q);
		
		Buff.p.done(pp);
		return q;
	}
	
	public VkQueue getGraphicsQueue(){
		return queueGraphics;
	}
	
	
	public VkQueue getSurfaceQueue(){
		return queueSurface;
	}
	
	private void createSurfaceQueue(Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
	}
	
	
	public VkQueue getTransferQueue(){
		return queueTransfer;
	}
	
	private void createTransferQueue(Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
	}
	
	
	/**
	 * calls create for all queues (makes sure basic gpu attributes are created)
	 *
	 * @param deviceExtensions
	 * @param layers
	 * @return
	 */
	public VkGpu init(Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		if(queueGraphics==null) queueGraphics=createQueue(deviceExtensions, layers, getQueueGraphicsId(), 0);
		if(queueSurface==null) queueSurface=createQueue(deviceExtensions, layers, getQueueSurfaceId(), 0);
		if(queueTransfer==null) queueTransfer=createQueue(deviceExtensions, layers, getQueueTransferId(), 0);
		
		graphicsPool=createCommandPool(getQueueGraphicsId());
		transferPool=createCommandPool(getQueueTransferId());
		return this;
	}
	
	private VkCommandPool createCommandPool(int id){
		try(MemoryStack stack=MemoryStack.stackPush()){
			VkCommandPoolCreateInfo poolInfo=VkCommandPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			poolInfo.queueFamilyIndex(id);
			return Vk.createCommandPool(getDevice(), poolInfo, stack.mallocLong(1));
		}
	}
	
	public void waitIdle(){
		vkDeviceWaitIdle(device);
	}
	
	public VkCommandPool getGraphicsPool(){
		return graphicsPool;
	}
	
	public VkCommandPool getTransferPool(){
		return transferPool;
	}
}
