package com.testapp.vknews.tools;

public class ClientException extends BaseException {

	public ClientException(String message) {
		super(message);
	}
	
	public ClientException(String message, Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8278902168906691410L;

}
