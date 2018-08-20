package com.lapissea.vulkanimpl.util;

import com.lapissea.datamanager.DataSignature;

import java.io.IOException;
import java.io.InputStream;

public class AnyImage{
	
	public enum Channel{
		R, G, B, A, Z;
	}
	
	public AnyImage(DataSignature source){
		this(source.path, source.readAllBytes());
	}
	
	public AnyImage(String name, InputStream source) throws IOException{
		this(name, source.readAllBytes());
		
	}
	
	public AnyImage(String name, byte[] source){
		switch(name.substring(name.length()-4, name.length())){
		case "":{
		
		}
		}
		
	}
	
}
