package com.lapissea.vulkanimpl.shaders.states;

import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public enum VkBlendMode{
	NO_BLEND(d->d.colorWriteMask(VK_COLOR_COMPONENT_R_BIT|VK_COLOR_COMPONENT_G_BIT|VK_COLOR_COMPONENT_B_BIT|VK_COLOR_COMPONENT_A_BIT)
	             .blendEnable(false)
	             .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
	             .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
	             .colorBlendOp(VK_BLEND_OP_ADD)
	             .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
	             .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
	             .alphaBlendOp(VK_BLEND_OP_ADD)),
	
	ONE_MINUS_DST(d->d.set(NO_BLEND.data)
	                  .blendEnable(true)
	                  .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
	                  .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA));
	
	
	private final VkPipelineColorBlendAttachmentState data;
	
	VkBlendMode(Consumer<VkPipelineColorBlendAttachmentState> data){
		data.accept(this.data=VkPipelineColorBlendAttachmentState.calloc());
	}
	
	public VkPipelineColorBlendAttachmentState.Buffer write(VkPipelineColorBlendAttachmentState.Buffer buffer){
		return write(buffer.position(), buffer);
	}
	
	public VkPipelineColorBlendAttachmentState.Buffer write(int pos, VkPipelineColorBlendAttachmentState.Buffer buffer){
		return buffer.put(pos, data);
	}
}