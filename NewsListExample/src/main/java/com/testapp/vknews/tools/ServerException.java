package com.testapp.vknews.tools;


public class ServerException extends BaseException {

	private String mErrorcode = null;
	
	public ServerException(String message) {
		super(message);
	}
	
	public ServerException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	
	public ServerException(String errorcode, String message) {
		super(message);
		mErrorcode = errorcode;
	}
	
	public String getErrorcode() {
		return mErrorcode;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5816442505405802129L;

}
