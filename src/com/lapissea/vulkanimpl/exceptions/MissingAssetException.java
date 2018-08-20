package com.lapissea.vulkanimpl.exceptions;

public class MissingAssetException extends RuntimeException{
	public MissingAssetException(){
	}
	
	public MissingAssetException(String message){
		super(message);
	}
	
	public MissingAssetException(String message, Throwable cause){
		super(message, cause);
	}
	
	public MissingAssetException(Throwable cause){
		super(cause);
	}
}
