package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.TextUtil;
import com.lapissea.util.event.change.ChangeRegistryBool;
import com.lapissea.vec.color.ColorM;
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
import com.lapissea.vulkanimpl.util.types.VkCommandBufferM;
import com.lapissea.vulkanimpl.util.types.VkCommandPool;
import com.lapissea.vulkanimpl.util.types.VkRenderPass;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
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

public class VulkanRenderer implements VkDestroyable{
	
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
	private VkCommandBufferM[] sceneCommandBuffers;
	
	private boolean surfaceBad;
	
	private VkModel model;
	
	public VulkanRenderer(IDataManager assets){
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
		initModel();
		initSurfaceDependant();
	}
	
	private void initModel(){
		VkModelFormat format=new VkModelFormat(VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32A32_SFLOAT);
		
		VkModelBuilder modelBuilder=new VkModelBuilder(format);
		modelBuilder.put3F(-0.5F, -0.5F, 0).put4F(1,0,0,1).next();
		modelBuilder.put3F(+0.0F, +0.5F, 0).put4F(0,1,0,1).next();
		modelBuilder.put3F(+0.5F, -0.5F, 0).put4F(1,1,1,1).next();
		
		
		model=modelBuilder.bake(renderGpu);
	}
	
	private void initSurfaceDependant(){
		
		swapchain=new VkSwapchain(renderGpu, surface);
		renderPass=createRenderPass();
		shader=createGraphicsPipeline(model.getFormat());
		graphicsPool=renderGpu.getGraphicsQueue().createCommandPool();
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
		
		PointerBuffer requ=glfwGetRequiredInstanceExtensions();
		Stream.generate(requ::getStringUTF8).limit(requ.limit()).forEach(extensionNames::add);
		
		validateList(Vk.enumerateInstanceLayerProperties(stack).stream().map(VkLayerProperties::layerNameString).collect(Collectors.toList()), layerNames, "Layer");
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
//			}else{
//				renderGpu.init(null, Vk.stringsToPP(renderExtensions, stack));
//				computeGpu.init(null, Vk.stringsToPP(computeExtensions, stack));
//			}
		}
	}
	
	private VkShader createGraphicsPipeline(VkModelFormat format){
		VkShader    shader=new VkShader(assets.subData("assets/shaders"), "test", getRenderGpu(), surface);
		ShaderState state =new ShaderState();
		state.setViewport(window.size);
		state.setInput(new VkPipelineInput(format));
		shader.init(state, renderPass);
		
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
		graphicsPool.destroy();
		renderPass.destroy();
		swapchain.destroy();
	}
	
	@Override
	public void destroy(){
		renderGpu.waitIdle();
		
		destroySurfaceDependant();
		
		surface.destroy();
		
		
		if(DEV_ON){
			debugReport.destroy();
		}
		vkDestroyInstance(instance, null);
	}
	
	public void render(){
		if(window.isHidden()) return;
		
		if(surfaceBad) recreateSurface();
		
		try(MemoryStack stack=stackPush()){
			
			VkSwapchain.Frame frame=swapchain.acquireNextFrame();
			if(frame==null){
				recreateSurface();
				frame=swapchain.acquireNextFrame();
			}
			
			LongBuffer imgAviable  =swapchain.getImageAviable().getBuffer();
			LongBuffer renderFinish=frame.getRenderFinish().getBuffer();
			LongBuffer swapchains  =swapchain.getBuffer();
			
			VkSubmitInfo mainRenderSubmitInfo=VkSubmitInfo.callocStack(stack);
			mainRenderSubmitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
			                    .pWaitSemaphores(imgAviable)
			                    .waitSemaphoreCount(imgAviable.limit())
			                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
			                    .pSignalSemaphores(renderFinish)
			                    .pCommandBuffers(stack.pointers(sceneCommandBuffers));
			
			renderGpu.getGraphicsQueue().submit(mainRenderSubmitInfo);
			
			//push rendered image to screen
			VkPresentInfoKHR presentInfo=VkPresentInfoKHR.callocStack(stack);
			presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
			           .pWaitSemaphores(renderFinish)
			           .pSwapchains(swapchains)
			           .swapchainCount(swapchains.limit())
			           .pImageIndices(stack.ints(frame.index));
			
			if(renderGpu.getSurfaceQueue().present(presentInfo)){
				surfaceBad=true;
			}
			
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
