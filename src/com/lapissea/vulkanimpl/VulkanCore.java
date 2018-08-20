package com.lapissea.vulkanimpl;

import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.TextUtil;
import com.lapissea.util.event.change.ChangeRegistryBool;
import com.lapissea.vec.Vec3f;
import com.lapissea.vec.color.ColorMSolid;
import com.lapissea.vec.color.IColorM;
import com.lapissea.vec.color.IColorMSolid;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.assets.ResourceManagerSourced;
import com.lapissea.vulkanimpl.assets.ResourcePool;
import com.lapissea.vulkanimpl.assets.TextureNode;
import com.lapissea.vulkanimpl.devonly.ValidationLayers;
import com.lapissea.vulkanimpl.devonly.VkDebugReport;
import com.lapissea.vulkanimpl.exceptions.VkException;
import com.lapissea.vulkanimpl.renderer.lighting.Attenuation;
import com.lapissea.vulkanimpl.renderer.lighting.DirectionalLight;
import com.lapissea.vulkanimpl.renderer.lighting.PointLight;
import com.lapissea.vulkanimpl.renderer.model.VkMesh;
import com.lapissea.vulkanimpl.renderer.model.VkMeshFormat;
import com.lapissea.vulkanimpl.shaders.ShaderState;
import com.lapissea.vulkanimpl.shaders.VkPipelineInput;
import com.lapissea.vulkanimpl.shaders.VkShader;
import com.lapissea.vulkanimpl.util.*;
import com.lapissea.vulkanimpl.util.format.VkDescriptor;
import com.lapissea.vulkanimpl.util.types.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.SimplexNoise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lapissea.vulkanimpl.VkSwapchain.*;
import static com.lapissea.vulkanimpl.devonly.VkDebugReport.Type.*;
import static com.lapissea.vulkanimpl.util.DevelopmentInfo.*;
import static com.lapissea.vulkanimpl.util.format.VkDescriptor.*;
import static java.util.Arrays.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanCore implements VkDestroyable, Executor{
	
	public static final boolean VULKAN_INSTALLED;
	
	static{
		boolean vulkanInstalled=false;
		try{
			System.loadLibrary("vulkan-1");
			vulkanInstalled=true;
		}catch(Throwable e){
//			e.printStackTrace();
//			System.exit(-1);
		}
		VULKAN_INSTALLED=vulkanInstalled;
		
		if(DEV_ON){
			TextUtil.__REGISTER_CUSTOM_TO_STRING(VkExtent2D.class, extent2D->extent2D.getClass().getSimpleName()+"{width="+extent2D.width()+", height="+extent2D.height()+"}");
		}
	}
	
	private IColorMSolid clearColor=IColorM.BLACK;// new ColorMSolid(0xC4F8FF);
	
	private final Deque<Runnable> toExecute=new ArrayDeque<>();
	
	@Override
	public void execute(@NotNull Runnable command){
		synchronized(toExecute){
			toExecute.push(command);
		}
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
	
	private final GlfwWindowVk window;
	
	private VkSurface surface;
	
	private Settings     settings=new Settings();
	public  IDataManager assets;
	public  IDataManager modelSource;
	
	private ResourceManagerSourced<ByteBuffer, VkShaderCode> shaderSource;
	
	private SurfaceContext surfaceContext;
	
	private List<VkMesh>                models      =new ArrayList<>();
	private Map<VkMeshFormat, VkShader> shaderLookup=new HashMap<>();
	private ResourcePool<VkTexture>     texturePool =new ResourcePool<>(s->new TextureNode(s, getRenderGpu()));
	
	private VkUniform mainUniform;
	private VkUniform lightingUniform;
	
	private VkDescriptorSetLayout uniformLayout;
	private VkDescriptorPool      descriptorPool;
	
	private long lastUpdate;
	
	public boolean commandBuffersDirty=true;
	
	
	private      VulkanRender renderer    =new VulkanRender();
	public final Object       recreateLock=new Object();
	
	public VkCamera camera=new VkCamera();
	
	public RenderQueue renderQueue=new RenderQueue();
	
	public VulkanCore(IDataManager assets, GlfwWindowVk window){
		this.assets=assets;
		this.window=window;
		shaderSource=new ResourceManagerSourced<>(assets.subData("shaders"), VkShaderCode::new);
		modelSource=assets.subData("models");
	}
	
	public void createContext(){
		window.size.register(this::onWindowResize);
		
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
			debugReport=new VkDebugReport(this, asList(
				ERROR
				, WARNING
				, PERFORMANCE_WARNING
			                                          ), asList(
				DEBUG
//				, INFORMATION
			                                                   ));
		}
		
		surface=window.createSurface(this);
		intGpus();
		initModel();
		initUniform();
		initSurfaceDependant();

//		async(this::initModel, this);
	}
	
	int dirLightCount  =1;
	int pointLightCount=1;
	
	
	private void initUniform(){
		
		try(MemoryStack stack=stackPush()){
			int floatSize =Float.SIZE/Byte.SIZE;
			int vec3Size  =floatSize*3;
			int matrixSize=floatSize*16;
			
			mainUniform=new VkUniform(renderGpu, matrixSize*3, this::updateMainUniforms);
			lightingUniform=new VkUniform(renderGpu, DirectionalLight.SIZE_BYTE*dirLightCount+PointLight.SIZE_BYTE*pointLightCount, this::updateLightUniforms);
			
			
			uniformLayout=renderGpu.createDescriptorSetLayout(VERTEX_UNIFORM_BUFF,
			                                                  FRAGMENT_UNIFORM_BUFF,
			                                                  FRAGMENT_IMAGE,
			                                                  FRAGMENT_IMAGE);
			
			VkDescriptor[] poolParts=uniformLayout.getParts();
			poolParts[0]=poolParts[0].withUniform(mainUniform);
			poolParts[1]=poolParts[1].withUniform(lightingUniform);
			
			descriptorPool=renderGpu.createDescriptorPool(models.size(), poolParts);
			
		}
		
	}
	
	private void updateLightUniforms(ByteBuffer byteBuffer){
		
		float tim=(float)(System.currentTimeMillis()%1000000000)/400F;
		
		Vec3f            normal=new Vec3f(1.3F, 2, 2).normalise();
		DirectionalLight sun   =new DirectionalLight(normal, BlackBody.blackBodyColor(new ColorMSolid(), 1700+4000).mul(0.4F));
		PointLight idk=new PointLight(camera.pos.clone().add(0, 0, 0), new ColorMSolid(
			0.2F+SimplexNoise.noise(tim, 0)*0.01F,
			0.3F+SimplexNoise.noise(tim, 0.25F)*0.01F,
			0.5F+SimplexNoise.noise(tim, 0.5F)*0.01F
		), Attenuation.fromRadius(40));
		
		
		sun.put(byteBuffer);
		idk.put(byteBuffer);
		
	}
	
	private void updateMainUniforms(ByteBuffer uniforms){
		
		float farPlane=1000;
		
		Matrix4f model=new Matrix4f();
		Matrix4f view =new Matrix4f();
		
		int floatSize =Float.SIZE/Byte.SIZE;
		int matrixSize=floatSize*16;
		
		model.get(0, uniforms);
		camera.getView().get(matrixSize, uniforms);
		camera.getProjection(surfaceContext.getSwapchain().getSize()).get(matrixSize*2, uniforms);
	}
	
	private void initModel(){
		List<VkMesh> b=ModelLoader.load(renderGpu, modelSource, "bow-g\\bow.obj");
		List<VkMesh> a=ModelLoader.load(renderGpu, modelSource, "crysponza\\untitled.obj");
		List<VkMesh> c=ModelLoader.load(renderGpu, modelSource, "toothless\\toothless.obj");
//		async((Runnable)()->{
		models.addAll(a);
		models.addAll(b);
		models.addAll(c);
//		}, this);
//		async((Runnable)()->models.addAll(a), this);
	}
	
	private void initSurfaceDependant(){
		try{
			surfaceContext=new SurfaceContext(renderGpu, surface, window.size, VK_SAMPLE_COUNT_1_BIT);
		}catch(VkException e){
			if(ZERO_EXTENT_MESSAGE.equals(e.getMessage())) return;
			throw e;
		}
		shaderLookup.clear();
		commandBuffersDirty=true;
	}
	
	private VkShader makeShader(VkMeshFormat format){
		try(MemoryStack stack=stackPush()){
			return surfaceContext.createGraphicsPipeline(
				shaderSource,
				new ShaderState()
					.setCullMode(VkShader.Cull.FRONT)
					.setInput(new VkPipelineInput(format))
				, "test",
				stack.longs(uniformLayout.getHandle()));
		}
	}
	
	private void updateFrameBuffers(){
		if(!commandBuffersDirty) return;
		commandBuffersDirty=false;
//		try{
		surfaceContext.updateCommandBuffers((frame, sceneBuffer)->{
			
			renderQueue.processor=(shader, meshes)->{
				
				shader.bind(sceneBuffer);
				
				for(VkMesh model : models){
					
					shader.bindDescriptorSets(sceneBuffer, model.getDescriptor(descriptorPool, uniformLayout, texturePool));
					
					model.bind(sceneBuffer);
					model.render(sceneBuffer);
					
				}
				
			};
			
			surfaceContext.getRenderPass().begin(sceneBuffer, frame.getFrameBuffer(), surfaceContext.getSwapchain().getExtent(), clearColor);
			
			try(var e=renderQueue){
				for(VkMesh model : models){
					e.add(shaderLookup.computeIfAbsent(model.getFormat(), this::makeShader), model);
				}
			}
//				LogUtil.println(shaderLookup);
			
			sceneBuffer.endRenderPass();
		});
//		}catch(VkException.OutOfDeviceMemory e){
//			LogUtil.println("Growing descriptor pool");
//			commandBuffersDirty=true;
//			recreateUniforms();
//		}
	}
	
	private void validateList(List<String> supported, List<String> requested, String type){
		List<String> fails=requested.stream()
		                            .filter(l->!supported.contains(l))
		                            .map(s->s==null?"<NULL_STRING>":s.isEmpty()?"<EMPTY_STRING>":s)
		                            .collect(Collectors.toList());
		
		if(!fails.isEmpty()){
			throw new IllegalStateException(TextUtil.plural(type, fails.size())+" not supported: "+String.join(", ", fails));
		}
	}
	
	private void initAddons(MemoryStack stack, List<String> layerNames, List<String> extensionNames){
		
		if(DEV_ON){
			ValidationLayers.addLayers(layerNames);
			
			extensionNames.add(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
			
		}
		validateList(Vk.enumerateInstanceLayerProperties(stack).stream().map(VkLayerProperties::layerNameString).collect(Collectors.toList()), layerNames, "Layer");
		
		PointerBuffer requ=glfwGetRequiredInstanceExtensions();
		if(requ==null) throw new IllegalStateException("GLFW failed to deliver required Vulkan instance extensions. Please try to reinstall or update your graphics driver!");
		
		Stream.generate(requ::getStringUTF8).limit(requ.limit()).forEach(extensionNames::add);
		
		validateList(Vk.enumerateInstanceExtensionProperties(stack).stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toList()), extensionNames, "Extension");
		
	}
	
	private void intGpus(){
		try(GpuInitializer init=new GpuInitializer(this)){
			renderGpu=init.find(asList(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
		}
	}
	
	private void destroySurfaceDependant(){
		if(surfaceContext==null) return;
		
		renderGpu.waitIdle();
		destroyShaders();
		surfaceContext.destroy();
		surfaceContext=null;
	}
	
	private void destroyShaders(){
		shaderLookup.values().forEach(VkShader::destroy);
		shaderLookup.clear();
	}
	
	@Override
	public void destroy(){
		renderGpu.waitIdle();
		
		destroySurfaceDependant();
		
		surface.destroy();
		surface=null;
		
		models.forEach(VkMesh::destroy);
		models.clear();
		
		texturePool.destroy();
		
		destroyUniforms();
		
		descriptorPool.destroy();
		descriptorPool=null;
		
		renderGpu.destroy();
		renderGpu=null;
		
		renderer.destroy();
		renderer=null;
		
		if(DEV_ON){
			debugReport.destroy();
		}
		vkDestroyInstance(instance, null);
	}
	
	private void destroyUniforms(){
		
		uniformLayout.destroy();
		uniformLayout=null;
		mainUniform.destroy();
		mainUniform=null;
		lightingUniform.destroy();
		lightingUniform=null;
	}
	
	private void recreateUniforms(){
		getRenderGpu().waitIdle();
		destroyUniforms();
		initUniform();
	}
	
	private void onWindowResize(IVec2iR size){
		recreateSurfaceDependant();
	}
	
	private void recreateSurfaceDependant(){
		synchronized(recreateLock){
			renderGpu.waitIdle();
			destroySurfaceDependant();
			initSurfaceDependant();
		}
	}
	
	public void render(){
		if(surfaceContext==null) return;
		synchronized(toExecute){
			while(!toExecute.isEmpty()){
				toExecute.pop().run();
			}
		}
		
		synchronized(recreateLock){
			
			
			updateFrameBuffers();
			
			mainUniform.updateBuffer();
			lightingUniform.updateBuffer();
			
			VkSwapchain.Frame frame=surfaceContext.getSwapchain().acquireNextFrame();
			
			if(frame==null||renderer.render(surfaceContext, frame)){
				recreateSurfaceDependant();
			}
			
			long tim=System.currentTimeMillis();
			if(lastUpdate<tim){
				lastUpdate=tim+1000;
				
				//use count to force call on all shaders
				if(shaderLookup.values().stream().filter(VkShader::updateCode).count()>0){
					commandBuffersDirty=true;
					updateFrameBuffers();
				}
			}
		}
	}
	
	public VkInstance getInstance(){
		return instance;
	}
	
	public VkGpu getRenderGpu(){
		return renderGpu;
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
	
	
}
