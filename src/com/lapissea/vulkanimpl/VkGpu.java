package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.simplevktypes.VkCommandPool;
import com.lapissea.vulkanimpl.simplevktypes.VkFence;
import com.lapissea.vulkanimpl.simplevktypes.VkSemaphore;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.lapissea.vulkanimpl.BufferUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu{
	
	public static final int QUEUE_FAIL=-1, QUEUE_NULL=-2;
	
	public final VkPhysicalDevice physicalDevice;
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
	
	protected Collection<? extends CharSequence> deviceExtensions;
	private   PointerBuffer                      layers;
	
	public VkGpu(GlfwWindow window, VkInstance instance, long pointer, Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		this(window, new VkPhysicalDevice(pointer, instance), deviceExtensions, layers);
	}
	
	public VkGpu(GlfwWindow window, VkPhysicalDevice physicalDevice, Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		this.physicalDevice=physicalDevice;
		this.deviceExtensions=deviceExtensions;
		this.layers=layers;
		setWindow(window);
	}
	
	public void destroy(){
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
	
	public VkGpu setWindow(GlfwWindow window){
		if(this.window==window) return this;
		destroy();
		this.window=window;
		return this;
	}
	
	public VkPhysicalDeviceFeatures getFeatures(){
		if(features==null){
			features=VkPhysicalDeviceFeatures.calloc();
			VK10.vkGetPhysicalDeviceFeatures(physicalDevice, features);
		}
		return features;
	}
	
	public @Nonnull
	VkPhysicalDeviceProperties getProperties(){
		if(properties==null){
			properties=VkPhysicalDeviceProperties.calloc();
			vkGetPhysicalDeviceProperties(physicalDevice, properties);
		}
		
		return properties;
	}
	
	public VkPhysicalDeviceMemoryProperties getMemoryProperties(){
		if(memoryProperties==null){
			memoryProperties=VkPhysicalDeviceMemoryProperties.calloc();
			vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		}
		
		return memoryProperties;
	}
	
	public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(){
		if(queueFamilyProperties==null){
			try(MemoryStack stack=MemoryStack.stackPush()){
				queueFamilyProperties=Vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.callocInt(1));
			}
		}
		return queueFamilyProperties;
	}
	
	public VkSurfaceFormatKHR.Buffer getFormats(){
		if(formats==null){
			try(MemoryStack stack=MemoryStack.stackPush()){
				IntBuffer ib   =stack.callocInt(1);
				int       count=Vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window, ib);
				formats=VkSurfaceFormatKHR.calloc(count);
				
				if(count>0) Vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, window, ib, formats);
			}
		}
		return formats;
	}
	
	public IntBuffer getPresentModes(){
		if(presentModes==null){
			try(MemoryStack stack=MemoryStack.stackPush()){
				
				int count=Vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window, stack.callocInt(1));
				presentModes=MemoryUtil.memAllocInt(count);
				
				if(count>0) Vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, window, buffSingle(stack, count), presentModes);
			}
		}
		return presentModes;
	}
	
	public VkSurfaceCapabilitiesKHR getSurfaceCapabilities(){
		if(surfaceCapabilities==null){
			surfaceCapabilities=VkSurfaceCapabilitiesKHR.calloc();
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
			
			try(MemoryStack stack=MemoryStack.stackPush()){
				IntBuffer ip=stack.callocInt(1);
				for(int i=0;i<props.capacity();i++){
					if(Vk.getPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, window, ip)) return queueSurfaceId=i;
				}
			}
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
				//TODO
				
				VkDeviceCreateInfo deviceCreateInfo=VkDeviceCreateInfo.callocStack(stack);
				deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
				deviceCreateInfo.pQueueCreateInfos(queue);
				deviceCreateInfo.pEnabledFeatures(deviceFeatures);
				deviceCreateInfo.ppEnabledExtensionNames(Vk.stringsToPP(deviceExtensions, stack));
				deviceCreateInfo.ppEnabledLayerNames(layers);
				
				device=Vk.createDevice(physicalDevice, deviceCreateInfo, stack.callocPointer(1));
			}
			
		}
		return device;
	}
	
	
	private VkQueue createQueue(int id, int queueIndex){
		try(MemoryStack stack=MemoryStack.stackPush()){
			
			VkQueue q=Vk.createDeviceQueue(getDevice(), id, queueIndex, stack.callocPointer(1));
			queues.add(q);
			
			return q;
		}
	}
	
	public VkQueue getGraphicsQueue(){
		if(queueGraphics==null) queueGraphics=createQueue(getQueueGraphicsId(), 0);
		return queueGraphics;
	}
	
	public VkQueue getSurfaceQueue(){
		if(queueSurface==null) queueSurface=createQueue(getQueueSurfaceId(), 0);
		return queueSurface;
	}
	
	public VkQueue getTransferQueue(){
		if(queueTransfer==null) queueTransfer=createQueue(getQueueTransferId(), 0);
		return queueTransfer;
	}
	
	private VkCommandPool createCommandPool(int id){
		try(MemoryStack stack=MemoryStack.stackPush()){
			VkCommandPoolCreateInfo poolInfo=VkCommandPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			poolInfo.queueFamilyIndex(id);
			return Vk.createCommandPool(getDevice(), poolInfo, stack.callocLong(1));
		}
	}
	
	public void waitIdle(){
		vkDeviceWaitIdle(device);
	}
	
	public VkCommandPool getGraphicsPool(){
		if(graphicsPool==null) graphicsPool=createCommandPool(getQueueGraphicsId());
		return graphicsPool;
	}
	
	public VkCommandPool getTransferPool(){
		if(transferPool==null) transferPool=createCommandPool(getQueueTransferId());
		return transferPool;
	}
	
	public VkFence createFence(){
		return Vk.createFence(getDevice());
	}
	
	public VkFence createFence(VkFenceCreateInfo fenceInfo){
		return Vk.createFence(getDevice(), fenceInfo);
	}
	
	public VkSemaphore createSemaphore(){
		try(MemoryStack stack=MemoryStack.stackPush()){
			VkSemaphoreCreateInfo semaphoreInfo=VkSemaphoreCreateInfo.callocStack(stack);
			semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			return createSemaphore(semaphoreInfo);
		}
	}
	
	public VkSemaphore createSemaphore(VkSemaphoreCreateInfo semaphoreInfo){
		return Vk.createSemaphore(getDevice(), semaphoreInfo);
	}
}
