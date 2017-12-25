package com.lapissea.vulkanimpl;

import com.lapissea.util.UtilL;
import com.lapissea.vulkanimpl.model.BufferBuilder;
import com.lapissea.vulkanimpl.model.VkBufferMemory;
import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.simplevktypes.*;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.lapissea.vulkanimpl.BufferUtil.*;
import static org.lwjgl.system.MemoryUtil.*;
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
	private VkPipelineLayout                pipelineLayout;
	private VkGpu                           gpu;
	private VkGraphicsPipeline              graphicsPipeline;
	private VkDescriptorSetLayoutCreateInfo descriptorSetLayout;
	private VkDescriptorSetLayout           layout;
	private VkBufferMemory                  uniformBuffer;
	private VkDescriptorPool                descriptorPool;
	private VkDescriptorSet                 descriptorSet;
	private VkViewport.Buffer               viewport;
	
	public Shader(String name){this.name=name;}
	
	public void create(VkGpu gpu, VkViewport.Buffer viewport, VkRenderPass renderPass, VkModel model){
		
		VkModelFormat format=model.getFormat();
		
		this.viewport=viewport;
		
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
			
			VkExtent2D extent=VkExtent2D.callocStack(stack);
			extent.width((int)viewport.width())
			      .height((int)viewport.height());
			
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
			
			
			VkDescriptorSetLayoutBinding.Buffer uboLayoutBindings=VkDescriptorSetLayoutBinding.callocStack(2, stack);
			uboLayoutBindings.get(0)
			                 .binding(0)
			                 .descriptorCount(1)
			                 .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			                 .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
			uboLayoutBindings.get(1)
			                 .binding(1)
			                 .descriptorCount(1)
			                 .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
			                 .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
			
			descriptorSetLayout=VkDescriptorSetLayoutCreateInfo.calloc();
			descriptorSetLayout.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
			                   .pBindings(uboLayoutBindings);
			
			layout=Vk.createDescriptorSetLayout(gpu.getDevice(), descriptorSetLayout, stack);
			
			
			VkPipelineLayoutCreateInfo layoutInfo=VkPipelineLayoutCreateInfo.callocStack(stack);
			layoutInfo.pNext(0)
			          .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
			          .pPushConstantRanges(null)
			          .pSetLayouts(buffSingle(stack, layout));
			
			pipelineLayout=Vk.createPipelineLayout(gpu.getDevice(), layoutInfo, stack.callocLong(1));
			
			VkPipelineDepthStencilStateCreateInfo depthStencilState=VkPipelineDepthStencilStateCreateInfo.callocStack(stack);
			depthStencilState.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
			                 .depthTestEnable(true)
			                 .depthWriteEnable(false)
			                 .depthCompareOp(VK_COMPARE_OP_LESS)
			                 .depthBoundsTestEnable(false)
			                 .stencilTestEnable(false)
			                 .minDepthBounds(0)
			                 .maxDepthBounds(1);
			
			depthStencilState.back()
			                 .failOp(VK_STENCIL_OP_KEEP)
			                 .passOp(VK_STENCIL_OP_KEEP)
			                 .compareOp(VK_COMPARE_OP_ALWAYS);
			
			depthStencilState.front(depthStencilState.back());
			
			
			VkPipelineShaderStageCreateInfo.Buffer stages=VkPipelineShaderStageCreateInfo.callocStack(2+(gs!=null?1:0), stack);
			
			stages.put(0, vs.pipelineShaderStageCreate(stack));
			if(gs!=null) stages.put(1, gs.pipelineShaderStageCreate(stack));
			stages.put(gs!=null?2:1, fs.pipelineShaderStageCreate(stack));
			
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
			gpu.waitIdle();
			
			initUniforms(model);
			uploadUniforms();
			
		}catch(Exception e){
			throw e;
		}
	}
	
	public void initUniforms(VkModel model){
		int uniSiz;
		
		uniSiz=Float.SIZE*16*3+Float.SIZE;
		
		uniformBuffer=gpu.createBufferMem(uniSiz, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		
		try(MemoryStack stack=MemoryStack.stackPush()){
			
			descriptorPool=gpu.createDescriptorPool(1);
			
			
			descriptorSet=Vk.allocateDescriptorSets(gpu.getDevice(), VkDescriptorSetAllocateInfo.callocStack(stack)
			                                                                                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
			                                                                                    .descriptorPool(descriptorPool.get())
			                                                                                    .pSetLayouts(buffSingle(stack, layout)), stack);
			
			VkDescriptorBufferInfo.Buffer bufferInfo=VkDescriptorBufferInfo.callocStack(1, stack);
			bufferInfo.get(0)
			          .buffer(uniformBuffer.getBuffer().get())
			          .offset(0)
			          .range(uniformBuffer.byteSize());
			
			VkDescriptorImageInfo.Buffer imageInfo=VkDescriptorImageInfo.callocStack(1, stack);
			imageInfo.get(0)
			         .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
			         .imageView(model.texture.getView().get())
			         .sampler(model.texture.getSampler().get());
			
			VkWriteDescriptorSet.Buffer descriptorWrite=VkWriteDescriptorSet.callocStack(2, stack);
			descriptorWrite.get(0)
			               .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			               .dstSet(descriptorSet.get())
			               .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			               .pBufferInfo(bufferInfo)
			               .dstBinding(0)
			               .dstArrayElement(0);
			descriptorWrite.get(1)
			               .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			               .dstSet(descriptorSet.get())
			               .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
			               .pImageInfo(imageInfo)
			               .dstBinding(0)
			               .dstArrayElement(0);
			
			
			vkUpdateDescriptorSets(gpu.getDevice(), descriptorWrite, null);
		}
	}
	
	public void uploadUniforms(){
		Matrix4f model=new Matrix4f();
		Matrix4f view =new Matrix4f();
		Matrix4f proj =new Matrix4f();
		
		double tim=System.currentTimeMillis()/1000D;

//		view.rotate((float)Math.cos((tim)%(Math.PI*2))/3, 0, 1, 0);
//		view.rotate((float)Math.cos((tim*2)%(Math.PI*2))/3, 1, 1, 0);
		view.translate(0, 0, -1F);
		view.rotate(1, 0, 0, 0);
		
		model.rotate((float)((tim/2)%(Math.PI*2)), 1, 0, 0);
		
		proj.perspective(90, viewport.width()/viewport.height(), 0.001F, 1000, false);
		
		try(VkBufferMemory.MemorySession ses=uniformBuffer.requestMemory(gpu)){
			BufferBuilder.put(ses.memory, model);
			BufferBuilder.put(ses.memory, view);
			BufferBuilder.put(ses.memory, proj);
			BufferBuilder.put(ses.memory, (float)((tim*20)%(Math.PI*2)));
		}
	}
	
	public void destroy(){
		descriptorPool.destroy(gpu);
		descriptorSetLayout.free();
		layout.destroy(gpu);
		if(fs!=null){
			vs.destroy(gpu);
			fs.destroy(gpu);
			vs=fs=null;
		}
		uniformBuffer.destroy(gpu);
		graphicsPipeline.destroy(gpu);
		pipelineLayout.destroy(gpu);
		
		gpu=null;
		graphicsPipeline=null;
		pipelineLayout=null;
	}
	
	public VkGraphicsPipeline getGraphicsPipeline(){
		return graphicsPipeline;
	}
	
	public VkPipelineLayout getPipelineLayout(){
		return pipelineLayout;
	}
	
	public VkDescriptorSet getDescriptorSet(){
		return descriptorSet;
	}
	
	private static byte[] read(String name, Type type) throws IOException{
		return Files.readAllBytes(new File("res\\shaders", name+'.'+type.extension+".spv").toPath());
	}
}
