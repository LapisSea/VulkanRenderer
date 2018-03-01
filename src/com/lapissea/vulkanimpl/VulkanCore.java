package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.TextUtil;
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
		
		public ChangeRegistryBool vSyncEnabled           =new ChangeRegistryBool(true);
		public ChangeRegistryBool trippleBufferingEnabled=new ChangeRegistryBool(true);
	}
	
	public static final String ENGINE_NAME   ="JLapisor";
	public static final String ENGINE_VERSION="0.0.1";
	
	private VkDebugReport debugReport;
	
	private VkInstance instance;
	
	private VkGpu renderGpu;
//	private VkGpu computeGpu;
	
	private VkSwapchain  swapchain;
	private GlfwWindowVk window;
	
	private VkSurface surface;
	
	private VkShader     shader;
	private VkRenderPass renderPass;
	
	private Settings settings=new Settings();
	public IDataManager assets;
	
	private VkCommandPool      graphicsPool;
	private VkCommandPool      transferPool;
	private VkCommandBufferM[] sceneCommandBuffers;
	
	private boolean surfaceBad;
	
	private VkModel               model;
	private VkUniform             mainUniform;
	private VkDescriptorSetLayout uniformLayout;
	private VkDescriptorPool      descriptorPool;
	private long                  descriptorSet;
	
	private VulkanRender renderer=new VulkanRender();
	
	public VulkanCore(IDataManager assets){
		this.assets=assets;
	}
	
	public void createContext(GlfwWindowVk window){
		
		this.window=window;
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
			debugReport=new VkDebugReport(this, List.of(ERROR, WARNING, PERFORMANCE_WARNING), List.of());
		}
		
		surface=window.createSurface(this);
		intGpus();
		initUniform();
		initTexture();
		initModel();
		initSurfaceDependant();
		Vec2i prevPos=new Vec2i(window.mousePos);
		window.mousePos.register(e->{
			bulge+=Math.sqrt(e.getSource().distanceTo(prevPos))/20;
			prevPos.set(e.getSource());
		});
	}
	
	private void initTexture(){
		try{
			BufferedImage image=ImageIO.read(assets.getInStream("textures/SmugDude.png"));
			
			
			
		}catch(IOException e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void initUniform(){
		try(MemoryStack stack=stackPush()){
			int floatSize =Float.SIZE/Byte.SIZE;
			int matrixSize=floatSize*16;
			mainUniform=new VkUniform(renderGpu, matrixSize*3+floatSize, this::updateUniforms);
			
			uniformLayout=renderGpu.createDescriptorSetLayout(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
			
			VkDescriptorPoolSize.Buffer poolSize=VkDescriptorPoolSize.callocStack(1);
			poolSize.get(0)
			        .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			        .descriptorCount(1);
			
			VkDescriptorPoolCreateInfo poolInfo=VkConstruct.descriptorPoolCreateInfo(stack);
			poolInfo.pPoolSizes(poolSize)
			        .maxSets(1);
			descriptorPool=Vk.createDescriptorPool(renderGpu, poolInfo, stack.mallocLong(1));
			
			descriptorSet=descriptorPool.allocateDescriptorSets(uniformLayout.getBuffer());
			
			VkDescriptorBufferInfo.Buffer bufferInfo=VkDescriptorBufferInfo.callocStack(1, stack);
			mainUniform.put(bufferInfo.get(0));
			
			VkWriteDescriptorSet.Buffer descriptorWrite=VkWriteDescriptorSet.callocStack(1, stack);
			descriptorWrite.get(0)
			               .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
			               .dstSet(descriptorSet)
			               .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
			               .pBufferInfo(bufferInfo)
			               .dstBinding(0)
			               .dstArrayElement(0);
			
			vkUpdateDescriptorSets(renderGpu.getDevice(), descriptorWrite, null);
		}
	}
	
	float bulge;
	
	private void updateUniforms(ByteBuffer uniforms){
		bulge=bulge*0.95F;
		if(bulge<0.1) bulge=0.1F;
		
		float farPlane=1000;
		
		Matrix4f model=new Matrix4f();
		Matrix4f view =new Matrix4f();
		Matrix4f proj =new Matrix4f();
		
		double tim  =System.currentTimeMillis()/500D;
		Vec2f  mouse=new Vec2f(window.mousePos).div(window.size).sub(0.5F);
		
		model.rotate((float)((tim/2)%(Math.PI*2)), 0, 0, 1).scale(2F);
		view.lookAt(mouse.x()*5, 2, -mouse.y()*15,
		            0, 0, 0,
		            0, 0, 1);
		proj.perspective((float)Math.toRadians(80), window.size.divXY(), 0.001F, farPlane, true);
		
		FloatBuffer uniformsF=uniforms.asFloatBuffer();
		
		int matrixSize=16;
		model.get(0, uniformsF);
		view.get(matrixSize, uniformsF);
		proj.get(matrixSize*2, uniformsF);
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
		
		model=modelBuilder.bake(renderGpu, transferPool);
	}
	
	private void initSurfaceDependant(){
		
		swapchain=new VkSwapchain(renderGpu, surface);
		renderPass=createRenderPass();
		shader=createGraphicsPipeline(model.getFormat());
		swapchain.initFrameBuffers(renderPass);
		initScene();
	}
	
	private void initScene(){
		
		sceneCommandBuffers=graphicsPool.allocateCommandBuffers(swapchain.getFrames().size());
		for(VkSwapchain.Frame frame : swapchain.getFrames()){
			VkCommandBufferM sceneBuffer=sceneCommandBuffers[frame.index];
			
			sceneBuffer.begin();
			renderPass.begin(sceneBuffer, frame.getFrameBuffer(), surface.getSize(), new ColorM(0, 0, 0, 0));
			
			
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
	
	private VkRenderPass createRenderPass(){
		
		try(MemoryStack stack=stackPush()){
			
			VkAttachmentDescription.Buffer attachments=VkAttachmentDescription.callocStack(1, stack);
			attachments.get(0)//color attachment
			           .format(swapchain.getFormat()).samples(VK_SAMPLE_COUNT_1_BIT)
			           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
			           .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
			           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
			           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			           .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			
			VkAttachmentReference.Buffer colorAttachmentRef=VkAttachmentReference.callocStack(1, stack);
			colorAttachmentRef.get(0) //fragment out color
			                  .attachment(0)
			                  .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
			
			VkSubpassDescription.Buffer subpasses=VkSubpassDescription.callocStack(1, stack);
			subpasses.get(0)
			         .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
			         .colorAttachmentCount(colorAttachmentRef.limit())
			         .pColorAttachments(colorAttachmentRef);
			
			
			VkSubpassDependency.Buffer dependency=VkSubpassDependency.callocStack(1, stack);
			dependency.get(0)
			          .srcSubpass(VK_SUBPASS_EXTERNAL)
			          .dstSubpass(0)
			          .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)//wait for swapchain to bake reading the image that is presented
			          .srcAccessMask(0)
			          .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
			          .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT|VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
			
			VkRenderPassCreateInfo renderPassInfo=VkRenderPassCreateInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
			              .pAttachments(attachments)
			              .pSubpasses(subpasses)
			              .pDependencies(dependency);
			return Vk.createRenderPass(renderGpu, renderPassInfo, stack.mallocLong(1));
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
			graphicsPool=renderGpu.getGraphicsQueue().createCommandPool();
			transferPool=renderGpu.getTransferQueue().createCommandPool();
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
		try(MemoryStack stack=stackPush()){
			shader.init(state, renderPass, stack.longs(uniformLayout.getHandle()));
		}
		
		return shader;
	}
	
	private void onWindowResize(IVec2iR size){
		surfaceBad=true;
	}
	
	private void recreateSurface(){
		renderGpu.waitIdle();
		surfaceBad=false;
		destroySurfaceDependant();
		initSurfaceDependant();
	}
	
	private void destroySurfaceDependant(){
		forEach(sceneCommandBuffers, VkCommandBufferM::destroy);
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
		
		uniformLayout.destroy();
		mainUniform.destroy();
		
		descriptorPool.destroy();
		
		graphicsPool.destroy();
		transferPool.destroy();
		renderGpu.destroy();
		
		renderer.destroy();
		
		if(DEV_ON){
			debugReport.destroy();
		}
		vkDestroyInstance(instance, null);
	}
	
	public void render(){
		
		mainUniform.updateBuffer();
		
		submitRender();
	}
	
	private void submitRender(){
		
		if(surfaceBad) recreateSurface();
		
		VkSwapchain.Frame frame=swapchain.acquireNextFrame();
		
		if(frame==null){
			surfaceBad=true;
			return;
		}
		
		if(renderer.render(swapchain, frame, sceneCommandBuffers[frame.index])) surfaceBad=true;
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