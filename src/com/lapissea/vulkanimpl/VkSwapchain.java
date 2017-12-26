package com.lapissea.vulkanimpl;

import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;
import com.lapissea.vec.Vec2iFinal;
import com.lapissea.vec.color.ColorM;
import com.lapissea.vec.interf.IVec2iR;
import com.lapissea.vulkanimpl.model.VkModel;
import com.lapissea.vulkanimpl.simplevktypes.*;
import com.lapissea.vulkanimpl.util.VkImageAspect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

import static com.lapissea.vulkanimpl.BufferUtil.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkSwapchain{
	
	
	public VkDevice getDevice(){
		return device;
	}
	
	public int getFormat(){
		return format;
	}
	
	public IVec2iR getImageSize(){
		return imageSize;
	}
	
	public VkImageTexture getDepth(){
		return depth;
	}
	
	
	public class Buffer{
		private VkImage         image;
		private VkImageView     view;
		private VkCommandBuffer cmd;
		private boolean deleted=false;
		
		public Buffer(VkImage image, VkImageView view){
			this.image=image;
			this.view=view;
		}
		
		private void check(){
			if(deleted) throw new IllegalStateException();
		}
		
		public void destroy(VkDevice device){
			check();
			view.destroy(device);
			view=null;
			cmd=null;
			deleted=true;
			
		}
		
		public VkImage getImage(){
			check();
			return image;
		}
		
		public VkImageView getView(){
			check();
			return view;
		}
		
		public VkCommandBuffer getCmd(){
			check();
			return cmd;
		}
	}
	
	private long            id;
	private Buffer[]        buffers;
	private VkDevice        device;
	private int             format;
	private VkFramebuffer[] frameBuffers;
	private boolean         destroyed;
	private IVec2iR         imageSize;
	private VkImageTexture  depth;
	
	private void check(){
		if(destroyed) throw new IllegalStateException();
	}
	
	public void create(VkGpu gpu){
		check();
		
		VkSurfaceCapabilitiesKHR caps       =gpu.getSurfaceCapabilities();
		VkSurfaceFormatKHR       format     =chooseSwapSurfaceFormat(gpu.getFormats());
		int                      presentMode=choosePresentMode(gpu.getPresentModes());
		int                      imageCount =caps.minImageCount()+1;
		
		try(MemoryStack stack=stackPush()){
			VkExtent2D extent=chooseSwapExtent(gpu.getWindow(), stack, gpu.getSurfaceCapabilities());
			
			imageSize=new Vec2iFinal(extent.width(), extent.height());
			
			if(caps.maxImageCount()>0&&imageCount>caps.maxImageCount()) imageCount=caps.maxImageCount();
			
			
			VkSwapchainCreateInfoKHR swapchainInfo=VkSwapchainCreateInfoKHR.callocStack(stack);
			swapchainInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
			             .surface(gpu.getWindow().getSurface())
			             .minImageCount(imageCount)
			             .imageFormat(format.format()==0?VK_FORMAT_B8G8R8A8_UNORM:format.format())
			             .imageColorSpace(format.colorSpace())
			             .imageExtent(extent)
			             .imageArrayLayers(1)
			             .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
			             .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
			             .presentMode(presentMode)
			             .clipped(true)
			             .oldSwapchain(id);
			
			if((caps.supportedTransforms()&VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)!=0) swapchainInfo.preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
			else swapchainInfo.preTransform(caps.currentTransform());
			
			if(gpu.getQueueGraphicsId()!=gpu.getQueueSurfaceId()){
				swapchainInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
				IntBuffer ib=stack.callocInt(2);
				ib.put(0, gpu.getQueueGraphicsId());
				ib.put(1, gpu.getQueueSurfaceId());
				swapchainInfo.pQueueFamilyIndices(ib);
			}else{
				swapchainInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
				swapchainInfo.pQueueFamilyIndices(null);
			}
			
			this.device=gpu.getDevice();
			this.format=swapchainInfo.imageFormat();
			
			id=Vk.createSwapchainKHR(device, swapchainInfo, stack.callocLong(1));
			
			IntBuffer ib=stack.callocInt(1);
			imageCount=Vk.getSwapchainImagesKHR(device, this, ib);
			
			LongBuffer images=MemoryUtil.memAllocLong(imageCount);
			Vk.getSwapchainImagesKHR(device, this, ib, images);
			
			VkImageViewCreateInfo createInfo=VkImageViewCreateInfo.callocStack(stack);
			createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
			          .viewType(VK_IMAGE_VIEW_TYPE_2D)
			          .format(swapchainInfo.imageFormat());
			
			VkImageSubresourceRange subresourceRange=createInfo.subresourceRange();
			subresourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
			                .baseMipLevel(0)
			                .levelCount(1)
			                .baseArrayLayer(0)
			                .layerCount(1);
			
			VkComponentMapping comps=createInfo.components();
			comps.r(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .g(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .b(VK_COMPONENT_SWIZZLE_IDENTITY)
			     .a(VK_COMPONENT_SWIZZLE_IDENTITY);
			
			
			buffers=new Buffer[imageCount];
			LongBuffer lb=stack.callocLong(1);
			for(int i=0;i<buffers.length;i++){
				long image=images.get(i);
				createInfo.image(image);
				buffers[i]=new Buffer(new VkImage(image, imageSize.x(), imageSize.y(), createInfo.format()), Vk.createImageView(device, createInfo, lb));
			}
			VkGpu.Feature feature    =VkGpu.Feature.OPTIMAL;
			int           depthFormat=gpu.anyFormat(feature, VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT);
			if(depthFormat==-1) UtilL.exitWithErrorMsg("Unsupported depth format");
//			LogUtil.println(depthFormat);
//			System.exit(0);
			depth=gpu.createImageTexture(Vk.imageCreateInfo(stack, imageSize.x(), imageSize.y(), depthFormat, feature, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
			depth.image.transitionImageLayout(gpu, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			depth.init(gpu, VkImageAspect.DEPTH);
		}
	}
	
	public void initCommandBuffers(MemoryStack stack, VkCommandPool commandPool, VkRenderPass renderPass, Shader shader, VkModel model, VkViewport.Buffer viewport){
		check();
		if(Vk.DEBUG){
			Objects.requireNonNull(stack);
			Objects.requireNonNull(commandPool);
			Objects.requireNonNull(renderPass);
			Objects.requireNonNull(shader);
		}
		if(frameBuffers==null) initFrameBuffers(stack, renderPass);
		
		int count=getBufferCount();
		
		
		for(int i=0;i<count;i++){
			
			VkCommandBufferAllocateInfo allocInfo=VkCommandBufferAllocateInfo.callocStack(stack);
			allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
			         .commandPool(commandPool.get())
			         .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
			         .commandBufferCount(1);
			
			buffers[i].cmd=Vk.allocateCommandBuffers(device, allocInfo, stack.callocPointer(1))[0];
			
			VkCommandBufferBeginInfo beginInfo=VkCommandBufferBeginInfo.callocStack(stack);
			beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
			         .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
			         .pInheritanceInfo(null)
			         .pNext(NULL);
			
			VkCommandBuffer cmd=buffers[i].cmd;
			Vk.beginCommandBuffer(cmd, beginInfo);
			
			VkRenderPassBeginInfo renderPassInfo=VkRenderPassBeginInfo.callocStack(stack);
			renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
			              .pNext(NULL)
			              .renderPass(renderPass.get())
			              .pClearValues(Vk.clearDepth(Vk.clearCol(new ColorM(0x2F91E0), VkClearValue.callocStack(1, stack))))
			              .framebuffer(frameBuffers[i].get());
			VkRect2D renderArea=renderPassInfo.renderArea();
			renderArea.offset().set(0, 0);
			renderArea.extent().set(imageSize.x(), imageSize.y());
			
			VkRect2D.Buffer scissor=VkRect2D.callocStack(1);
			scissor.extent().set((int)viewport.width(), (int)viewport.height());
			scissor.offset().set(0, 0);
			
			vkCmdBeginRenderPass(cmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
			vkCmdSetViewport(cmd, 0, viewport);
			vkCmdSetScissor(cmd, 0, scissor);
			
			vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, shader.getGraphicsPipeline().get());
			
			vkCmdBindVertexBuffers(cmd, 0, buffSingle(stack, model.getMemory().getBuffer()), buffSingle(stack, 0L));
			vkCmdBindIndexBuffer(cmd, model.getMemory().getBuffer().get(), model.getDataSize(), model.getIndexFormat());
			vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, shader.getPipelineLayout().get(), 0, buffSingle(stack, shader.getDescriptorSet()), null);
			
			vkCmdDrawIndexed(cmd, model.getVertexCount(), 1, 0, 0, 0);
			
			vkCmdEndRenderPass(cmd);
			Vk.endCommandBuffer(cmd);
		}
	}
	
	public void initFrameBuffers(MemoryStack stack, VkRenderPass renderPass){
		check();
		frameBuffers=new VkFramebuffer[buffers.length];
		LongBuffer lb=stack.callocLong(2);
		
		VkFramebufferCreateInfo framebufferInfo=VkFramebufferCreateInfo.callocStack(stack);
		framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
		               .renderPass(renderPass.get())
		               .width(imageSize.x())
		               .height(imageSize.y())
		               .layers(1);
		
		lb.put(1, depth.getView().get());
		
		for(int i=0;i<getBufferCount();i++){
			
			framebufferInfo.pAttachments(lb.put(0, buffers[i].getView().get()));
			
			frameBuffers[i]=Vk.createFrameBuffer(device, framebufferInfo, stack.callocLong(1));
		}
		
	}
	
	public void destroy(){
		check();
		depth.destroy(device);
		UtilL.forEach(frameBuffers, t->t.destroy(device));
		frameBuffers=null;
		
		if(buffers!=null){
			for(Buffer b : buffers) b.destroy(device);
			buffers=null;
		}
		Vk.destroySwapchainKHR(device, this);
		device=null;
		
		destroyed=true;
	}
	
	public Buffer getBuffer(int index){
		check();
		return buffers[index];
	}
	
	public int getBufferCount(){
		check();
		return buffers.length;
	}
	
	private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats){
		check();
		if(availableFormats.capacity()==1&&availableFormats.position(0).format()==VK_FORMAT_UNDEFINED) return availableFormats.get(0);
		
		for(int i=0;i<availableFormats.capacity();i++){
			VkSurfaceFormatKHR format=availableFormats.get(i);
			
			int f=format.format();
			if((f==VK_FORMAT_B8G8R8A8_UNORM||f==VK_FORMAT_UNDEFINED)&&format.colorSpace()==VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) return format;
		}
		
		return availableFormats.get(0);
	}
	
	private VkExtent2D chooseSwapExtent(GlfwWindow window, MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities){
		if(capabilities.currentExtent().width()==0xFFFFFFFF) return capabilities.currentExtent();
		
		VkExtent2D min=capabilities.minImageExtent(), max=capabilities.maxImageExtent();

//		return VkExtent2D.callocStack(stack).set(MathUtil.snap(window.getSize().x(), min.width(), max.width()), MathUtil.snap(window.getSize().y(), min.height(), max.height()));
		return VkExtent2D.callocStack(stack).set(window.getSize().x(), window.getSize().y());
	}
	
	private int choosePresentMode(IntBuffer availablePresentModes){
		int[] ids=new int[availablePresentModes.capacity()];
		for(int i=0;i<availablePresentModes.capacity();i++) ids[i]=availablePresentModes.get(i);

//		if(UtilL.contains(ids, VK_PRESENT_MODE_MAILBOX_KHR)) return KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;//triple buffering
		if(UtilL.contains(ids, VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;//v-sync
		return VK_PRESENT_MODE_IMMEDIATE_KHR;//tearing
	}
	
	public long getId(){
		check();
		return id;
	}
}
