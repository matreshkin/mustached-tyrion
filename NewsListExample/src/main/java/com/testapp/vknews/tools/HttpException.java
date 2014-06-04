/**
 * 
 */
package com.testapp.vknews.tools;

import java.util.HashMap;
import java.util.Map;

import android.content.res.Resources;
import org.json.JSONArray;

public class HttpException extends ServerException {

	private String mErrorCode = null;
	private String mErrorMessage = null;
	private String mDetailMessage = null;



	/**
	 * 
	 */

	private static final long serialVersionUID = -2117786243547177581L;

	/**
	 * @param message
	 */

	public HttpException(String message) {
		this(null, message);
	}

	public HttpException(String errorcode, String message,
						 String detailMessage, Throwable throwable) {
		super(message, throwable);
		mErrorCode = errorcode;
		mErrorMessage = message;
		mDetailMessage = detailMessage;
	}


	/**
	 * @param message
	 * @param throwable
	 */
	public HttpException(String message, Throwable throwable) {
		this(null, message, null, throwable);
	}

	/**
	 * @param errorcode
	 * @param message
	 */
	public HttpException(String errorcode, String message) {
		this(errorcode, message, null, null);
	}
	
	public HttpException(String errorcode, String message,
			String detailMessage) {
		this(errorcode, message, detailMessage, null);
	}

	/**
	 * @return код возврата e.g. "404"
	 */
	public String getErrorCode() {
		return mErrorCode;
	}

	/**
	 * @return строку - Reason Phrase, e.g. "NOT FOUND"
	 */
	public String getErrorMessage() {
		return mErrorMessage;
	}
	
	/**
	 * @return стрку в теле ответа сервера
	 */
	public String getDetailMessage() {
		return mDetailMessage;
	}

}