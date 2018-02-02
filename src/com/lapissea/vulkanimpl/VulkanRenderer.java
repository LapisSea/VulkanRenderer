package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.LogUtil;
import com.lapissea.util.TextUtil;
import com.lapissea.util.UtilL;
import com.lapissea.util.event.change.ChangeRegistryBool;
import com.lapissea.util.filechange.FileChageDetector;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.devonly.ValidationLayers;
import com.lapissea.vulkanimpl.devonly.VkDebugReport;
import com.lapissea.vulkanimpl.shaders.ShaderState;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.GlfwWindowVk;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkShaderCompiler;
import com.lapissea.vulkanimpl.util.types.VkSurface;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lapissea.vulkanimpl.VulkanRenderer.Settings.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements VkDestroyable{
	
	public static class Settings{
		
		public static final boolean DEVELOPMENT;
		
		static{
			String key="VulkanRenderer.devMode",
				devArg0=System.getProperty(key, "false"),
				devArg=devArg0.toLowerCase();
			
			if(devArg.equals("true")) DEVELOPMENT=true;
			else{
				if(devArg.equals("false")) DEVELOPMENT=false;
				else throw UtilL.exitWithErrorMsg("Invalid property: "+key+"="+devArg0+" (valid: \"true\", \"false\", \"\")");
			}
			System.setProperty("org.lwjgl.util.NoChecks", Boolean.toString(DEVELOPMENT));
			LogUtil.println("Running VulkanRenderer in "+(DEVELOPMENT?"development":"production")+" mode");
		}
		
		public ChangeRegistryBool enableVsync           =new ChangeRegistryBool(true);
		public ChangeRegistryBool enableTrippleBuffering=new ChangeRegistryBool(true);
	}
	
	public static final String ENGINE_NAME   ="JLapisor";
	public static final String ENGINE_VERSION="0.0.1";
	
	private VkDebugReport debugReport;
	
	private VkInstance instance;
	
	private VkGpu renderGpu, computeGpu;
	
	private VkSwapchain  swapchain;
	private GlfwWindowVk window;
	
	private VkSurface surface;
	
	private VkShader shader;
	private long     renderPass;
	
	private Settings settings=new Settings();
	public IDataManager assets;
	
	private long commandPool;
	
	public VulkanRenderer(IDataManager assets){
		this.assets=assets;
	}
	
	public void createContext(GlfwWindowVk window){
		this.window=window;
		window.size.register(e->onWindowResize(e.getSource()));
		
		try(MemoryStack stack=stackPush()){
			
			
			List<String> layerNames=new ArrayList<>(), extensionNames=new ArrayList<>();
			
			initAddons(stack, layerNames, extensionNames);
			
			VkInstanceCreateInfo info=VkInstanceCreateInfo.callocStack(stack);
			info.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
			    .pApplicationInfo(Vk.initAppInfo(stack, window.title.get(), "0.0.2", ENGINE_NAME, ENGINE_VERSION))
			    .ppEnabledLayerNames(Vk.stringsToPP(layerNames, stack))
			    .ppEnabledExtensionNames(Vk.stringsToPP(extensionNames, stack));
			
			instance=Vk.createInstance(info, null);
		}
		initDebugLog();
		
		surface=window.createSurface(this);
		intGpus();
		swapchain=new VkSwapchain(renderGpu, surface);
		
		
		initRenderPass();
		initGraphicsPipeline();
	}
	
	private void initRenderPass(){
		
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
			
			VkRenderPassCreateInfo renderPassInfo=VkRenderPassCreateInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
			              .pAttachments(attachments)
			              .pSubpasses(subpasses);
			renderPass=Vk.createRenderPass(renderGpu, renderPassInfo, stack.mallocLong(1));
			
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
		
		if(DEVELOPMENT){
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
			
			computeGpu=gpus.stream()
			               .filter(gpu->gpu.getDeviceExtensionProperties().containsAll(computeExtensions)&&gpu.getTransferQueue()!=null)
			               .sorted(sort.thenComparing((g1, g2)->Boolean.compare(g1==renderGpu, g2==renderGpu)))
			               .findFirst()
			               .orElseThrow(()->new RuntimeException("No Vulkan compatible devices can display to a screen or do not meet minimal requirements"));
			
			gpus.remove(renderGpu);
			gpus.remove(computeGpu);
			gpus.forEach(VkGpu::destroy);
			
			if(renderGpu==computeGpu){
				renderGpu.init(null, Vk.stringsToPP(Stream.concat(renderExtensions.stream(), computeExtensions.stream()).distinct().collect(Collectors.toList()), stack));
			}else{
				renderGpu.init(null, Vk.stringsToPP(renderExtensions, stack));
				computeGpu.init(null, Vk.stringsToPP(computeExtensions, stack));
			}
		}
	}
	
	private void initDebugLog(){
		if(!DEVELOPMENT) return;
		
		try(MemoryStack stack=stackPush()){
			
			debugReport=new VkDebugReport(this, stack, (type, prefix, code, message)->{
				if(message.startsWith("Device Extension")) return;
				
				List<String>  msgLin=TextUtil.wrapLongString(message, 100);
				StringBuilder msg   =new StringBuilder().append(type).append(": [").append(prefix).append("] Code ").append(code).append(": ");
				
				if(msgLin.size()>1) msg.append("\n").append(TextUtil.wrappedString(UtilL.array(msgLin)));
				else msg.append(msgLin.get(0));
				
				if(type==VkDebugReport.Type.ERROR||type==VkDebugReport.Type.WARNING) throw new RuntimeException(msg.toString());
//				else LogUtil.println(msg);
			});
		}
		
	}
	
	private void initGraphicsPipeline(){
		if(DEVELOPMENT){
			FileChageDetector.autoHandle(new File("res/assets/shaders/test.vert"), ()->VkShaderCompiler.compileVertex("test.vert"));
			FileChageDetector.autoHandle(new File("res/assets/shaders/test.frag"), ()->VkShaderCompiler.compileFragment("test.frag"));
		}
		
		shader=new VkShader(assets.subData("assets/shaders"), "test", getRenderGpu(), surface);
		ShaderState state=new ShaderState();
		state.setViewport(window.size);
		shader.init(state, renderPass);
	}
	
	private void onWindowResize(IVec2iR size){
		LogUtil.println(size);
	}
	
	public void render(){
	
	}
	
	public VkInstance getInstance(){
		return instance;
	}
	
	public VkGpu getRenderGpu(){
		return renderGpu;
	}
	
	public VkGpu getComputeGpu(){
		return computeGpu;
	}
	
	public GlfwWindowVk getWindow(){
		return window;
	}
	
	public Settings getSettings(){
		return settings;
	}
	
	public VkSurface getSurface(){
		return surface;
	}
	
	@Override
	public void destroy(){
		
		vkDestroyRenderPass(renderGpu.getDevice(), renderPass, null);
		shader.destroy();
		
		swapchain.destroy();
		surface.destroy();
		
		if(DEVELOPMENT){
			debugReport.destroy();
		}
		vkDestroyInstance(instance, null);
	}
	
}
