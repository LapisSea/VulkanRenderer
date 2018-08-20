package com.lapissea.vulkanimpl.renderer.lighting;

import com.lapissea.vec.Vec3f;
import com.lapissea.vec.color.ColorMSolid;

import java.nio.ByteBuffer;

import static com.lapissea.util.ObjectSize.*;

public class DirectionalLight{
	
	public static final int SIZE     =sizeof()+Float.SIZE*2;
	public static final int SIZE_BYTE=SIZE/Byte.SIZE;
	
	public Vec3f       normal;
	public ColorMSolid color;
	
	public DirectionalLight(Vec3f normal, ColorMSolid color){
		this.normal=normal;
		this.color=color;
	}
	
	public void put(ByteBuffer dest){
		normal.put(dest);
		dest.putFloat(0);
		color.putRGB(dest);
		dest.putFloat(0);
	}
}
