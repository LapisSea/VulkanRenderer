package com.lapissea.vulkanimpl.assets;

import com.lapissea.datamanager.DataSignature;
import com.lapissea.datamanager.IDataManager;
import com.lapissea.util.NotNull;
import com.lapissea.vulkanimpl.util.VkDestroyable;

import java.util.HashMap;
import java.util.function.Function;

public class ResourcePool<T extends VkDestroyable> implements VkDestroyable{
	
	public abstract static class Node<T> extends DataSignature implements VkDestroyable{
		
		public Node(@NotNull String path, @NotNull IDataManager source){
			super(path, source);
		}
		public Node(@NotNull DataSignature signature){
			super(signature.path, signature.source);
		}
		
		protected abstract T get();
		
		protected abstract boolean hasData();
		
	}
	
	private final HashMap<DataSignature, Node<T>> loaded=new HashMap<>();
	
	@NotNull
	private final Function<DataSignature, Node<T>> impl;
	
	public ResourcePool(@NotNull Function<DataSignature, Node<T>> impl){
		this.impl=impl;
	}
	
	public T get(DataSignature signature){
		
		Node<T> n;
		synchronized(loaded){
			n=loaded.get(signature);
			if(n==null)n=tryGetNew(signature);
		}
		return n==null?null:n.get();
	}
	
	private Node<T> tryGetNew(DataSignature signature){
		Node<T> n=impl.apply(signature);
		
		synchronized(loaded){
			loaded.put(signature, n);
		}
		
		if(n.hasData()){
			return n;
		}
		
		synchronized(loaded){
			loaded.remove(n);
		}
		n.destroy();
		return null;
	}
	
	@Override
	public void destroy(){
		synchronized(loaded){
			loaded.values().forEach(VkDestroyable::destroy);
			loaded.clear();
		}
	}
}
