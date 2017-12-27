package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.simplevktypes.VkDeviceMemory;
import com.lapissea.vulkanimpl.simplevktypes.VkImage;
import com.lapissea.vulkanimpl.simplevktypes.VkImageView;
import com.lapissea.vulkanimpl.simplevktypes.VkSampler;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkImageTexture{
	public final VkImage        image;
	public final VkDeviceMemory memory;
	private      VkImageView    view;
	private      VkSampler      sampler;
	
	public VkImageTexture(VkImage image, VkDeviceMemory memory, int byteSize){
		this.image=image;
		this.memory=memory;
	}
	
	public void init(VkGpu gpu, VkImageAspect aspect){
		view=gpu.createView(image, aspect);
		
		try(MemoryStack stack=stackPush()){
			VkSamplerCreateInfo samplerInfo=VkSamplerCreateInfo.callocStack(stack);
			
			samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
			           .magFilter(VK_FILTER_LINEAR)
			           .minFilter(VK_FILTER_LINEAR)
			           .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
			           .anisotropyEnable(true)
			           .maxAnisotropy(16)
			           .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
			           .unnormalizedCoordinates(false)
			           .compareEnable(false)
			           .compareOp(VK_COMPARE_OP_ALWAYS)
			           .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
			           .mipLodBias(0)
			           .minLod(0)
			           .maxLod(0);
			
			sampler=Vk.createSampler(gpu, samplerInfo, stack.mallocLong(1));
		}
		
	}
	
	public VkImageView getView(){
		return view;
	}
	
	public void destroy(){
		if(view!=null){
			sampler.destroy();
			view.destroy();
		}
		image.destroy();
		memory.destroy();
		view=null;
	}
	
	public VkSampler getSampler(){
		return sampler;
	}
}
