package com.lapissea.vulkanimpl.simplevktypes;

import com.lapissea.vulkanimpl.IMemoryAddressable;
import com.lapissea.vulkanimpl.SingleUseCommands;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.VkImageFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.util.Objects;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkImage extends ExtendableLong implements IMemoryAddressable, VkDestroyable, VkGpuCtx{
	
	public static final int ON_GPU_TEXTURE=VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT;
	
	public final  int           byteSize;
	public final  int           width;
	public final  int           height;
	public final  VkImageFormat format;
	private final VkGpu         gpu;
	
	public VkImage(VkGpuCtx gpuCtx, long val, int width, int height, VkImageFormat format){
		super(val);
		this.width=width;
		this.height=height;
		this.format=format;
		byteSize=width*height*format.bytes();
		gpu=gpuCtx.getGpu();
		if(Vk.DEVELOPMENT) Objects.requireNonNull(gpu);
	}
	
	@Override
	public void destroy(){
		vkDestroyImage(getGpuDevice(), get(), null);
		val=0;
	}
	
	@Override
	public VkDeviceMemory alocateMem(VkGpu gpu, int properties){
		
		VkDeviceMemory mem;
		try(MemoryStack stack=stackPush()){
			mem=gpu.alocateMem(byteSize, getMemRequirements(gpu, stack), properties);
		}
		
		bind(gpu.getDevice(), mem);
		return mem;
	}
	
	@Override
	public VkMemoryRequirements getMemRequirements(VkGpu gpu, VkMemoryRequirements dest){
		vkGetImageMemoryRequirements(gpu.getDevice(), get(), dest);
		return dest;
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory){
		bind(device, memory, 0);
	}
	
	public void bind(VkDevice device, VkDeviceMemory memory, int offset){
		Vk.bindImageMemory(device, this, memory, offset);
	}
	
	public void transitionImageLayout(VkGpu gpu, int oldLayout, int newLayout){
		
		try(MemoryStack stack=stackPush();SingleUseCommands commands=new SingleUseCommands(stack, gpu)){
			
			VkImageMemoryBarrier.Buffer barrier=VkImageMemoryBarrier.callocStack(1, stack);
			barrier.get(0)
			       .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
			       .oldLayout(oldLayout)
			       .newLayout(newLayout)
			       .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			       .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			       .image(get());
			
			VkImageSubresourceRange srr=barrier.get(0).subresourceRange();
			srr.baseMipLevel(0)
			   .levelCount(1)
			   .baseArrayLayer(0)
			   .layerCount(1);
			
			if(newLayout==VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL){
				if(format.hasAspect(VkImageAspect.STENCIL)) srr.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT|VK_IMAGE_ASPECT_STENCIL_BIT);
				else srr.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
			}else{
				barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			}
			
			
			int sourceStage;
			int destinationStage;
			if(oldLayout==VK_IMAGE_LAYOUT_UNDEFINED&&newLayout==VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL){
				barrier.srcAccessMask(0)
				       .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
				
				sourceStage=VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage=VK_PIPELINE_STAGE_TRANSFER_BIT;
				
				
			}else if(oldLayout==VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL&&newLayout==VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL){
				barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
				       .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
				
				sourceStage=VK_PIPELINE_STAGE_TRANSFER_BIT;
				destinationStage=VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
				
				
			}else if(oldLayout==VK_IMAGE_LAYOUT_UNDEFINED&&newLayout==VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL){
				barrier.srcAccessMask(0);
				barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT|VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
				
				sourceStage=VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage=VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
				
			}else{
				throw new IllegalArgumentException("Unsupported layout transition!");
			}
			
			vkCmdPipelineBarrier(commands.commandBuffer, sourceStage, destinationStage, 0, null, null, barrier);
		}
	}
	
	private boolean hasStencilComponent(int format){
		return format==VK_FORMAT_D32_SFLOAT_S8_UINT||format==VK_FORMAT_D24_UNORM_S8_UINT;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
