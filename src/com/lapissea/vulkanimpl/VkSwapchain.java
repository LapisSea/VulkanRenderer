package com.lapissea.vulkanimpl;

import com.lapissea.util.ArrayViewList;
import com.lapissea.util.MathUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.exceptions.VkException;
import com.lapissea.vulkanimpl.util.DevelopmentInfo;
import com.lapissea.vulkanimpl.util.VkDestroyable;
import com.lapissea.vulkanimpl.util.VkGpuCtx;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import com.lapissea.vulkanimpl.util.types.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain implements VkDestroyable, VkGpuCtx{
	
	public static final String ZERO_EXTENT_MESSAGE="Extent is 0/0";
	
	public class Frame{
		private      long        frameBuffer;
		private      VkTexture   colorFrame;
		private      VkSemaphore renderFinish;
		public final int         index;
		
		public Frame(int index, VkTexture colorFrame, VkSemaphore renderFinish){
			this.index=index;
			this.colorFrame=colorFrame;
			this.renderFinish=renderFinish;
		}
		
		public void destroy(){
			vkDestroyFramebuffer(getDevice(), frameBuffer, null);
			colorFrame.destroy();
			renderFinish.destroy();
		}
		
		public VkSemaphore getRenderFinish(){
			return renderFinish;
		}
		
		public long getFrameBuffer(){
			return frameBuffer;
		}
	}
	
	private       LongBuffer  handle;
	private final VkGpu       gpu;
	private       int         colorFormat;
	private       int         colorSpace;
	private       VkSemaphore imageAviable;
	private       List<Frame> frames;
	private       VkTexture   depth;
	private       VkTexture   color;
	
	private final VkExtent2D extent    =VkExtent2D.calloc();
	private final IVec2iR    extentView=new IVec2iR(){
		@Override
		public int x(){ return extent.width(); }
		
		@Override
		public int y(){ return extent.height(); }
	};
	
	private IntBuffer acquireNextImageMem=memAllocInt(1);
	
	private       VkCommandBufferM[] frameBinds;
	private final int                samples;
	
	public VkSwapchain(VkGpu gpu, VkSurface surface, IVec2iR size, int samples){
		this.gpu=gpu;
		this.samples=samples;
		try(MemoryStack stack=stackPush()){
			create(gpu, surface, size, stack);
		}
		imageAviable=gpu.createSemaphore();
	}
	
	private void create(VkGpu gpu, VkSurface surface, IVec2iR size, MemoryStack stack){
		//acquire currency
		IntBuffer count=stack.mallocInt(1);
		Vk.getPhysicalDeviceSurfacePresentModesKHR(gpu.getPhysicalDevice(), surface, count, null);
		IntBuffer presentModes=memAllocInt(count.get(0));
		
		Vk.getPhysicalDeviceSurfacePresentModesKHR(gpu.getPhysicalDevice(), surface, count, presentModes);
		
		VkSurfaceCapabilitiesKHR caps=VkSurfaceCapabilitiesKHR.callocStack(stack);
		Vk.getPhysicalDeviceSurfaceCapabilitiesKHR(gpu, surface, caps);
		
		//regard females
		int                presentMode=choosePresentMode(presentModes);
		VkSurfaceFormatKHR format     =chooseSwapSurfaceFormat(Vk.getPhysicalDeviceSurfaceFormatsKHR(gpu, stack));
		chooseSwapExtent(size, stack, surface.getCapabilities(gpu, stack));
		
		if(extentView.isZero()){
			extent.free();
			memFree(acquireNextImageMem);
			throw new VkException(ZERO_EXTENT_MESSAGE);
		}
		
		int imageCount=caps.minImageCount()+1;
		if(caps.maxImageCount()>0) imageCount=Math.min(imageCount, caps.maxImageCount());
		
		
		this.colorFormat=format.format();
		colorSpace=format.colorSpace();
		
		VkSwapchainCreateInfoKHR createInfo=VkSwapchainCreateInfoKHR.callocStack(stack);
		createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
		          .surface(surface.handle)
		          .minImageCount(imageCount)
		          .imageFormat(colorFormat)
		          .imageColorSpace(colorSpace)
		          .imageExtent(extent)
		          .imageArrayLayers(1)
		          .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
		
		int graphics=gpu.getGraphicsQueue().id, present=gpu.getSurfaceQueue().id;
		
		if(graphics!=present){
			createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
			          .pQueueFamilyIndices(stack.ints(graphics, present));
		}else{
			createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
		}
		
		createInfo.preTransform(caps.currentTransform())
		          .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
		          .presentMode(presentMode)
		          .clipped(true)
		          .oldSwapchain(VK_NULL_HANDLE);
		
		handle=memAllocLong(1);
		Vk.createSwapchainKHR(gpu, createInfo, handle);
		
		VkTexture[] colors =UtilL.convert(Vk.getSwapchainImagesKHR(gpu, this, stack), VkTexture[]::new, image->new VkTexture(image, null).createView(VkImageAspect.COLOR));
		Frame[]     frames0=new Frame[colors.length];
		for(int i=0;i<colors.length;i++){
			frames0[i]=new Frame(i, colors[i], gpu.createSemaphore());
		}
		frames=ArrayViewList.create(frames0, null);
	}
	
	
	public void initFrameBuffers(VkRenderPass renderPass){
		
		int tiling=VK_IMAGE_TILING_OPTIMAL;
		
		VkImage        img=getGpu().create2DImage(extent, findDepthFormat(tiling), tiling, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, samples);
		VkDeviceMemory mem=img.createMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		
		img.transitionLayout(getGpu().getGraphicsQueue(), VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, VkImage.TOP_TO_TRANSFER_WRITE);
		
		depth=new VkTexture(img, mem);
		depth.createView(VkImageAspect.DEPTH);
		
//		img=getGpu().create2DImage(extent, colorFormat, tiling, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, samples);
//		mem=img.createMemory(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
//
//		img.transitionLayout(getGpu().getGraphicsQueue(), VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//
//		color=new VkTexture(img, mem);
//		color.createView(VkImageAspect.COLOR);
		
		initFrames(renderPass);
	}
	
	private void initFrames(VkRenderPass renderPass){
		
		try(MemoryStack stack=stackPush()){
//			LongBuffer attach=stack.longs(color.getView(), 0, depth.getView());
			LongBuffer attach=stack.longs(0, depth.getView());
			
			VkFramebufferCreateInfo framebufferInfo=VkFramebufferCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
			
			framebufferInfo.renderPass(renderPass.getHandle())
			               .pAttachments(attach)
			               .width(extent.width())
			               .height(extent.height())
			               .layers(1);
			
			
			for(Frame frame : frames){
				attach.put(0, frame.colorFrame.getView());
				frame.frameBuffer=Vk.createFrameBuffer(gpu, framebufferInfo, stack.mallocLong(1));
			}
		}
		
	}
	
	private int findDepthFormat(int tiling){
		return findDepthFormat(tiling, VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
		                       VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT);
	}
	
	private int findDepthFormat(int tiling, int features, int... acceptedFormats){
		return getGpu().supportedFormats()
		               .filter(f->UtilL.contains(acceptedFormats, f.format.handle)&&f.checkFeatures(tiling, features))
		               .map(f->f.format.handle)
		               .findFirst()
		               .orElseThrow(()->new UnsupportedOperationException("Unable to find supported depth colorFormat"));
	}
	
	private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats){
		if(availableFormats.capacity()==1&&availableFormats.position(0).format()==VK_FORMAT_UNDEFINED) return availableFormats.get(0);
		
		for(int i=0;i<availableFormats.capacity();i++){
			VkSurfaceFormatKHR format=availableFormats.get(i);
			
			int f=format.format();
			if((f==VK_FORMAT_B8G8R8A8_UNORM||f==VK_FORMAT_UNDEFINED)&&format.colorSpace()==VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) return format;
		}
		
		return availableFormats.get(0);
	}
	
	private void chooseSwapExtent(IVec2iR size, MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities){
		if(capabilities.currentExtent().width()==0xFFFFFFFF){
			extent.set(capabilities.currentExtent());
			return;
		}
		
		VkExtent2D min=capabilities.minImageExtent(), max=capabilities.maxImageExtent();
		
		extent.set(MathUtil.snap(size.x(), min.width(), max.width()), MathUtil.snap(size.y(), min.height(), max.height()));
	}
	
	private int choosePresentMode(IntBuffer availablePresentModes){
		
		int[] ids=new int[availablePresentModes.capacity()];
		for(int i=0;i<availablePresentModes.capacity();i++) ids[i]=availablePresentModes.get(i);
		VulkanCore.Settings settings=gpu.getInstance().getSettings();
		
		if(settings.tripleBufferingEnabled.get()&&UtilL.contains(ids, VK_PRESENT_MODE_MAILBOX_KHR)) return VK_PRESENT_MODE_MAILBOX_KHR;
		if(settings.vSyncEnabled.get()&&UtilL.contains(ids, VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;
		return VK_PRESENT_MODE_IMMEDIATE_KHR;
	}
	
	public Frame acquireNextFrame(){
		int id=Vk.acquireNextImageKHR(getDevice(), this, imageAviable.getHandle(), VK_NULL_HANDLE, acquireNextImageMem);
		return id==-1?null:frames.get(id);
	}
	
	public VkRenderPass createRenderPass(){
		
		
		try(MemoryStack stack=stackPush()){
			VkAttachmentDescription.Buffer attachments=VkAttachmentDescription.callocStack(2, stack);
			attachments.get(0)//color attachment
			           .format(getColorFormat())
			           .samples(samples)
			           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
			           .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
			           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
			           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			           .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
			attachments.get(1)//depth attachment
			           .format(findDepthFormat(VK_IMAGE_TILING_OPTIMAL))
			           .samples(samples)
			           .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
			           .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
			           .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
			           .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
			           .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
			
			VkAttachmentReference.Buffer colorAttachmentRef=VkAttachmentReference.callocStack(1, stack);
			colorAttachmentRef.get(0) //fragment out color
			                  .attachment(0)
			                  .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
			
			VkAttachmentReference depthAttachmentRef=VkAttachmentReference.callocStack(stack);
			depthAttachmentRef.attachment(1)
			                  .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
			
			VkSubpassDescription.Buffer subpasses=VkSubpassDescription.callocStack(1, stack);
			subpasses.get(0)
			         .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
			         .colorAttachmentCount(colorAttachmentRef.limit())
			         .pColorAttachments(colorAttachmentRef)
			         .pDepthStencilAttachment(depthAttachmentRef);
			
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
			
			return Vk.createRenderPass(getGpu(), renderPassInfo, stack.mallocLong(1));
		}
	}
	
	@Override
	public void destroy(){
		if(DevelopmentInfo.DEV_ON&&handle==null) throw new IllegalStateException("Swapchain already destroyed");
		
		memFree(acquireNextImageMem);
		imageAviable.destroy();
		frames.forEach(Frame::destroy);
		depth.destroy();
		vkDestroySwapchainKHR(gpu.getDevice(), handle.get(0), null);
		
		handle=null;
	}
	
	public long getHandle(){
		return handle.get(0);
	}
	
	public LongBuffer getBuffer(){
		return handle;
	}
	
	public int getColorFormat(){
		return colorFormat;
	}
	
	
	public List<Frame> getFrames(){
		return frames;
	}
	
	@Override
	public VkGpu getGpu(){
		return gpu;
	}
	
	public VkSemaphore getImageAviable(){
		return imageAviable;
	}
	
	public VkExtent2D getExtent(){
		return extent;
	}
	
	public IVec2iR getSize(){
		return extentView;
	}
}
