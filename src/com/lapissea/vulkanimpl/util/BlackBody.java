package com.lapissea.vulkanimpl.util;

import com.lapissea.vec.color.ColorMSolid;

public class BlackBody{
	
	public static ColorMSolid blackBodyColor(ColorMSolid dest, double kelvins){
		float x =(float)(kelvins/1000.0);
		float x2=x*x;
		float x3=x2*x;
		float x4=x3*x;
		float x5=x4*x;
		
		
		if(kelvins<=6600) dest.r(1);
		else dest.r(0.0002889f*x5-0.01258f*x4+0.2148f*x3-1.776f*x2+6.907f*x-8.723f);
		
		if(kelvins<=6600) dest.g(-4.593e-05f*x5+0.001424f*x4-0.01489f*x3+0.0498f*x2+0.1669f*x-0.1653f);
		else dest.g(-1.308e-07f*x5+1.745e-05f*x4-0.0009116f*x3+0.02348f*x2-0.3048f*x+2.159f);
		
		if(kelvins<=2000f) dest.b(0);
		else if(kelvins<6600f) dest.b(1.764e-05f*x5+0.0003575f*x4-0.01554f*x3+0.1549f*x2-0.3682f*x+0.2386f);
		else dest.b(1);
		
		return dest;
	}
	
}