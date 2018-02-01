package com.lapissea.vulkanimpl.shaders;

import com.lapissea.vec.Vec2i;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.shaders.states.VkBlendMode;
import com.lapissea.vulkanimpl.shaders.states.VkDrawMode;
import org.lwjgl.vulkan.*;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderState{
	
	private static final int[] SAMPLE_LEVELS={
		VK_SAMPLE_COUNT_1_BIT,
		VK_SAMPLE_COUNT_2_BIT,
		VK_SAMPLE_COUNT_4_BIT,
		VK_SAMPLE_COUNT_8_BIT,
		VK_SAMPLE_COUNT_16_BIT,
		VK_SAMPLE_COUNT_32_BIT,
		VK_SAMPLE_COUNT_64_BIT
	};
	
	private       boolean     blending       =false;
	private       boolean     supersampling  =false;
	private       int         sampleLevel    =0;
	private       VkDrawMode  drawMode       =VkDrawMode.TRIANGLES;
	private       VkBlendMode blendMode      =VkBlendMode.NO_BLEND;
	private       int[]       dynamicStates  ={};
	private       boolean     scissorsEnabled=false;
	private       int[]       scissors       ={0, 0, 0, 0};
	private final Vec2i       viewport       =new Vec2i();
	
	public ShaderState setBlending(boolean blending){
		this.blending=blending;
		return this;
	}
	
	public ShaderState setSampleLevel(int sampleLevel){
		this.sampleLevel=sampleLevel;
		return this;
	}
	
	public void setSupersampling(boolean supersampling){
		this.supersampling=supersampling;
	}
	
	public void setDrawMode(VkDrawMode drawMode){
		this.drawMode=drawMode;
	}
	
	public void setViewport(IVec2iR viewport){
		this.viewport.set(viewport);
	}
	
	public void write(){
		
		if(DEVELOPMENT){
			if(sampleLevel<0) throw new IllegalArgumentException("Sample power of 2 can not be negative!");
			if(sampleLevel>=SAMPLE_LEVELS.length) throw new IllegalArgumentException((1<<sampleLevel)+" samples per pixel is to much!");
		}
		
		VkPipelineMultisampleStateCreateInfo multisampling=VkPipelineMultisampleStateCreateInfo.callocStack();
		multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
		             .sampleShadingEnable(supersampling)
		             .rasterizationSamples(SAMPLE_LEVELS[sampleLevel])//samples per pixel
		             .minSampleShading(1)
		             .pSampleMask(null)//custom sampling mask... you need this because?????
		             .alphaToCoverageEnable(false)
		             .alphaToOneEnable(false);
		
		
		VkPipelineColorBlendAttachmentState.Buffer colorBlending=blendMode.write(VkPipelineColorBlendAttachmentState.callocStack(1));
		
		VkPipelineColorBlendStateCreateInfo blendingState=VkPipelineColorBlendStateCreateInfo.callocStack();
		blendingState.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
		             .logicOpEnable(blending)
		             .logicOp(VK_LOGIC_OP_COPY)
		             .pAttachments(colorBlending)
		             .blendConstants(0, 0)
		             .blendConstants(1, 0)
		             .blendConstants(2, 0)
		             .blendConstants(3, 0);
		
		
		VkPipelineRasterizationStateCreateInfo rasterizer=VkPipelineRasterizationStateCreateInfo.callocStack();
		rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
		          .depthClampEnable(false)//clamp pixels outside depth range instead of discarding them
		          .rasterizerDiscardEnable(false) //disable output to frame buffer
		          .polygonMode(drawMode.polygonMode)
		          .lineWidth(1)
		          .cullMode(VK_CULL_MODE_BACK_BIT)
		          .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
		          .depthBiasEnable(false)// modify true depth value of a fragment
		          .depthBiasConstantFactor(0)
		          .depthBiasClamp(0)
		          .depthBiasSlopeFactor(0);
		
		
		VkPipelineDynamicStateCreateInfo dynamicStates=VkPipelineDynamicStateCreateInfo.callocStack();
		dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
		             .pDynamicStates(stackGet().ints(this.dynamicStates));
		
		
		VkPipelineInputAssemblyStateCreateInfo inputAssembly=VkPipelineInputAssemblyStateCreateInfo.callocStack();
		inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
		             .topology(drawMode.handle)
		             .primitiveRestartEnable(false);
		
		
	}
	
}
