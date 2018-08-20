package com.lapissea.vulkanimpl.renderer.lighting;

import com.lapissea.vec.Vec3f;
import com.lapissea.vec.color.ColorMSolid;
import com.lapissea.vec.color.IColorMSolid;

import java.nio.ByteBuffer;

import static com.lapissea.util.ObjectSize.*;

public class PointLight{
	
	public static final int SIZE     =sizeof()+Float.SIZE*2;
	public static final int SIZE_BYTE=SIZE/Byte.SIZE;
	
	public ColorMSolid color;
	public Vec3f       pos;
	public Attenuation attenuation;
	
	public PointLight(Vec3f pos, IColorMSolid color, Attenuation attenuation){
		this.pos=pos;
		this.color=new ColorMSolid(color);
		this.attenuation=attenuation;
	}
	
	public void put(ByteBuffer dest){
//		pos.put(dest);
//		dest.putFloat(1);
		color.putRGB(dest);
		dest.putFloat(1);
		attenuation.put(dest);
		
	}
}
