package com.lapissea.vulkanimpl.util;

import com.lapissea.vulkanimpl.Vk;

import java.util.Objects;

public class VkAutoDestroyable implements AutoCloseable{
	
	private final VkDestroyable[] destroyables;
	
	public VkAutoDestroyable(VkDestroyable... destroyables){
		this.destroyables=destroyables;
		if(Vk.DEBUG) Objects.requireNonNull(destroyables);
	}
	
	@Override
	public void close(){
		for(VkDestroyable destroyable : destroyables){
			destroyable.destroy();
		}
	}
}
