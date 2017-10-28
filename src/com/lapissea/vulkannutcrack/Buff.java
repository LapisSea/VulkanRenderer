package com.lapissea.vulkannutcrack;

import com.lapissea.util.UtilL;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Stack;

public abstract class Buff<T>{
	
	
	public static final Buff<IntBuffer>     i=new Buff<IntBuffer>(){
		@Override
		protected IntBuffer create(int size){
			return MemoryUtil.memAllocInt(size);
		}
		
		@Override
		protected int size(IntBuffer buff){
			return buff.capacity();
		}
	};
	public static final Buff<LongBuffer>    l=new Buff<LongBuffer>(){
		@Override
		protected LongBuffer create(int size){
			return MemoryUtil.memAllocLong(size);
		}
		
		@Override
		protected int size(LongBuffer buff){
			return buff.capacity();
		}
	};
	public static final Buff<PointerBuffer> p=new Buff<PointerBuffer>(){
		@Override
		protected PointerBuffer create(int size){
			return MemoryUtil.memAllocPointer(size);
		}
		
		@Override
		protected int size(PointerBuffer buff){
			return buff.capacity();
		}
	};
	public static final Buff<FloatBuffer>   f=new Buff<FloatBuffer>(){
		@Override
		protected FloatBuffer create(int size){
			return MemoryUtil.memAllocFloat(size);
		}
		
		@Override
		protected int size(FloatBuffer buff){
			return buff.capacity();
		}
	};
	
	private class Node implements Comparable<Node>{
		final Stack<T> t=new Stack<>();
		final int size;
		
		Node(int size){
			this.size=size;
		}
		
		public void push(T t1){
			t.push(t1);
		}
		
		public T pop(){
			return t.empty()?create(size):t.pop();
		}
		
		@Override
		public int compareTo(Node o){
			return Integer.compare(size, o.size);
		}
	}
	
	private Node[] data=UtilL.array(Node.class, 0);
	
	private void grow(int size){
		data=Arrays.copyOf(data, data.length+1);
		data[data.length-1]=new Node(size);
		Arrays.sort(data);
	}
	
	private int find(int size){
		if(data.length==0){
			grow(size);
			return 0;
		}
		
		int low=0, high=data.length-1;
		while(low<=high){
			int  mid   =(low+high)>>>1;
			Node midVal=data[mid];
			int  cmp   =Integer.compare(midVal.size, size);
			if(cmp<0) low=mid+1;
			else if(cmp>0) high=mid-1;
			else return mid;
		}
		
		grow(size);
		return find(size);
	}
	
	protected abstract T create(int size);
	
	protected abstract int size(T t);
	
	public synchronized T get(int size){
		int pos=find(size);
		return data[pos].pop();
	}
	
	public synchronized void done(T b){
		int pos=find(size(b));
		data[pos].push(b);
		
	}
	
	
}
