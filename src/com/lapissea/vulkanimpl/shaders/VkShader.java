package com.lapissea.vulkanimpl.shaders;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.UtilL;
import com.lapissea.util.filechange.FileChageDetector;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkShaderCompiler;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkRenderPass;
import com.lapissea.vulkanimpl.util.types.VkShaderModule;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkShader implements VkDestroyable, VkGpuCtx{
	
	
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
	
	public static final String SHADER_ROOT="assets/shaders/";
	
	public final  String               name;
	private final IDataManager         shadersFolder;
	private final VkGpu                gpu;
	private final VkSurface            surface;
	private       long                 pipeline;
	private       List<VkShaderModule> stages;
	
	private long pipelineLayout;
	
	/**
	 * @param shadersFolder Has to point to a shaders folder (SHADER_ROOT)
	 * @param name
	 * @param gpu
	 * @param surface
	 */
	public VkShader(IDataManager shadersFolder, String name, VkGpu gpu, VkSurface surface){
		this.name=name;
		this.shadersFolder=shadersFolder;
		this.gpu=gpu;
		this.surface=surface;
	}
	
	public VkShader init(ShaderState state, VkRenderPass renderPass){
		if(DevelopmentInfo.DEV_ON){
			
			BiConsumer<String, String> winComp=(fileName, type)->{
				File target=new File("res/assets/shaders", fileName);
				if(!target.exists()) return;
				
				FileChageDetector.autoHandle(target, new File("dev/assets/shaders", fileName+".track"), ()->VkShaderCompiler.compile(fileName, type));
			};
			Arrays.stream(Type.values()).parallel().map(t->"."+t.extension).forEach(type->winComp.accept(name+type, type));
		}
		
		stages=Stream.of(loadStage(Type.VERTEX),
		                 loadStage(Type.FRAGMENT),
		                 loadStageOptional(Type.GEOMETRY))
		             .filter(Objects::nonNull)
		             .collect(Collectors.toList());
		stages=Collections.unmodifiableList(stages);
		
		try(MemoryStack stack=stackPush()){
			
			VkPipelineLayoutCreateInfo pipelineLayoutInfo=VkPipelineLayoutCreateInfo.callocStack(stack);
			pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
			                  .pSetLayouts(null)
			                  .pPushConstantRanges(null);
			
			pipelineLayout=Vk.createPipelineLayout(gpu, pipelineLayoutInfo, stack.mallocLong(1));
			
			
			VkPipelineShaderStageCreateInfo.Buffer shaderStages=VkPipelineShaderStageCreateInfo.callocStack(stages.size(), stack);
			for(int i=0;i<stages.size();i++){
				stages.get(i).write(shaderStages.get(i));
			}
			
			VkGraphicsPipelineCreateInfo.Buffer pipelineInfo=VkGraphicsPipelineCreateInfo.callocStack(1, stack);
			pipelineInfo.get(0)
			            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
			            .pStages(shaderStages)
			            .layout(pipelineLayout)
			            .renderPass(renderPass.getHandle())
			            .basePipelineHandle(VK_NULL_HANDLE)
			            .basePipelineIndex(-1);
			state.write(stack,pipelineInfo.get(0));
			
			
			pipeline=Vk.createGraphicsPipelines(gpu, 0, pipelineInfo, stack.mallocLong(1));
		}
		return this;
	}
	
	
	private VkShaderModule loadStageOptional(Type type){
		try{
			return loadStage(type);
		}catch(Throwable t){
			return null;
		}
	}
	
	private VkShaderModule loadStage(Type type){
		//check if resource is valid/exists
		String path=name+"."+type.extension+".spv";
		int    size=(int)shadersFolder.getSize(path);
		if(size<=0) throw new IllegalStateException("missing: "+path);
		
		//NOTE: On heap allocation! Possibly a lot of memory to be allocated!
		ByteBuffer spvCode=memAlloc(size);
		
		try(MemoryStack stack=stackPush()){
			
			//load shader bytes
			try(BufferedInputStream in=shadersFolder.getInStream(path)){
				int b;
				while((b=in.read())!=-1){
					spvCode.put((byte)b);
				}
			}catch(IOException e){
				UtilL.uncheckedThrow(e);
			}
			
			//loaded
			spvCode.flip();
			
			//compile
			return VkShaderModule.create(this, spvCode, type, stack);
		}finally{
			
			// make sure to unload heap memory
			memFree(spvCode);
		}
	}
	
	public void bind(VkCommandBufferM cmd){
		vkCmdBindPipeline(cmd, isCompute()?VK_PIPELINE_BIND_POINT_COMPUTE:VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
	}
	
	@Override
	public void destroy(){
		vkDestroyPipeline(gpu.getDevice(), pipeline, null);
		vkDestroyPipelineLayout(gpu.getDevice(), pipelineLayout, null);
		stages.forEach(VkShaderModule::destroy);
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public List<VkShaderModule> getStages(){
		return stages;
	}
	
	public boolean isCompute(){
		return false;
	}
}
