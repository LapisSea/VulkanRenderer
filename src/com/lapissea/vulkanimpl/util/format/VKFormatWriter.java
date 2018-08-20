package com.lapissea.vulkanimpl.util.format;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.lapissea.vulkanimpl.util.format.VkFormat.StorageType.*;

public abstract class VKFormatWriter{
	
	public abstract static class F extends VKFormatWriter{
		public abstract void write(float f1);
	}
	
	public abstract static class FF extends VKFormatWriter{
		public abstract void write(float f1, float f2);
	}
	
	public abstract static class FFF extends VKFormatWriter{
		public abstract void write(float f1, float f2, float f3);
	}
	
	public abstract static class FFFF extends VKFormatWriter{
		public abstract void write(float f1, float f2, float f3, float f4);
	}
	
	public abstract static class I extends VKFormatWriter{
		public abstract void write(int i);
	}
	
	private static final List<VKFormatWriter> WRITERS=new ArrayList<>();
	
	static{
		abstract class ISiz extends I{
			final int size;
			
			ISiz(int size){this.size=size;}
			
			@Override
			public boolean canWriteAs(VkFormat info){
				if(info.totalByteSize!=size) return false;
				if(info.packSizes.size()==1&&info.components.get(0).storageType==INT) return true;
				return info.components.size()==1&&info.components.get(0).storageType==INT;
			}
		}
		
		WRITERS.add(new ISiz(4){
			@Override
			public void write(int i){
				dest.putInt(i);
			}
		});
		WRITERS.add(new ISiz(2){
			@Override
			public void write(int i){
				dest.putShort((short)(i&0xFFFF));
			}
		});
		
		WRITERS.add(new F(){
			@Override
			public boolean canWriteAs(VkFormat info){
				if(info.totalByteSize!=4) return false;
				if(info.packSizes.size()==1&&info.components.get(0).storageType==FLOAT) return true;
				return info.components.size()==1&&info.components.get(0).storageType==FLOAT;
			}
			
			@Override
			public void write(float f){
				dest.putFloat(f);
			}
		});
		
		WRITERS.add(new FF(){
			@Override
			public boolean canWriteAs(VkFormat info){
				if(info.totalByteSize!=8) return false;
				if(info.packSizes.size()==1&&info.components.get(0).storageType==FLOAT) return true;
				return info.components.size()==2&&info.components.get(0).storageType==FLOAT;
			}
			
			@Override
			public void write(float f, float f1){
				dest.putFloat(f).putFloat(f1);
			}
		});
		
		WRITERS.add(new FFF(){
			@Override
			public boolean canWriteAs(VkFormat info){
				if(info.totalByteSize!=12) return false;
				if(info.packSizes.size()==1&&info.components.get(0).storageType==FLOAT) return true;
				return info.components.size()==3&&info.components.get(0).storageType==FLOAT;
			}
			
			@Override
			public void write(float f, float f1, float f2){
				dest.putFloat(f).putFloat(f1).putFloat(f2);
			}
		});
		
		WRITERS.add(new FFFF(){
			@Override
			public boolean canWriteAs(VkFormat info){
				if(info.totalByteSize!=16) return false;
				if(info.packSizes.size()==1&&info.components.get(0).storageType==FLOAT) return true;
				return info.components.size()==4&&info.components.get(0).storageType==FLOAT;
			}
			
			@Override
			public void write(float f, float f1, float f2, float f3){
				dest.putFloat(f).putFloat(f1).putFloat(f2).putFloat(f3);
			}
		});
	}
	
	////////////////////////////
	
	public static VKFormatWriter get(VkFormat info){
		for(VKFormatWriter w : WRITERS){
			if(w.canWriteAs(info)){
				return w;
			}
		}
		return null;
	}
	
	////////////////////////////
	
	protected ByteBuffer dest;
	
	public void setDest(ByteBuffer buff){
		dest=buff;
	}
	
	public abstract boolean canWriteAs(VkFormat info);
	
}
