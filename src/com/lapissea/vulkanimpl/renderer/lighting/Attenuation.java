package com.lapissea.vulkanimpl.renderer.lighting;

import java.nio.ByteBuffer;

public class Attenuation{
	
	private static final float DEFAULT_FALLOFF=2/256F;
	
	public static Attenuation fromRadius(float radius){
		return fromRadius(radius, DEFAULT_FALLOFF);
	}
	
	public static Attenuation fromRadius(float radius, float falloff){
		
		float constant=0;
		float linear  =0;
		float exponential;
		
		exponential=((1/falloff-constant)-radius*linear)/(radius*radius);
		
		return new Attenuation(constant, linear, exponential);
	}
	
	private float constant;
	private float linear;
	private float exponential;
	private float radius;
	
	public Attenuation(){}
	
	public Attenuation(float constant, float linear, float exponential){
		this.constant=constant;
		this.linear=linear;
		this.exponential=exponential;
		calcRadius();
	}
	
	private void calcRadius(){
		calcRadius(DEFAULT_FALLOFF);
	}
	
	private void calcRadius(float falloff){
		radius=(-linear+(float)Math.sqrt(exponential*(exponential+4*(1/falloff-constant))))/(2*exponential);
	}
	
	public void put(ByteBuffer dest){
		dest.putFloat(constant);
		dest.putFloat(linear);
		dest.putFloat(exponential);
//		dest.putFloat(1);
		dest.putFloat(radius);
	}
	
	public void setConstant(float constant){
		this.constant=constant;
		calcRadius();
	}
	
	public void setLinear(float linear){
		this.linear=linear;
		calcRadius();
	}
	
	public void setExponential(float exponential){
		this.exponential=exponential;
		calcRadius();
	}
	
	public float getConstant(){
		return constant;
	}
	
	public float getLinear(){
		return linear;
	}
	
	public float getExponential(){
		return exponential;
	}
	
	public float getRadius(){
		return radius;
	}
}
