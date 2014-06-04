package com.testapp.vknews.tools;


public class AnswerException extends ServerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1607674663876097610L;

	/**
	 * @param message
	 */
	public AnswerException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param throwable
	 */
	public AnswerException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
