package com.lapissea.vulkannutcrack;

import com.lapissea.util.UtilL;
import com.lapissea.vulkannutcrack.model.VkModel;
import com.lapissea.vulkannutcrack.simplevktypes.VkGraphicsPipeline;
import com.lapissea.vulkannutcrack.simplevktypes.VkPipelineLayout;
import com.lapissea.vulkannutcrack.simplevktypes.VkRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.lwjgl.vulkan.VK10.*;

public class Shader{
	public enum Type{
		FRAGMENT("frag", VK_SHADER_STAGE_FRAGMENT_BIT),
		GEOMETRY("geom", VK_SHADER_STAGE_GEOMETRY_BIT),
		VERTEX("vert", VK_SHADER_STAGE_VERTEX_BIT),
		COMPUTE("comp", VK_SHADER_STAGE_COMPUTE_BIT);
		public final String extension;
		public final int    stageBit;
		
		Type(String extension, int stageBit){
			this.extension=extension;
			this.stageBit=stageBit;
		}
	}
	
	public final String name;
	
	private VkShaderModule fs, gs, vs;
	private VkPipelineLayout   pipelineLayout;
	private VkGpu              gpu;
	private VkGraphicsPipeline graphicsPipeline;
	
	public Shader(String name){this.name=name;}
	
	public void create(VkGpu gpu, VkExtent2D extent, VkRenderPass renderPass, VkViewport.Buffer viewport, VkModel model){
		
		this.gpu=gpu;
		
		try{
			vs=Vk.createShaderModule(gpu.getDevice(), read(name, Type.VERTEX), Type.VERTEX);
		}catch(Exception e){
			throw UtilL.uncheckedThrow(e);
		}
		
		try{
			gs=Vk.createShaderModule(gpu.getDevice(), read(name, Type.GEOMETRY), Type.GEOMETRY);
		}catch(Exception e){}
		
		try{
			fs=Vk.createShaderModule(gpu.getDevice(), read(name, Type.FRAGMENT), Type.FRAGMENT);
		}catch(Exception e){
			throw UtilL.uncheckedThrow(e);
		}
		
		try(MemoryStack stack=MemoryStack.stackPush()){
			
			/*
			VK_PRIMITIVE_TOPOLOGY_POINT_LIST: points from vertices
			VK_PRIMITIVE_TOPOLOGY_LINE_LIST: line from every 2 vertices without reuse
			VK_PRIMITIVE_TOPOLOGY_LINE_STRIP: the end vertex of every line is used as start vertex for the done line
			VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST: triangle from every 3 vertices without reuse
			VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP: the second and third vertex of every triangle are used as first two vertices of the done triangle
			*/
			VkRect2D.Buffer scissor=VkRect2D.callocStack(1, stack);
			
			scissor.offset(VkOffset2D.callocStack(stack).x(0).y(0));
			scissor.extent(extent);
			
			/*
			VK_POLYGON_MODE_FILL: fill the area of the polygon with fragments
			VK_POLYGON_MODE_LINE: polygon edges are drawn as lines
			VK_POLYGON_MODE_POINT: polygon vertices are drawn as points
			 */
			
			VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment=VkPipelineColorBlendAttachmentState.callocStack(1, stack);
			colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT|VK_COLOR_COMPONENT_G_BIT|VK_COLOR_COMPONENT_B_BIT|VK_COLOR_COMPONENT_A_BIT)
			                    .blendEnable(false)
			                    .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
			                    .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
			                    .colorBlendOp(VK_BLEND_OP_ADD)
			                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
			                    .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
			                    .alphaBlendOp(VK_BLEND_OP_ADD);
			
			VkPipelineColorBlendStateCreateInfo colorBlending=VkPipelineColorBlendStateCreateInfo.callocStack(stack);
			colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
			             .logicOpEnable(false)
			             .logicOp(VK_LOGIC_OP_COPY)
			             .pAttachments(colorBlendAttachment);
			colorBlending.blendConstants().put(0, 0);
			colorBlending.blendConstants().put(1, 0);
			colorBlending.blendConstants().put(2, 0);
			colorBlending.blendConstants().put(3, 0);
			
			VkPipelineDynamicStateCreateInfo dynamicState=VkPipelineDynamicStateCreateInfo.callocStack(stack);
			dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
			dynamicState.pDynamicStates(BufferUtil.of(stack, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_LINE_WIDTH, VK_DYNAMIC_STATE_SCISSOR).flip());
			
			VkPipelineLayoutCreateInfo pipelineLayoutInfo=VkPipelineLayoutCreateInfo.callocStack(stack);
			pipelineLayoutInfo.pNext(0)
			                  .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
			                  .pSetLayouts(null) // Optional
			                  .pPushConstantRanges(null);
			pipelineLayout=Vk.createPipelineLayout(gpu.getDevice(), pipelineLayoutInfo, stack.callocLong(1));
			
			VkPipelineDepthStencilStateCreateInfo depthStencilState=VkPipelineDepthStencilStateCreateInfo.callocStack(stack);
			depthStencilState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
			                 .depthTestEnable(false)
			                 .depthWriteEnable(false)
			                 .depthCompareOp(VK_COMPARE_OP_ALWAYS)
			                 .depthBoundsTestEnable(false)
			                 .stencilTestEnable(false);
			
			depthStencilState.back()
			                 .failOp(VK_STENCIL_OP_KEEP)
			                 .passOp(VK_STENCIL_OP_KEEP)
			                 .compareOp(VK_COMPARE_OP_ALWAYS);
			
			depthStencilState.front(depthStencilState.back());
			
			
			VkPipelineShaderStageCreateInfo.Buffer stages=VkPipelineShaderStageCreateInfo.callocStack(2+(gs!=null?1:0), stack);
			
			stages.put(0, vs.pipelineShaderStageCreate(stack));
			if(gs!=null) stages.put(1, gs.pipelineShaderStageCreate(stack));
			stages.put(gs!=null?2:1, fs.pipelineShaderStageCreate(stack));
			
			VkModelFormat format=model.getFormat();
			
			VkPipelineVertexInputStateCreateInfo     vertexInput        =VkPipelineVertexInputStateCreateInfo.callocStack(stack);
			VkVertexInputBindingDescription.Buffer   vertexInputBindings=VkVertexInputBindingDescription.callocStack(1, stack);
			VkVertexInputAttributeDescription.Buffer vertexInputAttrs   =VkVertexInputAttributeDescription.callocStack(format.partCount(), stack);
			
			vertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
			           .pVertexAttributeDescriptions(vertexInputAttrs)
			           .pVertexBindingDescriptions(vertexInputBindings);
			
			vertexInputBindings.get(0)
			                   .binding(0)
			                   .stride(format.getSizeBits()/Byte.SIZE)
			                   .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
			
			for(int i=0;i<format.partCount();i++){
				vertexInputAttrs.get(i)
				                .binding(0)
				                .location(i)
				                .format(format.getFormat(i))
				                .offset(format.getOffset(i)/Byte.SIZE);
			}
			
			VkGraphicsPipelineCreateInfo.Buffer pipelineInfo=VkGraphicsPipelineCreateInfo.callocStack(1, stack);
			pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
			            .layout(pipelineLayout.get())
			            .renderPass(renderPass.get())
			            .pVertexInputState(vertexInput)
			            .pInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo
					                                 .callocStack(stack)
					                                 .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
					                                 .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
					                                 .primitiveRestartEnable(false))
			            .pRasterizationState(VkPipelineRasterizationStateCreateInfo
					                                 .callocStack(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
					                                 .depthClampEnable(false)
					                                 .rasterizerDiscardEnable(false)
					                                 .polygonMode(VK_POLYGON_MODE_FILL)
					                                 .lineWidth(1)
					                                 .cullMode(VK_CULL_MODE_BACK_BIT)
					                                 .frontFace(VK_FRONT_FACE_CLOCKWISE)
					                                 .depthBiasEnable(false)
					                                 .depthBiasConstantFactor(0)
					                                 .depthBiasClamp(0)
					                                 .depthBiasSlopeFactor(0))
			            .pMultisampleState(VkPipelineMultisampleStateCreateInfo
					                               .callocStack(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
					                               .sampleShadingEnable(false)
					                               .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
					                               .minSampleShading(1)
					                               .pSampleMask(null)
					                               .alphaToCoverageEnable(false)
					                               .alphaToOneEnable(false))
			            .pColorBlendState(colorBlending)
			            .pViewportState(VkPipelineViewportStateCreateInfo
					                            .callocStack(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
					                            .pViewports(viewport)
					                            .pScissors(scissor))
			            .pDepthStencilState(depthStencilState)
			            .pStages(stages)
			            .pDynamicState(dynamicState)
			            .subpass(0)
			            .basePipelineHandle(VK_NULL_HANDLE)
			            .basePipelineIndex(-1)
			;
			
			graphicsPipeline=Vk.createGraphicsPipelines(gpu.getDevice(), pipelineInfo, stack.callocLong(1))[0];
			
		}catch(Exception e){
			throw e;
		}
	}
	
	public void destroy(VkDevice device){
		if(fs!=null){
			vs.destroy(device);
			fs.destroy(device);
			vs=fs=null;
		}
		
		graphicsPipeline.destroy(gpu.getDevice());
		pipelineLayout.destroy(gpu.getDevice());
		
		gpu=null;
		graphicsPipeline=null;
		pipelineLayout=null;
	}
	
	public VkGraphicsPipeline getGraphicsPipeline(){
		return graphicsPipeline;
	}
	
	private static byte[] read(String name, Type type) throws IOException{
		return Files.readAllBytes(new File("res\\shaders", name+'.'+type.extension+".spv").toPath());
	}
}
