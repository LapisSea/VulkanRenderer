package com.lapissea.vulkanimpl;

import com.lapissea.vec.Vec3f;
import com.lapissea.vec.interf.IVec2iR;
import org.joml.Matrix4f;

public class VkCamera{
	
	protected float nearPlane=0.001F;
	protected float farPlane =1000F;
	protected float fow      =(float)Math.toRadians(85);
	protected Vec3f rotation =new Vec3f();
	protected Vec3f pos      =new Vec3f();
	
	protected final Matrix4f projection=new Matrix4f();
	protected final Matrix4f view      =new Matrix4f();
	
	public Matrix4f getView(){
		return view.identity()
		           .rotateZ(rotation.z())
		           .rotateX(rotation.x())
		           .rotateY(rotation.y())
		           .translate(-pos.x(), -pos.y(), -pos.z());
	}
	
	public Matrix4f getProjection(IVec2iR displaySize){
		return projection.identity().perspective(fow, displaySize.divXY(), nearPlane, farPlane, true).scale(1, -1, 1);
	}
	
}
