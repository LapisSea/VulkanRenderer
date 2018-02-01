package com.lapissea.vulkanimpl.shaders;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.UtilL;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.shaders.states.VkDrawMode;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.types.VkShaderModule;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.stream.Stream;

import static com.lapissea.util.UtilL.*;
import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
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
	
	private final String       name;
	private final IDataManager shadersFolder;
	private final VkGpu        gpu;
	private final VkSurface    surface;
	
	private VkShaderModule[] stages;
	
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
	
	public VkShader init(){
		stages=Stream.of(loadStage(Type.VERTEX),
		                 loadStage(Type.FRAGMENT),
		                 loadStageOptional(Type.GEOMETRY))
		             .filter(Objects::nonNull)
		             .toArray(VkShaderModule[]::new);
		
		try(MemoryStack stack=stackPush()){
			ByteBuffer name=stack.UTF8("main");
			
			VkPipelineShaderStageCreateInfo[] createInfos=Stream.of(stages)
			                                                    .map(this::stageInfo)
			                                                    .toArray(VkPipelineShaderStageCreateInfo[]::new);
			
			VkPipelineVertexInputStateCreateInfo info=VkPipelineVertexInputStateCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
			    .pVertexBindingDescriptions(null)
			    .pVertexAttributeDescriptions(null);
			
			VkDrawMode drawMode=VkDrawMode.TRIANGLES;
			
			
		}
		return this;
	}
	
	private VkPipelineShaderStageCreateInfo stageInfo(VkShaderModule stage){
		VkPipelineShaderStageCreateInfo info=VkPipelineShaderStageCreateInfo.callocStack();
		info.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
		    .stage(VK_SHADER_STAGE_VERTEX_BIT)
		    .module(stage.getHandle())
		    .pName(stackUTF8("main"));
		return info;
	}
	
	
	private VkPipelineViewportStateCreateInfo basicFullScreen(IVec2iR size){
		
		VkViewport.Buffer viewports=VkViewport.mallocStack(1);
		viewports.get(0).set(0, 0, size.x(), size.y(), 0, 1);
		
		VkRect2D.Buffer scissors=VkRect2D.mallocStack(1);
		
		VkRect2D sc=scissors.get(0);
		sc.offset().set(0, 0);
		sc.extent().set(size.x(), size.y());
		
		VkPipelineViewportStateCreateInfo viewportInfo=VkPipelineViewportStateCreateInfo.callocStack();
		viewportInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
		            .viewportCount(viewports.limit())
		            .pViewports(viewports)
		            .scissorCount(scissors.limit())
		            .pScissors(scissors);
		
		return viewportInfo;
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
	
	@Override
	public void destroy(){
		forEach(stages, VkShaderModule::destroy);
		
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
}
