package com.lapissea.vulkanimpl;

import com.lapissea.vulkanimpl.renderer.model.VkMesh;
import com.lapissea.vulkanimpl.shaders.VkShader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderQueue implements AutoCloseable{
	
	public interface RenderBatchProcessor{
		void process(VkShader shader, Iterable<VkMesh> data);
	}
	
	private class ShaderGroup{
		final List<VkMesh> queue=new ArrayList<>();
		int timeout;
		
		boolean render(VkShader s){
			if(queue.isEmpty()&&--timeout<0) return true;
			timeout=timeoutCooldown;
			
			processor.process(s, queue);
			return false;
		}
	}
	
	private final Map<VkShader, ShaderGroup> queue=new HashMap<>();
	public        RenderBatchProcessor       processor;
	
	public int timeoutCooldown=30;
	
	public void add(VkShader shader, VkMesh mesh){
		queue.computeIfAbsent(shader, this::newShaderNode).queue.add(mesh);
	}
	
	private ShaderGroup newShaderNode(VkShader s){
		return new ShaderGroup();
	}
	
	@Override
	public void close(){
		queue.entrySet().removeIf(e->{
			var s=e.getKey();
			var g=e.getValue();
			
			return g.render(s);
		});
	}
}
