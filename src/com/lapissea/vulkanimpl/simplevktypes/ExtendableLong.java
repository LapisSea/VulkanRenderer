package com.lapissea.vulkanimpl.simplevktypes;

public abstract class ExtendableLong extends Number implements Comparable<ExtendableLong>{
	
	protected long val;
	
	ExtendableLong(long val){
		this.val=val;
	}
	
	public long get(){
		return val;
	}
	
	@Override
	public int compareTo(ExtendableLong o){
		return Long.compare(val, o.val);
	}
	
	@Override
	public int intValue(){
		return (int)val;
	}
	
	@Override
	public long longValue(){
		return val;
	}
	
	@Override
	public float floatValue(){
		return val;
	}
	
	@Override
	public double doubleValue(){
		return val;
	}
}
