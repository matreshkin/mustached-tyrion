package com.testapp.vknews.tools;

import java.io.PrintWriter;

public class BaseException extends Exception {

	public BaseException(String message) {
		super(message);
		Log.e("BaseException", message + "\n");
	}
	
	public BaseException(String message, Throwable throwable) {
		super(message, throwable);
		try {
			Log.e("BaseException", message + "\n" + (throwable == null ? "" : throwable.getMessage()));
		} catch (Exception e) {e.printStackTrace();
				
		}
	}

	@Override
	public void printStackTrace(PrintWriter p) {
		if (Log.DEBUG || Log.TEST) super.printStackTrace(p);
		else Log.e("ERROR", "" + getMessage());
	}
	/**
	 * for serializing
	 */
	private static final long serialVersionUID = 8116306296415672579L;
	
}
