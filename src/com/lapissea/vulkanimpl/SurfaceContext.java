package com.lapissea.vulkanimpl;

import com.lapissea.util.NotNull;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.assets.ResourceManagerSourced;
import com.lapissea.vulkanimpl.shaders.ShaderState;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkRenderPass;
import com.lapissea.vulkanimpl.util.types.VkShaderCode;
import com.lapissea.vulkanimpl.util.types.VkSurface;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.function.BiConsumer;

import static com.lapissea.util.UtilL.*;

public class SurfaceContext implements VkDestroyable{
	
	private final VkGpu     gpu;
	private final VkSurface surface;
	
	private VkSwapchain        swapchain;
	private VkRenderPass       renderPass;
	private VkCommandBufferM[] sceneCommandBuffers;
	
	public SurfaceContext(@NotNull VkGpu gpu, @NotNull VkSurface surface, @NotNull IVec2iR resolution, int samples){
		this.gpu=gpu;
		this.surface=surface;
		
		swapchain=new VkSwapchain(gpu, surface, resolution, samples);
		renderPass=swapchain.createRenderPass();
		swapchain.initFrameBuffers(renderPass);
		createCmds();
	}
	
	private void createCmds(){
		sceneCommandBuffers=gpu.getGraphicsQueue().getPool().allocateCommandBuffers(swapchain.getFrames().size());
	}
	
	private void destroyCmds(){
		gpu.waitIdle();
		forEach(sceneCommandBuffers, VkCommandBufferM::destroy);
		sceneCommandBuffers=null;
	}
	
	public VkCommandBufferM[] getSceneCommandBuffers(){
		if(swapchain.getFrames().size()!=sceneCommandBuffers.length){
			destroyCmds();
			createCmds();
		}
		
		return sceneCommandBuffers;
	}
	
	public VkShader createGraphicsPipeline(ResourceManagerSourced<ByteBuffer, VkShaderCode> source, ShaderState state, String name, LongBuffer setLayouts){
		state.setViewport(swapchain.getSize());
		return new VkShader(source, "test", gpu, surface).init(state, renderPass, setLayouts);
	}
	
	@Override
	public void destroy(){
		forEach(sceneCommandBuffers, VkCommandBufferM::destroy);
		sceneCommandBuffers=null;
		
		renderPass.destroy();
		renderPass=null;
		
		swapchain.destroy();
		swapchain=null;
	}
	
	public void updateCommandBuffers(BiConsumer<VkSwapchain.Frame, VkCommandBufferM> update){
		gpu.getGraphicsQueue().getPool().reset();
		
		for(VkSwapchain.Frame frame : swapchain.getFrames()){
			VkCommandBufferM sceneBuffer=sceneCommandBuffers[frame.index];
			sceneBuffer.begin();
			update.accept(frame, sceneBuffer);
			sceneBuffer.end();
		}
	}
	
	public VkRenderPass getRenderPass(){
		return renderPass;
	}
	
	public VkSwapchain getSwapchain(){
		return swapchain;
	}
}
