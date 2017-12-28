package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.simplevktypes.*;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.lapissea.vulkanimpl.BufferUtil.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkGpuCtx, VkDestroyable{
	
	
	public enum Feature{
		LINEAR(VK_IMAGE_TILING_LINEAR, f->f.linearTilingFeatures),
		OPTIMAL(VK_IMAGE_TILING_OPTIMAL, f->f.optimalTilingFeatures);
		
		private final Function<PhysicalDeviceFormat, Integer> get;
		
		public final int tiling;
		
		Feature(int tiling, Function<PhysicalDeviceFormat, Integer> get){
			this.tiling=tiling;
			this.get=get;
		}
		
		public int get(PhysicalDeviceFormat f){
			return get.apply(f);
		}
	}
	
	private static class PhysicalDeviceFormat implements Comparable<PhysicalDeviceFormat>{
		
		public final int     format;
		public final Integer linearTilingFeatures, optimalTilingFeatures, bufferFeatures;
		
		
		public PhysicalDeviceFormat(int format){
			this(format, 0, 0, 0);
		}
		
		public PhysicalDeviceFormat(int format, int linearTilingFeatures, int optimalTilingFeatures, int bufferFeatures){
			this.format=format;
			this.linearTilingFeatures=linearTilingFeatures;
			this.optimalTilingFeatures=optimalTilingFeatures;
			this.bufferFeatures=bufferFeatures;
		}
		
		@Override
		public int compareTo(PhysicalDeviceFormat o){
			return Integer.compare(format, o.format);
		}
	}
	
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
	
	private final List<PhysicalDeviceFormat> phyDevFormats=new ArrayList<>();
	
	public VkGpu(GlfwWindow window, VkInstance instance, long pointer, Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		this(window, new VkPhysicalDevice(pointer, instance), deviceExtensions, layers);
	}
	
	public VkGpu(GlfwWindow window, VkPhysicalDevice physicalDevice, Collection<? extends CharSequence> deviceExtensions, PointerBuffer layers){
		this.physicalDevice=physicalDevice;
		this.deviceExtensions=deviceExtensions;
		this.layers=layers;
		setWindow(window);
	}
	
	@Override
	public void destroy(){
		if(features!=null) features.free();
		if(properties!=null) properties.free();
		if(memoryProperties!=null) memoryProperties.free();
		
		if(queueFamilyProperties!=null) queueFamilyProperties.free();
		
		if(surfaceCapabilities!=null) surfaceCapabilities.free();
		if(formats!=null) formats.free();
		memFree(presentModes);
		
		if(graphicsPool!=null) graphicsPool.destroy();
		if(transferPool!=null) transferPool.destroy();
		
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
	
	public VkPhysicalDeviceProperties getProperties(){
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
			try(MemoryStack stack=stackPush()){
				queueFamilyProperties=Vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice, stack.callocInt(1));
			}
		}
		return queueFamilyProperties;
	}
	
	public VkSurfaceFormatKHR.Buffer getFormats(){
		if(formats==null){
			try(MemoryStack stack=stackPush()){
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
			try(MemoryStack stack=stackPush()){
				
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
			
			try(MemoryStack stack=stackPush()){
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
			try(MemoryStack stack=stackPush()){
				
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
				deviceFeatures.samplerAnisotropy(false);
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
		try(MemoryStack stack=stackPush()){
			
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
		try(MemoryStack stack=stackPush()){
			VkCommandPoolCreateInfo poolInfo=VkCommandPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			poolInfo.queueFamilyIndex(id);
			return Vk.createCommandPool(this, poolInfo, stack.callocLong(1));
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
		try(MemoryStack stack=stackPush()){
			VkFenceCreateInfo fenceInfo=VkFenceCreateInfo.callocStack(stack);
			fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			return createFence(fenceInfo);
		}
	}
	
	public VkFence createFence(VkFenceCreateInfo fenceInfo){
		return Vk.createFence(this, fenceInfo);
	}
	
	public VkSemaphore createSemaphore(){
		try(MemoryStack stack=stackPush()){
			VkSemaphoreCreateInfo semaphoreInfo=VkSemaphoreCreateInfo.callocStack(stack);
			semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
			return createSemaphore(semaphoreInfo);
		}
	}
	
	public VkSemaphore createSemaphore(VkSemaphoreCreateInfo semaphoreInfo){
		return Vk.createSemaphore(this, semaphoreInfo);
	}
	
	public VkBuffer createBuffer(int size, int usage){
		try(MemoryStack stack=stackPush()){
			return Vk.createBuffer(this, Vk.bufferInfo(stack, size, usage), stack.mallocLong(1));
		}
	}
	
	public VkDeviceMemory alocateMem(long size, IMemoryAddressable context, int properties){
		try(MemoryStack stack=stackPush()){
			return alocateMem(size, context.getMemRequirements(this, stack), properties);
		}
	}
	
	public VkDeviceMemory alocateMem(long size, VkMemoryRequirements memRequ, int properties){
		try(MemoryStack stack=stackPush()){
			
			VkMemoryAllocateInfo memAlloc=VkMemoryAllocateInfo.callocStack(stack);
			memAlloc.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
			        .allocationSize(Math.max(memRequ.size(), size))
			        .memoryTypeIndex(findMemoryType(memRequ.memoryTypeBits(), properties));
			
			return Vk.allocateMemory(this, memAlloc, stack.mallocLong(1));
		}
	}
	
	private int findMemoryType(int typeBits, int properties){
		VkPhysicalDeviceMemoryProperties deviceMemoryProperties=getMemoryProperties();
		
		int bits=typeBits;
		for(int i=0;i<VK_MAX_MEMORY_TYPES;i++){
			if((bits&1)==1){
				if((deviceMemoryProperties.memoryTypes(i).propertyFlags()&properties)==properties){
					return i;
				}
			}
			bits>>=1;
		}
		throw new IllegalStateException("unable to find suitable memory type");
	}
	
	public VkDeviceMemory alocateMem(VkMemoryAllocateInfo memAlloc){
		try(MemoryStack stack=stackPush()){
			return Vk.allocateMemory(this, memAlloc, stack.mallocLong(1));
		}
	}
	
	
	public VkBufferMemory createBufferMem(int size, int usage, int properties){
		
		VkBuffer buf=createBuffer(size, usage);
		
		return new VkBufferMemory(buf, buf.alocateMem(this, properties), size);
	}
	
	public VkDescriptorPool createDescriptorPool(int maxSets){
		try(MemoryStack stack=stackPush()){
			
			VkDescriptorPoolSize.Buffer poolSize=VkDescriptorPoolSize.callocStack(2, stack);
			poolSize.get(0)
			        .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			        .descriptorCount(1);
			poolSize.get(1)
			        .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
			        .descriptorCount(1);
			
			VkDescriptorPoolCreateInfo poolInfo=VkDescriptorPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
			        .pPoolSizes(poolSize)
			        .maxSets(maxSets);
			
			return Vk.createDescriptorPool(this, poolInfo, stack);
		}
	}
	
	public VkImage createImage(VkImageCreateInfo imageInfo){
		try(MemoryStack stack=stackPush()){
			return Vk.createImage(this, imageInfo, stack.mallocLong(1));
		}
	}
	
	public VkImageTexture createImageTexture(VkImageCreateInfo imageInfo, int properties){
		VkImage image=createImage(imageInfo);
		return new VkImageTexture(image, image.alocateMem(this, properties), image.byteSize);
	}
	
	
	public boolean checkImageFormatSampling(Feature feature, int format){
		return checkFormat(feature, VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT, format);
	}
	
	public int anyFormat(Feature feature, int requiredFeatures, int... formats){
		for(int format : formats){
			if(checkFormat(feature, requiredFeatures, format)) return format;
		}
		return -1;
	}
	
	public boolean checkFormat(Feature feature, int requiredFeatures, int format){
		PhysicalDeviceFormat d=null;
		try{
			d=phyDevFormats.get(Collections.binarySearch(phyDevFormats, new PhysicalDeviceFormat(format)));
			if(d.format!=format) d=null;
		}catch(Exception ignored){}
		
		if(d!=null) return (feature.get(d)&requiredFeatures)==requiredFeatures;
		
		
		try(MemoryStack stack=stackPush()){
			VkFormatProperties props=VkFormatProperties.mallocStack(stack);
			vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);
			phyDevFormats.add(new PhysicalDeviceFormat(format, props.linearTilingFeatures(), props.optimalTilingFeatures(), props.bufferFeatures()));
			phyDevFormats.sort(null);
		}
		return checkFormat(feature, requiredFeatures, format);
	}
	
	
	public VkImageView createView(VkImage image, VkImageAspect aspect){
		try(MemoryStack stack=stackPush()){
			VkImageViewCreateInfo createInfo=VkImageViewCreateInfo.callocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
			          .image(image.get())
			          .viewType(VK_IMAGE_VIEW_TYPE_2D)
			          .format(image.format)
			          .subresourceRange().set(aspect.val, 0, 1, 0, 1);
			
			VkComponentMapping comps=createInfo.components();
			comps.r(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .g(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .b(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .a(VK_COMPONENT_SWIZZLE_IDENTITY);
			
			return Vk.createImageView(this, createInfo, stack.callocLong(1));
		}
	}
	
	@Override
	public VkGpu getGpu(){
		return this;
	}
}
