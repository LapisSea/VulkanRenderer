package com.lapissea.vulkanimpl.shaders;

import com.lapissea.util.NotNull;
import com.lapissea.util.Nullable;
import com.lapissea.vulkanimpl.Vk;
import com.lapissea.vulkanimpl.VkGpu;
import com.lapissea.vulkanimpl.assets.ResourceManagerSourced;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.types.*;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.lapissea.util.UtilL.*;
import static com.lapissea.vulkanimpl.shaders.VkShader.Type.*;
import static graphics.scenery.spirvcrossj.EShLanguage.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkShader implements VkDestroyable, VkGpuCtx{
	
	public enum Type{
		VERTEX("vert", VK_SHADER_STAGE_VERTEX_BIT, EShLangVertex),
		FRAGMENT("frag", VK_SHADER_STAGE_FRAGMENT_BIT, EShLangFragment),
		GEOMETRY("geom", VK_SHADER_STAGE_GEOMETRY_BIT, EShLangGeometry),
		TESS_CONTROL("tesc", VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT, EShLangTessControl),
		TESS_EVALUATION("tese", VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT, EShLangTessEvaluation),
		COMPUTE("comp", VK_SHADER_STAGE_COMPUTE_BIT, EShLangCompute);
		
		public final String extension;
		public final int    stageBit;
		public final int    libspirvcrossj;
		
		Type(String extension, int stageBit, int libspirvcrossj){
			this.extension=extension;
			this.stageBit=stageBit;
			this.libspirvcrossj=libspirvcrossj;
		}
		
		public static Type fromName(String path){
			for(Type value : values()){
				if(path.endsWith("."+value.extension)) return value;
				if(path.endsWith("."+value.extension+".spv")) return value;
			}
			return null;
		}
		
		public String append(String path){
			return path+"."+extension;
		}
	}
	
	public enum Cull{
		FRONT(VK_CULL_MODE_FRONT_BIT),
		BACK(VK_CULL_MODE_BACK_BIT),
		NONE(VK_CULL_MODE_NONE);
		
		public final int handle;
		
		Cull(int handle){this.handle=handle;}
	}
	
	public static final String SHADER_ROOT="assets/shaders/";
	
	public final  String           name;
	private final VkGpu            gpu;
	private final VkSurface        surface;
	private       boolean          destroyed;
	private       long             pipeline;
	private final VkShaderModule[] stages=new VkShaderModule[Type.values().length];
	
	private long         pipelineLayout;
	private VkRenderPass renderPass;
	private ShaderState  state;
	
	private final ResourceManagerSourced<ByteBuffer, VkShaderCode> shadersFolder;
	
	/**
	 * @param shadersFolder Has to point to a shaders folder (SHADER_ROOT)
	 */
	public VkShader(ResourceManagerSourced<ByteBuffer, VkShaderCode> shadersFolder, String name, VkGpu gpu, VkSurface surface){
		this.name=name;
		this.gpu=gpu;
		this.surface=surface;
		this.shadersFolder=shadersFolder;

//		stageCode=convert(Type.values(), VkShaderCode[]::new, t->shadersFolder.source.exists(t.append(name))?shadersFolder.get(t.append(name)):null);
	}
	
	public VkShader init(ShaderState state, VkRenderPass renderPass, LongBuffer setLayouts){
		this.renderPass=renderPass;
		this.state=state;
		
		try(var stack=stackPush()){
			pipelineLayout=Vk.createPipelineLayout(gpu, VkConstruct.pipelineLayoutCreateInfo(stack, setLayouts, null), stack.mallocLong(1));
		}
		updateCode();
		validateStages();
		return this;
	}
	
	private boolean shaderChange;
	
	private void acceptSpirv(Type type, ByteBuffer spirv){
		try{
			if(spirv==null) return;
			shaderChange=true;
			VkShaderModule i=stages[type.ordinal()];
			if(i!=null) i.destroy();
			stages[type.ordinal()]=VkShaderModule.create(this, spirv, type);
		}finally{
			memFree(spirv);
		}
	}
	
	public synchronized boolean updateCode(){
		shaderChange=false;
		getGpu().waitIdle();
		var tasks=convert(Type.values(), CompletableFuture[]::new,
		                  t->shadersFolder.get(t.append(name))
		                                  .thenAccept(spriv->acceptSpirv(t, spriv))
		                 );
		
		for(CompletableFuture task : tasks){
			task.join();
		}
		
		if(!shaderChange){
			return false;
		}
		
		if(pipeline!=0) vkDestroyPipeline(gpu.getDevice(), pipeline, null);
		
		validateStages();
		
		try(var stack=stackPush()){
			
			var shaderStages=VkPipelineShaderStageCreateInfo.callocStack((int)Arrays.stream(stages).filter(Objects::nonNull).count(), stack);
			
			int count=0;
			for(var stage : stages){
				if(stage!=null) stage.write(shaderStages.get(count++));
			}
			
			var pipelineInfo=VkGraphicsPipelineCreateInfo.callocStack(1, stack);
			pipelineInfo.get(0)
			            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
			            .pStages(shaderStages)
			            .layout(pipelineLayout)
			            .renderPass(renderPass.getHandle())
			            .basePipelineHandle(VK_NULL_HANDLE)
			            .basePipelineIndex(-1);
			state.write(stack, pipelineInfo.get(0));
			
			
			pipeline=Vk.createGraphicsPipelines(gpu, 0, pipelineInfo, stack.mallocLong(1));
		}
		return true;
	}
	
	private void validateStages(){
		
		if(getStage(VERTEX)==null){
			throw new RuntimeException("Shader missing vertex stageFlags at \""+name+"\"!");
		}
		
	}
	
	public void bind(VkCommandBufferM cmd){
		vkCmdBindPipeline(cmd, isCompute()?VK_PIPELINE_BIND_POINT_COMPUTE:VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
	}
	
	public void bindDescriptorSets(@NotNull VkCommandBufferM cmd, @NotNull VkDescriptorSet descriptorSet){
		bindDescriptorSets(cmd, descriptorSet.getBuff());
	}
	
	public void bindDescriptorSets(@NotNull VkCommandBufferM cmd, @NotNull LongBuffer descriptorSet){
		bindDescriptorSets(cmd, descriptorSet, null);
	}
	
	public void bindDescriptorSets(@NotNull VkCommandBufferM cmd, @NotNull LongBuffer descriptorSet, @Nullable IntBuffer dynamicOffsets){
		vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, descriptorSet, dynamicOffsets);
	}
	
	private VkShaderModule getStage(@NotNull Type type){
		return stages[type.ordinal()];
	}
	
	@Override
	public void destroy(){
		destroyed=true;
		vkDestroyPipeline(gpu.getDevice(), pipeline, null);
		vkDestroyPipelineLayout(gpu.getDevice(), pipelineLayout, null);
		Arrays.stream(stages).filter(Objects::nonNull).forEach(VkShaderModule::destroy);
	}
	
	public boolean isDestroyed(){
		return destroyed;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public boolean isCompute(){
		return false;
	}
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		return o instanceof VkShader&&equals((VkShader)o);
	}
	
	public boolean equals(VkShader o){
		return o!=null&&pipeline==o.pipeline;
	}
	
	@Override
	public int hashCode(){
		return Long.hashCode(pipeline);
	}
}
