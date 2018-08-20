package com.lapissea.vulkanimpl.exceptions;

public class VkShaderCompilationException extends VkException{
	public VkShaderCompilationException(){
	}
	
	public VkShaderCompilationException(String message){
		super(message);
	}
	
	public VkShaderCompilationException(String message, Throwable cause){
		super(message, cause);
	}
	
	public VkShaderCompilationException(Throwable cause){
		super(cause);
	}
}
