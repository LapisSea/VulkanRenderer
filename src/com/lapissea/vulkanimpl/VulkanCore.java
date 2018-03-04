package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.glfw.BuffUtil;
import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.util.event.change.ChangeRegistryBool;
import com.lapissea.vec.Vec2f;
import com.lapissea.vec.Vec2i;
import com.lapissea.vec.color.ColorM;
import com.lapissea.vec.color.IColorM;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.devonly.ValidationLayers;
import com.lapissea.vulkanimpl.devonly.VkDebugReport;
import com.lapissea.vulkanimpl.renderer.model.VkModel;
import com.lapissea.vulkanimpl.renderer.model.VkModelBuilder;
import com.lapissea.vulkanimpl.renderer.model.VkModelFormat;
import com.lapissea.vulkanimpl.shaders.ShaderState;
import com.lapissea.vulkanimpl.shaders.VkPipelineInput;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import com.lapissea.vulkanimpl.util.VkConstruct;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.types.*;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lapissea.util.UtilL.*;
import static com.lapissea.vulkanimpl.devonly.VkDebugReport.Type.*;
import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCore implements VkDestroyable{
	public static final boolean VULKAN_INSTALLED;
	
	static{
		boolean vulkanInstalled=false;
		try{
			System.loadLibrary("vulkan-1");
			vulkanInstalled=true;
		}catch(Throwable e){ }
		VULKAN_INSTALLED=vulkanInstalled;
	}
	
	public static class Settings{
		
		public ChangeRegistryBool vSyncEnabled          =new ChangeRegistryBool(true);
		public ChangeRegistryBool tripleBufferingEnabled=new ChangeRegistryBool(true);
	}
	
	public static final String ENGINE_NAME   ="JLapisor";
	public static final String ENGINE_VERSION="0.0.1";
	
	private VkDebugReport debugReport;
	
	private VkInstance instance;
	
	private VkGpu renderGpu;
//	private VkGpu computeGpu;
	
	private       VkSwapchain  swapchain;
	private final GlfwWindowVk window;
	
	private VkSurface surface;
	
	private VkShader     shader;
	private VkRenderPass renderPass;
	
	private Settings settings=new Settings();
	public IDataManager assets;
	
	private VkCommandBufferM[] sceneCommandBuffers;
	
	private VkModel               model;
	private VkTexture             modelTexture;
	private VkUniform             mainUniform;
	private VkDescriptorSetLayout uniformLayout;
	private VkDescriptorPool      descriptorPool;
	private long                  descriptorSet;
	
	
	private       VulkanRender renderer    =new VulkanRender();
	private final Object       recreateLock=new Object();
	
	public VulkanCore(IDataManager assets, GlfwWindowVk window){
		this.assets=assets;
		this.window=window;
	}
	
	public void createContext(){
		
		window.size.register(e->onWindowResize(e.getSource()));
		
		try(MemoryStack stack=stackPush()){
			
			List<String> layerNames=new ArrayList<>(), extensionNames=new ArrayList<>();
			
			initAddons(stack, layerNames, extensionNames);
			
			VkInstanceCreateInfo info=VkConstruct.instanceCreateInfo(stack);
			info.pApplicationInfo(Vk.initAppInfo(stack, window.title.get(), "0.0.2", ENGINE_NAME, ENGINE_VERSION))
			    .ppEnabledLayerNames(Vk.stringsToPP(layerNames, stack))
			    .ppEnabledExtensionNames(Vk.stringsToPP(extensionNames, stack));
			
			instance=Vk.createInstance(info, null);
		}
		
		if(DEV_ON){
			debugReport=new VkDebugReport(this, List.of(ERROR, WARNING, PERFORMANCE_WARNING), List.of(/*INFORMATION*/));
		}
		
		surface=window.createSurface(this);
		intGpus();
		modelTexture=loadTexture("Birdy");
		initModel();
		initUniform();
		initSurfaceDependant();
		
		Vec2i prevPos=new Vec2i(window.mousePos);
		window.mousePos.register(e->{
			bulge+=Math.sqrt(e.getSource().distanceTo(prevPos))/20;
			prevPos.set(e.getSource());
		});
	}
	
	private VkTexture loadTexture(String name){
		BufferedImage image;
		try{
			image=ImageIO.read(assets.getInStream("textures/"+name+".png"));
		}catch(IOException e){
			throw UtilL.uncheckedThrow(e);
		}
		
		int width =image.getWidth();
		int height=image.getHeight();
		
		ByteBuffer buff=BuffUtil.imageToBuffer(image, memAlloc(width*height*4));
		
		VkBuffer       stagingBuffer=renderGpu.createBuffer(VK_BUFFER_USAGE_TRANSFER_SRC_BIT, buff.capacity());
		VkDeviceMemory stagingMemory=stagingBuffer.createMemory(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
		
		stagingMemory.memorySession(stagingBuffer.size, mem->{
			mem.put(buff);
			memFree(buff);
		});
		
		VkImage textureImage=renderGpu.create2DImage(width, height, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT);
		
		VkDeviceMemory textureImageMemory=textureImage.createMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		textureImage.transitionLayout(renderGpu.getTransferQueue(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		textureImage.copyFromBuffer(renderGpu.getTransferQueue(), stagingBuffer);
		textureImage.transitionLayout(renderGpu.getGraphicsQueue(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
		
		stagingMemory.destroy();
		stagingBuffer.destroy();
		return new VkTexture(textureImage, textureImageMemory).createView(VkImageAspect.COLOR).createSampler();
	}
	
	private void initUniform(){
		try(MemoryStack stack=stackPush()){
			int floatSize =Float.SIZE/Byte.SIZE;
			int matrixSize=floatSize*16;
			mainUniform=new VkUniform(renderGpu, matrixSize*3+floatSize, this::updateUniforms);
			
			uniformLayout=renderGpu.createDescriptorSetLayout(VK_SHADER_STAGE_VERTEX_BIT, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
			                                                  VK_SHADER_STAGE_FRAGMENT_BIT, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			
			VkDescriptorPoolSize.Buffer poolSize=VkDescriptorPoolSize.callocStack(2);
			poolSize.get(0)
			        .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			        .descriptorCount(1);
			poolSize.get(1)
			        .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
			        .descriptorCount(1);
			
			VkDescriptorPoolCreateInfo poolInfo=VkConstruct.descriptorPoolCreateInfo(stack);
			poolInfo.pPoolSizes(poolSize)
			        .maxSets(1);
			descriptorPool=Vk.createDescriptorPool(renderGpu, poolInfo, stack.mallocLong(1));
			
			descriptorSet=descriptorPool.allocateDescriptorSets(uniformLayout.getBuffer());
			
			VkDescriptorBufferInfo.Buffer bufferInfo=VkDescriptorBufferInfo.callocStack(1, stack);
			mainUniform.put(bufferInfo.get(0));
			
			VkDescriptorImageInfo.Buffer imageInfo=VkDescriptorImageInfo.callocStack(1, stack);
			modelTexture.put(imageInfo.get(0));
			
			
			VkWriteDescriptorSet.Buffer descriptorWrite=VkWriteDescriptorSet.callocStack(2, stack);
			descriptorWrite.get(0)
			               .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			               .dstSet(descriptorSet)
			               .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			               .pBufferInfo(bufferInfo)
			               .dstBinding(0)
			               .dstArrayElement(0);
			descriptorWrite.get(1)
			               .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			               .dstSet(descriptorSet)
			               .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
			               .dstBinding(1)
			               .dstArrayElement(0)
			               .pImageInfo(imageInfo);
			
			vkUpdateDescriptorSets(renderGpu.getDevice(), descriptorWrite, null);
		}
	}
	
	float bulge;
	
	private void updateUniforms(ByteBuffer uniforms){
		bulge=bulge*0.95F;
		if(bulge<0.1) bulge=0.1F;
		
		float farPlane=1000;
		
		Matrix4f model     =new Matrix4f();
		Matrix4f view      =new Matrix4f();
		Matrix4f projection=new Matrix4f();
		
		double tim  =System.currentTimeMillis()/500D;
		Vec2f  mouse=new Vec2f(window.mousePos).div(window.size).sub(0.5F);
		
		model.rotate((float)((tim/2)%(Math.PI*2)), 0, 0, 1).scale(2F);
		view.lookAt(mouse.x()*5, 2, -mouse.y()*10,
		            0, 0, 0,
		            0, 0, 1);
		projection.perspective((float)Math.toRadians(80), window.size.divXY(), 0.001F, farPlane, true);
		
		FloatBuffer uniformsF=uniforms.asFloatBuffer();
		
		int matrixSize=16;
		model.get(0, uniformsF);
		view.get(matrixSize, uniformsF);
		projection.get(matrixSize*2, uniformsF);
		uniformsF.put(matrixSize*3, (float)Math.sqrt(bulge));
	}
	
	private void initModel(){
		
		VkModelBuilder modelBuilder=new VkModelBuilder(VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32A32_SFLOAT, VK_FORMAT_R32G32_SFLOAT);
		
		modelBuilder.put3F(-0.5F, -0.5F, -0.5F).put4F(IColorM.randomRGBA()).put2F(0, 0).next();
		modelBuilder.put3F(-0.5F, +0.5F, -0.5F).put4F(IColorM.randomRGBA()).put2F(1, 0).next();
		modelBuilder.put3F(+0.5F, +0.5F, -0.5F).put4F(IColorM.randomRGBA()).put2F(1, 1).next();
		modelBuilder.put3F(+0.5F, -0.5F, -0.5F).put4F(IColorM.randomRGBA()).put2F(0, 1).next();
		
		modelBuilder.put3F(-0.5F, -0.5F, +0.5F).put4F(IColorM.randomRGBA()).put2F(0, 1).next();
		modelBuilder.put3F(-0.5F, +0.5F, +0.5F).put4F(IColorM.randomRGBA()).put2F(0, 0).next();
		modelBuilder.put3F(+0.5F, +0.5F, +0.5F).put4F(IColorM.randomRGBA()).put2F(1, 0).next();
		modelBuilder.put3F(+0.5F, -0.5F, +0.5F).put4F(IColorM.randomRGBA()).put2F(1, 1).next();
		
		modelBuilder.addIndices(
			0, 1, 2,
			0, 2, 3,
			4, 6, 5,
			4, 7, 6,
			0, 4, 1,
			4, 5, 1,
			2, 1, 5,
			2, 5, 6,
			3, 7, 4,
			4, 0, 3,
			6, 3, 2,
			3, 6, 7
		                       );
		
		model=modelBuilder.bake(renderGpu);
	}
	
	private void initSurfaceDependant(){
		swapchain=new VkSwapchain(renderGpu, surface, window.size);
		renderPass=swapchain.createRenderPass();
		swapchain.initFrameBuffers(renderPass);
		shader=createGraphicsPipeline(model.getFormat());
		initScene();
	}
	
	private void initScene(){
		
		sceneCommandBuffers=renderGpu.getGraphicsQueue().getPool().allocateCommandBuffers(swapchain.getFrames().size());
		for(VkSwapchain.Frame frame : swapchain.getFrames()){
			VkCommandBufferM sceneBuffer=sceneCommandBuffers[frame.index];
			
			sceneBuffer.begin();
			renderPass.begin(sceneBuffer, frame.getFrameBuffer(), swapchain.getSize(), new ColorM(0, 0, 0, 0));
			
			
			shader.bind(sceneBuffer);
			try(MemoryStack stack=stackPush()){
				shader.bindDescriptorSets(sceneBuffer, stack.longs(descriptorSet));
			}
			
			model.bind(sceneBuffer);
			model.render(sceneBuffer);
			
			sceneBuffer.endRenderPass();
			sceneBuffer.end();
		}
	}
	
	private void validateList(List<String> supported, List<String> requested, String type){
		List<String> fails=requested.stream()
		                            .filter(l->!supported.contains(l))
		                            .map(s->s==null?"<NULL_STRING>":s.isEmpty()?"<EMPTY_STRING>":s)
		                            .collect(Collectors.toList());
		
		if(!fails.isEmpty()){
			throw new IllegalStateException(TextUtil.plural(type, fails.size())+" not supported: "+fails.stream().collect(Collectors.joining(", ")));
		}
	}
	
	private void initAddons(MemoryStack stack, List<String> layerNames, List<String> extensionNames){
		
		if(DEV_ON){
			ValidationLayers.addLayers(layerNames);
			
			extensionNames.add(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
			
		}
		
		validateList(Vk.enumerateInstanceLayerProperties(stack).stream().map(VkLayerProperties::layerNameString).collect(Collectors.toList()), layerNames, "Layer");
		
		PointerBuffer requ=glfwGetRequiredInstanceExtensions();
		Stream.generate(requ::getStringUTF8).limit(requ.limit()).forEach(extensionNames::add);
		
		validateList(Vk.enumerateInstanceExtensionProperties(stack).stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toList()), extensionNames, "Extension");
		
	}
	
	private long getGpuVRam(VkGpu g){
		return g.getMemoryProperties()
		        .memoryHeaps()
		        .stream()
		        .filter(heap->(heap.flags()&VK_MEMORY_HEAP_DEVICE_LOCAL_BIT)==1)
		        .mapToLong(VkMemoryHeap::size)
		        .max()
		        .orElse(0);
	}
	
	private void intGpus(){
		try(MemoryStack stack=stackPush()){
			List<VkGpu> gpus=Arrays.stream(Vk.getPhysicalDevices(stack, instance)).map(d->new VkGpu(this, d)).collect(Collectors.toList());
			if(gpus.isEmpty()) throw new RuntimeException("No devices support Vulkan");
			
			List<String> renderExtensions =new ArrayList<>(List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
			List<String> computeExtensions=new ArrayList<>(List.of());
			
			Comparator<VkGpu> sort=(g1, g2)->-Long.compare(getGpuVRam(g1), getGpuVRam(g2));
			
			renderGpu=gpus.stream()
			              .filter(gpu->{
				
				              if(!gpu.getDeviceExtensionProperties().containsAll(renderExtensions)) return false;
				
				              if(gpu.getGraphicsQueue()==null||
				                 gpu.getSurfaceQueue()==null||
				                 gpu.getTransferQueue()==null) return false;
				
				              VkPhysicalDeviceFeatures features=gpu.getPhysicalFeatures();
				
				              return features.geometryShader()&&
				                     features.shaderClipDistance()&&
				                     features.shaderTessellationAndGeometryPointSize()&&
				                     features.tessellationShader();
			              })
			              .sorted(sort)
			              .findFirst()
			              .orElseThrow(()->new RuntimeException("No Vulkan compatible devices can display to a screen or do not meet minimal requirements"));

//			computeGpu=gpus.stream()
//			               .filter(gpu->gpu.getDeviceExtensionProperties().containsAll(computeExtensions)&&gpu.getTransferQueue()!=null)
//			               .sorted(sort.thenComparing((g1, g2)->Boolean.compare(g1==renderGpu, g2==renderGpu)))
//			               .findFirst()
//			               .orElseThrow(()->new RuntimeException("No Vulkan compatible devices can display to a screen or do not meet minimal requirements"));
			
			gpus.remove(renderGpu);
//			gpus.remove(computeGpu);
			gpus.forEach(VkGpu::destroy);

//			if(renderGpu==computeGpu){
			renderGpu.init(null, Vk.stringsToPP(Stream.concat(renderExtensions.stream(), computeExtensions.stream()).distinct().collect(Collectors.toList()), stack));
			LogUtil.println("Using GPU called", renderGpu.getPhysicalProperties().deviceNameString(), "as render device");
//			}else{
//				renderGpu.init(null, Vk.stringsToPP(renderExtensions, stack));
//				computeGpu.init(null, Vk.stringsToPP(computeExtensions, stack));
//			}
			
		}
	}
	
	private VkShader createGraphicsPipeline(VkModelFormat format){
		VkShader    shader=new VkShader(assets.subData("shaders"), "test", getRenderGpu(), surface);
		ShaderState state =new ShaderState();
		
		state.setViewport(window.size);
		state.setInput(new VkPipelineInput(format));
		state.setCullMode(VkShader.Cull.NONE);
		try(MemoryStack stack=stackPush()){
			shader.init(state, renderPass, stack.longs(uniformLayout.getHandle()));
		}
		
		return shader;
	}
	
	private void recreateSurface(){
		synchronized(recreateLock){
			renderGpu.waitIdle();
			destroySurfaceDependant();
			initSurfaceDependant();
		}
	}
	
	private void destroySurfaceDependant(){
		forEach(sceneCommandBuffers, VkCommandBufferM::destroy);
		sceneCommandBuffers=null;
		shader.destroy();
		renderPass.destroy();
		swapchain.destroy();
	}
	
	@Override
	public void destroy(){
		renderGpu.waitIdle();
		
		destroySurfaceDependant();
		
		surface.destroy();
		
		model.destroy();
		modelTexture.destroy();
		
		uniformLayout.destroy();
		mainUniform.destroy();
		
		descriptorPool.destroy();
		
		renderGpu.destroy();
		
		renderer.destroy();
		
		if(DEV_ON){
			debugReport.destroy();
		}
		vkDestroyInstance(instance, null);
	}
	
	private void onWindowResize(IVec2iR size){
		recreateSurface();
	}
	
	public void render(){
		mainUniform.updateBuffer();
		submitRender();
	}
	
	private void submitRender(){
		synchronized(recreateLock){
			
			VkSwapchain.Frame frame=swapchain.acquireNextFrame();
			
			if(frame==null){
				recreateSurface();
				return;
			}
			
			if(renderer.render(swapchain, frame, sceneCommandBuffers[frame.index])) recreateSurface();
			
		}
	}
	
	public VkInstance getInstance(){
		return instance;
	}
	
	public VkGpu getRenderGpu(){
		return renderGpu;
	}

//	public VkGpu getComputeGpu(){ return computeGpu; }
	
	public GlfwWindowVk getWindow(){
		return window;
	}
	
	public Settings getSettings(){
		return settings;
	}
	
	public VkSurface getSurface(){
		return surface;
	}
	
	
}
