package com.testapp.vknews.tools;

public class NetworkDisabledException extends NetworkException {

	private static final long serialVersionUID = 8746667953604199276L;

	public NetworkDisabledException() {
		super("Network is disabled");
	}
	
	public NetworkDisabledException(String message) {
		super(message);
	}

	public NetworkDisabledException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
