package com.lapissea.vulkanimpl.util.types;

import com.lapissea.util.LogUtil;
import com.lapissea.vulkanimpl.SingleUseCommandBuffer;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.format.VkFormatInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkImage implements VkDestroyable, VkGpuCtx{
	
	public static VkImage create(VkGpu gpu, VkImageCreateInfo info){
		LongBuffer handle=memAllocLong(1);
		try{
			int code=vkCreateImage(gpu.getDevice(), info, null, handle);
			if(DevelopmentInfo.DEV_ON) Vk.check(code);
			return new VkImage(handle, gpu, info.initialLayout(), VkExtent3D.malloc().set(info.extent()), VkFormatInfo.get(info.format()));
		}catch(Throwable t){
			memFree(handle);
			throw t;
		}
	}
	
	private final LongBuffer   handle;
	private final VkGpu        gpu;
	private       int          currentLayout;
	private final VkExtent3D   extent;
	private final VkFormatInfo format;
	
	public VkImage(LongBuffer handle, VkGpu gpu, int currentLayout, VkExtent3D extent, VkFormatInfo format){
		this.handle=handle;
		this.gpu=gpu;
		this.currentLayout=currentLayout;
		this.extent=extent;
		this.format=format;
	}
	
	public VkMemoryRequirements getMemRequirements(MemoryStack stack){
		return getMemRequirements(VkMemoryRequirements.mallocStack(stack));
	}
	
	public VkMemoryRequirements getMemRequirements(){
		return getMemRequirements(VkMemoryRequirements.malloc());
	}
	
	public VkMemoryRequirements getMemRequirements(VkMemoryRequirements memRequirements){
		vkGetImageMemoryRequirements(getDevice(), getHandle(), memRequirements);
		return memRequirements;
	}
	
	@Override
	public void destroy(){
		vkDestroyImage(gpu.getDevice(), getHandle(), null);
		memFree(getBuff());
		extent.free();
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public LongBuffer getBuff(){
		return handle;
	}
	
	public long getHandle(){
		return getBuff().get(0);
	}
	
	public VkDeviceMemory createMemory(int requestedProperties){
		VkDeviceMemory mem;
		try(VkMemoryRequirements req=getMemRequirements()){
			mem=getGpu().allocateMemory(req, req.size(), requestedProperties);
		}
		mem.bindImage(this);
		return mem;
	}
	
	public void transitionLayout(VkGpu.Queue queue, int newLayout){
		if(newLayout==currentLayout) return;
		
		try(MemoryStack stack=stackPush()){
			VkImageMemoryBarrier.Buffer barriers=VkConstruct.imageMemoryBarrier(stack, 1);
			
			int srcStageMask, dstStageMask,
				srcAccessMask, dstAccessMask,
				aspectMask;
			
			switch(1<<currentLayout|1<<newLayout){
			
			case 1<<VK_IMAGE_LAYOUT_UNDEFINED|1<<VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:{
				srcAccessMask=0;
				dstAccessMask=VK_ACCESS_TRANSFER_WRITE_BIT;
				
				srcStageMask=VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				dstStageMask=VK_PIPELINE_STAGE_TRANSFER_BIT;
			} break;
			
			case 1<<VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL|1<<VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:{
				srcAccessMask=VK_ACCESS_TRANSFER_WRITE_BIT;
				dstAccessMask=VK_ACCESS_SHADER_READ_BIT;
				
				srcStageMask=VK_PIPELINE_STAGE_TRANSFER_BIT;
				dstStageMask=VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
			} break;
			
			case 1<<VK_IMAGE_LAYOUT_UNDEFINED|1<<VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL:{
				srcAccessMask=0;
				dstAccessMask=VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT|VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
				
				srcStageMask=VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				dstStageMask=VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
			} break;
			
			default:
				throw new IllegalArgumentException("Unknown combination: "+currentLayout+"/"+newLayout);
			}
			
			LogUtil.println(format.name, format.components.stream().map(f->f.type).toArray(VkFormatInfo.ComponentType[]::new));
			
			if(newLayout==VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL){
				aspectMask=VK_IMAGE_ASPECT_DEPTH_BIT;
				
				if(format.hasComponentType(VkFormatInfo.ComponentType.STENCIL)){
					aspectMask|=VK_IMAGE_ASPECT_STENCIL_BIT;
				}
			}else{
				aspectMask=VK_IMAGE_ASPECT_COLOR_BIT;
			}
			
			
			VkImageMemoryBarrier barrier=barriers.get(0);
			barrier.oldLayout(currentLayout)
			       .newLayout(newLayout)
			       .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			       .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			       .image(getHandle())
			       .srcAccessMask(srcAccessMask)
			       .dstAccessMask(dstAccessMask);
			barrier.subresourceRange().set(aspectMask, 0, 1, 0, 1); ;
			
			try(SingleUseCommandBuffer su=new SingleUseCommandBuffer(queue, queue.getPool())){
				su.commandBuffer.pipelineBarrier(srcStageMask, dstStageMask, 0, barriers);
			}
			
			currentLayout=newLayout;
		}
		
	}
	
	public void copyFromBuffer(VkGpu.Queue queue, VkBuffer buffer){
		try(VkOffset3D offset=VkOffset3D.calloc()){
			copyFromBuffer(queue, buffer, offset);
		}
	}
	
	public void copyFromBuffer(VkGpu.Queue queue, VkBuffer buffer, VkOffset3D offset){
		try(VkBufferImageCopy.Buffer region=VkBufferImageCopy.calloc(1)){
			
			region.get(0)
			      .bufferOffset(0)
			      .bufferRowLength(0)
			      .bufferImageHeight(0)
			      .imageOffset(offset)
			      .imageExtent(extent)
			
			      .imageSubresource()
			      .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
			      .mipLevel(0)
			      .baseArrayLayer(0)
			      .layerCount(1);
			
			try(SingleUseCommandBuffer su=new SingleUseCommandBuffer(queue, queue.getPool())){
				su.commandBuffer.copyBufferToImage(buffer, this, region);
			}
		}
	}
	
	public int getLayout(){
		return currentLayout;
	}
	
	public VkFormatInfo getFormat(){
		return format;
	}
}
