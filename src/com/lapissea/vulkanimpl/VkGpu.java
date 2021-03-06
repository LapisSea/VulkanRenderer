package com.lapissea.vulkanimpl;

import com.lapissea.util.ArrayViewList;
import com.lapissea.vulkanimpl.renderer.model.VkMesh;
import com.lapissea.vulkanimpl.renderer.model.VkMesh.IndexType;
import com.lapissea.vulkanimpl.renderer.model.VkMeshFormat;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.format.VkDescriptor;
import com.lapissea.vulkanimpl.util.format.VkFormat;
import com.lapissea.vulkanimpl.util.types.*;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkGpu implements VkDestroyable, VkGpuCtx{
	
	public class Queue{
		
		protected    VkQueue       queue;
		public final int           id;
		private      VkCommandPool pool;
		
		public Queue(int id){
			this.id=id;
		}
		
		protected void init(){
			if(DEV_ON&&getDevice()==null) throw new IllegalStateException("Gpu not initialized");
			try(MemoryStack stack=stackPush()){
				queue=Vk.createDeviceQueue(getDevice(), id, 0, stack.callocPointer(1));
			}
			pool=createCommandPool();
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
		
		public Queue submit(VkSubmitInfo submitInfo, VkFence fence){
			Vk.queueSubmit(queue, submitInfo, fence.getHandle());
			return this;
		}
		
		public Queue submit(VkSubmitInfo submitInfo){
			Vk.queueSubmit(queue, submitInfo, 0);
			return this;
		}
		
		public Queue waitIdle(){
			Vk.queueWaitIdle(queue);
			return this;
		}
		
		public VkCommandPool getPool(){
			return pool;
		}
		
		public void destroy(){
			pool.destroy();
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
	
	public class FormatProps{
		
		public final VkFormat format;
		
		public final int linearTiling;
		public final int optimalTiling;
		public final int buffer;
		
		public FormatProps(VkFormat format, int linearTiling, int optimalTiling, int buffer){
			this.format=format;
			this.linearTiling=linearTiling;
			this.optimalTiling=optimalTiling;
			this.buffer=buffer;
		}
		
		public boolean checkFeatures(int tiling, int features){
			if(tiling==VK_IMAGE_TILING_LINEAR){
				return (linearTiling&features)==features;
			}
			
			if(DEV_ON&&tiling!=VK_IMAGE_TILING_OPTIMAL) throw new RuntimeException();
			
			return (optimalTiling&features)==features;
		}
	}
	
	private final VulkanCore instance;
	
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
	private Supplier<FormatProps[]>        supportedFormats;
	
	public VkGpu(VulkanCore instance, VkPhysicalDevice physicalDevice){
		this.instance=instance;
		this.physicalDevice=physicalDevice;
		
		memoryProperties=VkPhysicalDeviceMemoryProperties.malloc();
		vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		
		physicalFeatures=VkPhysicalDeviceFeatures.malloc();
		vkGetPhysicalDeviceFeatures(physicalDevice, physicalFeatures);
		physicalProperties=VkPhysicalDeviceProperties.malloc();
		vkGetPhysicalDeviceProperties(physicalDevice, physicalProperties);
		
		
		try(MemoryStack stack=stackPush()){
			String[] data=Vk.getDeviceExtensionProperties(stack, getPhysicalDevice(), stack.mallocInt(1))
			                .stream()
			                .map(VkExtensionProperties::extensionNameString)
			                .toArray(String[]::new);
			
			deviceExtensionProperties=ArrayViewList.create(data, null);
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
	
	public VkGpu init(PointerBuffer layers, PointerBuffer extensions){
		if(logicalDevice!=null) return this;
		
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
			
			deviceFeatures.samplerAnisotropy(true);
			
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
		
		
		CompletableFuture<FormatProps[]> fut=CompletableFuture.supplyAsync(()->{
			try(VkFormatProperties props=VkFormatProperties.calloc()){
				return VkFormat.stream().map(format->{
					vkGetPhysicalDeviceFormatProperties(physicalDevice, format.handle, props);
					
					if(props.linearTilingFeatures()==0&&props.optimalTilingFeatures()==0&&props.bufferFeatures()==0) return null;
					return new FormatProps(format, props.linearTilingFeatures(), props.optimalTilingFeatures(), props.bufferFeatures());
					
				}).filter(Objects::nonNull).sorted(Comparator.comparingInt(a->a.format.handle)).toArray(FormatProps[]::new);
			}
		});
		supportedFormats=()->{
			FormatProps[] i=fut.join();
			supportedFormats=()->i;
			return i;
		};
		
		return this;
	}
	
	@Override
	public void destroy(){
		
		if(graphicsQueue!=null) graphicsQueue.destroy();
		if(surfaceQueue!=null) surfaceQueue.destroy();
		if(transferQueue!=null) transferQueue.destroy();
		
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
	
	
	public int findMemoryType(VkMemoryRequirements memRequ, int requestedProperties){
		return findMemoryType(memRequ.memoryTypeBits(), requestedProperties);
	}
	
	public int findMemoryType(int typeBits, int requestedProperties){
		
		int bits=typeBits;
		for(int i=0;i<VK_MAX_MEMORY_TYPES;i++){
			if((bits&1)==1){
				if((memoryProperties.memoryTypes(i).propertyFlags()&requestedProperties)==requestedProperties){
					return i;
				}
			}
			bits >>= 1;
		}
		return -1;
	}
	
	public VkDeviceMemory allocateMemory(VkMemoryRequirements memRequ, long size, int requestedProperties){
		int memIndex=findMemoryType(memRequ, requestedProperties);
		if(memIndex==-1) throw new IllegalArgumentException("Can not find memory with properties: "+Integer.toString(requestedProperties, 2));
		try(VkMemoryAllocateInfo allocInfo=VkConstruct.memoryAllocateInfo()){
			allocInfo.allocationSize(Math.max(memRequ.size(), size))
			         .memoryTypeIndex(memIndex);
			return Vk.allocateMemory(this, allocInfo);
		}
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
	
	public VulkanCore getInstance(){
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
		try(VkSemaphoreCreateInfo info=VkConstruct.semaphoreCreateInfo()){
			return Vk.createSemaphore(this, info);
		}
	}
	
	public void waitIdle(){
		vkDeviceWaitIdle(getDevice());
	}
	
	public VkBuffer createBuffer(int usage, long size){
		try(VkBufferCreateInfo bufferInfo=VkConstruct.bufferCreateInfo()){
			return createBuffer(bufferInfo.usage(usage).size(size));
		}
	}
	
	public VkBuffer createBuffer(VkBufferCreateInfo bufferInfo){
		return Vk.createBuffer(this, bufferInfo);
	}
	
	public VkMemoryRequirements getMemRequirements(MemoryStack stack, VkBuffer buffer){
		return getMemRequirements(VkMemoryRequirements.mallocStack(stack), buffer);
	}
	
	public VkMemoryRequirements getMemRequirements(VkMemoryRequirements dest, VkBuffer buffer){
		vkGetBufferMemoryRequirements(getDevice(), buffer.getHandle(), dest);
		return dest;
	}
	
	public VkFence createFence(){
		LongBuffer lb=memAllocLong(1);
		try(VkFenceCreateInfo info=VkConstruct.fenceCreateInfo()){
			return Vk.createFence(this, info, lb);
		}finally{
			memFree(lb);
		}
	}
	
	
	public VkDescriptorSetLayout createDescriptorSetLayout(VkDescriptor... descriptors){
		
		try(MemoryStack stack=stackPush()){
			VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding=VkDescriptorSetLayoutBinding.calloc(descriptors.length);
			VkDescriptorSetLayoutCreateInfo     layoutInfo      =VkConstruct.descriptorSetLayoutCreateInfo(stack);
			
			for(int i=0;i<descriptors.length;i++){
				VkDescriptor d=Objects.requireNonNull(descriptors[i]);
				
				uboLayoutBinding.get(i)
				                .binding(i)
				                .descriptorType(d.type)
				                .descriptorCount(1)
				                .stageFlags(d.stageFlags);
			}
			layoutInfo.pBindings(uboLayoutBinding);
			return Vk.createDescriptorSetLayout(this, layoutInfo);
		}
	}
	
	public VkImage create2DImage(VkExtent2D extent, int format, int tiling, int usage, int samples){
		return create2DImage(extent.width(), extent.height(), format, tiling, usage, samples);
	}

//	public VkImage create2DImage(int width, int height, int format, int tiling, int usage){
//		return create2DImage(width, height, format, tiling, usage, VK_SAMPLE_COUNT_1_BIT);
//	}
	
	public VkImage create2DImage(int width, int height, int format, int tiling, int usage, int samples){
		try(VkImageCreateInfo imageInfo=VkConstruct.imageCreateInfo()){
			imageInfo.extent().set(width, height, 1);
			imageInfo.imageType(VK_IMAGE_TYPE_2D)
			         .mipLevels(1)
			         .arrayLayers(1)
			         .format(format)
			         .tiling(tiling)
			         .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			         .usage(usage)
			         .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
			         .samples(samples);
			/*
			 * VK_IMAGE_LAYOUT_UNDEFINED: Not usable by the GPU and the very first transition will discard the texels.
			 * VK_IMAGE_LAYOUT_PREINITIALIZED: Not usable by the GPU, but the first transition will preserve the texels.
			 * */
			
			return VkImage.create(this, imageInfo);
		}
	}
	
	public Stream<FormatProps> supportedFormats(){
		return Stream.of(supportedFormats.get());
	}
	
	public FormatProps getSupportedFormat(int format){
		FormatProps[] formats=supportedFormats.get();
		
		int pos=Arrays.binarySearch(formats, null, (a, b)->Integer.compare(a.format.handle, format));
		if(pos<0) return null;
		
		FormatProps fp=formats[pos];
		return fp.format.handle==format?fp:null;
	}
	
	public VkDescriptorPool createDescriptorPool(int maxSets, VkDescriptor... descriptors){
		try(MemoryStack stack=stackPush()){
			VkDescriptorPoolSize.Buffer poolSize=VkDescriptorPoolSize.callocStack(descriptors.length, stack);
			for(int i=0;i<descriptors.length;i++){
				poolSize.position(i)
				        .type(descriptors[i].type)
				        .descriptorCount(1);
			}
			return createDescriptorPool(maxSets, poolSize);
		}
	}
	
	public VkDescriptorPool createDescriptorPool(int maxSets, VkDescriptorPoolSize.Buffer poolSizes){
		LongBuffer lb=memAllocLong(1);
		try(VkDescriptorPoolCreateInfo poolInfo=VkConstruct.descriptorPoolCreateInfo()){
			poolInfo.pPoolSizes(poolSizes)
			        .maxSets(maxSets);
			return Vk.createDescriptorPool(this, poolInfo, lb);
		}finally{
			memFree(lb);
		}
	}
	
	public VkMesh upload(Consumer<ByteBuffer> writer, VkMeshFormat format, int totalSize, int indexCount){
		if(totalSize==0) return VkMesh.EMPTY_MESH;
		
		IndexType indexType  =IndexType.get(indexCount);
		boolean   indexed    =indexCount>0;
		int       indexStart =totalSize-indexType.bytes*indexCount;
		int       vertexCount=totalSize/format.getSize();

//		if(indexed){
//			if(indexStart%format.getSize()!=0) throw new RuntimeException("Invalid size! "+indexStart+"/"+format.getSize());
//		}else{
//			if(totalSize%format.getSize()!=0) throw new RuntimeException("Invalid size! "+totalSize+"/"+format.getSize());
//		}
		
		
		VkBuffer       stagingBuffer=createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, totalSize);
		VkDeviceMemory stagingMemory=stagingBuffer.createMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		
		try(VkDeviceMemory.MemorySession ses=stagingMemory.memorySession(stagingBuffer.getSize())){
			
			writer.accept(ses.memory);

//			LogUtil.println(ses.memory);
			
			stagingMemory.flushRanges();
			stagingMemory.invalidateRanges();
		}
		
		int usage=VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
		if(indexed) usage|=VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
		
		VkBuffer       modelBuffer=createBuffer(usage, totalSize);
		VkDeviceMemory modelMemory=modelBuffer.createMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		modelBuffer.copyFrom(stagingBuffer, 0, getTransferQueue().getPool());
		
		stagingBuffer.destroy();
		stagingMemory.destroy();
		
		if(indexed) return new VkMesh.Indexed(modelBuffer, modelMemory, format, indexStart, indexCount, indexType);
		return new VkMesh.Raw(modelBuffer, modelMemory, format, vertexCount);
	}
}
