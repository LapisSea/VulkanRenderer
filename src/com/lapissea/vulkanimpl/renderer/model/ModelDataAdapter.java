package com.lapissea.vulkanimpl.renderer.model;

import com.lapissea.vec.color.ColorM;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public abstract class ModelDataAdapter<T>{
	
	static{
		new ModelDataAdapter<ColorM>(){
			
			@Override
			public void put(int flags, ColorM instance){
			
			}
			
			@Override
			public int getByteSize(int flags){
				return 0;
			}
		};
	}
	
	private ByteBuffer  dest;
	private FloatBuffer floatForm;
	private FloatBuffer intForm;
	
	
	public void setDest(ByteBuffer dest){
		this.dest=dest;
	}
	
	public FloatBuffer floatForm(){
		if(floatForm==null) floatForm=dest.asFloatBuffer();
		return floatForm;
	}
	
	public ByteBuffer byteForm(){
		return dest;
	}
	
	public FloatBuffer intForm(){
		if(intForm==null) intForm=dest.asFloatBuffer();
		return intForm;
	}
	
	public abstract void put(int flags, T instance);
	
	public abstract int getByteSize(int flags);
	
}
