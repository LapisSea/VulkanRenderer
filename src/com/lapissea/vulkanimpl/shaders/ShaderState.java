package com.lapissea.vulkanimpl.shaders;

import com.lapissea.vec.Vec2i;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.shaders.states.VkBlendMode;
import com.lapissea.vulkanimpl.shaders.states.VkDrawMode;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import org.lwjgl.vulkan.*;

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
	private final int[]       scissors       ={0, 0, 0, 0};
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
	
	public void setDynamicStates(int... dynamicStates){
		this.dynamicStates=dynamicStates;
	}
	
	public void setScissorsEnabled(boolean scissorsEnabled){
		this.scissorsEnabled=scissorsEnabled;
	}
	
	public void setScissors(int top, int left, int width, int height){
		scissors[0]=top;
		scissors[1]=left;
		scissors[2]=width;
		scissors[3]=height;
	}
	
	
	public void write(VkGraphicsPipelineCreateInfo pipelineInfo){
		
		if(DevelopmentInfo.DEV_ON){
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
		
		VkPipelineColorBlendStateCreateInfo colorBlendingState=VkPipelineColorBlendStateCreateInfo.callocStack();
		colorBlendingState.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
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
		
		VkPipelineDynamicStateCreateInfo dynamicStates=null;
		if(this.dynamicStates.length>0){
			dynamicStates=VkPipelineDynamicStateCreateInfo.callocStack();
			dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
			             .pDynamicStates(stackGet().ints(this.dynamicStates));
		}
		
		
		VkPipelineInputAssemblyStateCreateInfo inputAssembly=VkPipelineInputAssemblyStateCreateInfo.callocStack();
		inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
		             .topology(drawMode.handle)
		             .primitiveRestartEnable(false);
		
		VkViewport.Buffer viewports=VkViewport.callocStack(1);
		viewports.get(0).set(0, 0, viewport.x(), viewport.y(), 0, 1);
		
		VkRect2D.Buffer scissors=VkRect2D.callocStack(1);
		
		VkRect2D sc=scissors.get(0);
		if(scissorsEnabled){
			sc.offset().set(this.scissors[0], this.scissors[1]);
			sc.extent().set(this.scissors[2], this.scissors[3]);
		}else{
			sc.offset().set(0, 0);
			sc.extent().set(viewport.x(), viewport.y());
		}
		
		VkPipelineViewportStateCreateInfo viewport=VkPipelineViewportStateCreateInfo.callocStack();
		viewport.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
		        .viewportCount(viewports.limit())
		        .pViewports(viewports)
		        .scissorCount(scissors.limit())
		        .pScissors(scissors);
		
		pipelineInfo.pInputAssemblyState(inputAssembly)
		            .pViewportState(viewport)
		            .pRasterizationState(rasterizer)
		            .pMultisampleState(multisampling)
		            .pDepthStencilState(null)
		            .pColorBlendState(colorBlendingState)
		            .pDynamicState(dynamicStates);
	}
	
}
