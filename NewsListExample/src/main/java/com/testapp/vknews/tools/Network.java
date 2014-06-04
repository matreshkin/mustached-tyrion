package com.testapp.vknews.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;
import android.os.Build;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;


public class Network {
	
	private static final String TAG = Network.class.getCanonicalName();

	public static final int CONNECTION_TIMEOUT = 60000;
	public static final int SOCKET_TIMEOUT = 60000;

    public static final String DEVICE_DESCRIPTION = "Android " +
            Build.VERSION.RELEASE +" (" + android.os.Build.MODEL + ")";
    public static final String USER_AGENT = DEVICE_DESCRIPTION;
	
	private Context mContext = null;
	private DocumentBuilder mDomBuilder = null;

    private static final ThreadPoolExecutor mTimeoutExecutor =
            new ThreadPoolExecutor(1, 1, 500, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
	
	public Network(Context context) {
		mContext = context;
		try {
			mDomBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("==========> WTF ??? <===========", e);
		}
	}
	
	/**
	 * For quick get (littlt answer)
	 *
	 * @param strUri
	 * @param headers
	 * @return
	 * @throws NetworkException
	 * @throws HttpException
	 * @throws ClientException
	 */
	public static String getStr(String strUri, Map<String, String> headers)
			throws NetworkException,
			HttpException,
			ClientException {

		String sourceString = null;
	    try {
			HttpEntity resEntity = get(strUri, headers);
			if (resEntity != null) {
				sourceString = new String(EntityUtils.toString(resEntity));
			}
		} catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		}
	    return sourceString;
	}

	public Node getXml(String strUri, Map<String, String> headers)
			throws NetworkException,
			HttpException,
			ClientException,
			AnswerException {

		Node sourceString = null;
	    try {
			HttpEntity resEntity = get(strUri, headers);
			if (resEntity != null) {
				sourceString = getXmlFromEntity(resEntity);
			}
		} catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
		}
	    return sourceString;
	}
	
	/**
	 * Full realized GET
	 * 
	 * @param strUri
	 * @param headers
	 * @return
	 * @throws NetworkException
	 * @throws ClientException
	 * @throws HttpException
	 */
	public static HttpEntity get(String strUri, Map<String, String> headers) 
			throws NetworkException,
			ClientException,
			HttpException {
		
		Log.d(TAG, "GET: " + String.valueOf(strUri) + " HEADERS: " + String.valueOf(headers));
		
		HttpEntity resEntityGet = null;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(
					httpParameters, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(
					httpParameters, SOCKET_TIMEOUT);
			HttpClient client = new DefaultHttpClient(httpParameters); 
			URI uri = new URI(strUri);
			HttpGet get = new HttpGet(uri);
			HttpResponse responseGet = null;
			addHeaders(get, headers);
			get.setHeader("User-Agent", USER_AGENT);
			responseGet = client.execute(get);
			if (responseGet == null || responseGet.getStatusLine() == null) {
				throw new NetworkException("No response");
			}
            resEntityGet = processHttpError(uri, responseGet);

			readHeaders(responseGet, headers);			

	    } catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
	    } catch (IllegalStateException e) {
			throw new NetworkException("Network is not available", e);
		} catch (ClientProtocolException e) {
			throw new NetworkException("Protocol error", e);
		} catch (UnknownHostException e) {
			throw new NetworkException("Unknown host", e);
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		} catch (URISyntaxException e) {
		    throw new ClientException("Bad Uri: " + strUri, e);
		}
		return resEntityGet;
	}

	public static String postStr(String strUri, Map<String, String> headers,
			String postData)
			throws NetworkException,
			HttpException,
			ClientException {

		String sourceString = null;
	    try {
			HttpEntity resEntity = post(strUri, headers, postData);
			if (resEntity != null) {
				sourceString = new String(EntityUtils.toString(resEntity));
			}
		} catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		}
	    return sourceString;
	}

	public Node postXml(String strUri, Map<String, String> headers,
			String postData)
			throws NetworkException,
			HttpException,
			ClientException,
			AnswerException {

		Node sourceString = null;
	    try {
			HttpEntity resEntity = post(strUri, headers, postData);
			if (resEntity != null) {
				sourceString = getXmlFromEntity(resEntity);
			}
		} catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
		}
	    return sourceString;
	}

	public JSONObject postJson(String strUri, Map<String, String> headers,
			String postData)
			throws NetworkException,
			HttpException,
			ClientException,
			AnswerException {

		JSONObject obj = null;
	    try {
			HttpEntity resEntity = post(strUri, headers, postData);
			if (resEntity != null) {
				obj = getJsonFromEntity(resEntity);
			}
		} catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
		}
	    return obj;
	}

	public static HttpEntity post(String strUri, Map<String, String> headers,
			String postData)
			throws NetworkException,
			HttpException,
			ClientException {
		try {
			StringEntity entity = null;
            if (postData != null) {
                entity = new StringEntity(postData, "UTF-8");
			    entity.setContentType("application/x-www-form-urlencoded");
            }
			return post(strUri, headers, entity);
		} catch (UnsupportedEncodingException e) {
			throw new NetworkException("Unsupported string encoding", e);
		} catch (IllegalStateException e) {
			throw new NetworkException("Network error", e);
		}
	}
	
	public static HttpEntity post(String strUri, Map<String, String> headers,
			HttpEntity postEntity) 
			throws NetworkException,
			HttpException,
			ClientException {
		
		HttpEntity resEntity = null;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(
					httpParameters, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(
					httpParameters, SOCKET_TIMEOUT);
			final HttpClient client = new DefaultHttpClient(httpParameters);
			URI uri = new URI(strUri);
			final HttpPost post = new HttpPost(uri);
			addHeaders(post, headers);
	        if (postEntity != null) post.setEntity(postEntity);
			HttpResponse response = null;
            final HttpResponse[] _response = new HttpResponse[] {null};
			post.setHeader("User-Agent", USER_AGENT);
            // This hack is for timout during looking up by sucking DNS
            Future<?> future = mTimeoutExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        _response[0] = client.execute(post);
                    } catch (Exception e) {
                    }
                }
            });
            try {
                future.get(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
                if (e.getCause() != null && e.getCause() instanceof Exception) {
                    Exception ex = (Exception)e.getCause();
                    throw  ex;
                }
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
			response = _response[0];
            if (!future.isDone()) {
                future.cancel(true);
				if (!post.isAborted())
					post.abort();
			}

			if (response == null 
					|| response.getStatusLine() == null)  {
				throw new NetworkException("No response");
			}

            resEntity = processHttpError(uri, response);
			
			readHeaders(response, headers);
			
	    } catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
	    } catch (IllegalStateException e) {
			throw new NetworkException("Network is not available", e);
		} catch (ClientProtocolException e) {
			throw new NetworkException("Protocol error", e);
		} catch (UnknownHostException e) {
			throw new NetworkException("Unknown host", e);
		} catch (URISyntaxException e) {
		    throw new ClientException("Bad Uri: " + strUri, e);
		} catch (IOException e) {
            throw new NetworkException("Network error", e);
        } catch (NetworkException e) {
            throw e;
        } catch (HttpException e) {
            throw e;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Internal error", e);
        }
		return resEntity;
	}

    private static HttpEntity processHttpError(URI uri, HttpResponse response)
            throws HttpException {
        HttpEntity resEntity;
        int errCode = response.getStatusLine().getStatusCode();

        Log.d("_API", ">>>>> SERVER CODE: " + errCode + " <<<<<");

        resEntity = response.getEntity();
        if (errCode != 200) {
            String body = null;
            try {
                body = Utils.toString(resEntity.getContent());
            } catch (Exception e) {
            }
            throw new HttpException(String.valueOf(errCode),
                    response.getStatusLine().getReasonPhrase(),
                    body);
        }
        return resEntity;
    }

    public HttpEntity post(String strUri,  Map<String, String> headers,
			List<BasicNameValuePair> postData)
			throws NetworkException,
			HttpException,
			ClientException {


//		if (postData == null) postData = "";
//
//		int l  = postData.length();
//		Log.d(TAG, "POST: " + String.valueOf(strUri) + " HEADERS: " + String.valueOf(headers)
//				+ " DATA: " + (l > 255 ? postData.substring(0, 256) : postData));

		HttpEntity resEntity = null;
		try {
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(
					httpParameters, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(
					httpParameters, SOCKET_TIMEOUT);
			HttpClient client = new DefaultHttpClient(httpParameters);
			URI uri = new URI(strUri);
			HttpPost post = new HttpPost(uri);
			addHeaders(post, headers);
	        if (postData != null && postData.size() > 0) {
	        	post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
	        }

			HttpResponse response = null;
			response = client.execute(post);

			if (response == null
					|| response.getStatusLine() == null) {
				throw new NetworkException("No response");
			}
			int errCode = response.getStatusLine().getStatusCode();
			if (errCode != 200) {
				final HttpEntity entity = response.getEntity();
				String resp = null;
				if (entity != null) {
					ByteArrayOutputStream os = null;
					// String resp = null;
					os = new ByteArrayOutputStream();
					entity.writeTo(os);
					resp = os.toString();
					Log.e("post", "Response code = "+errCode+"\n "+resp);
				}
				throw new HttpException(String.valueOf(errCode),
						response.getStatusLine().getReasonPhrase());
			}

			resEntity = response.getEntity();
			readHeaders(response, headers);

	    } catch (ParseException e) {
			throw new NetworkException("Web parse error", e);
	    } catch (IllegalStateException e) {
			throw new NetworkException("Network is not available", e);
		} catch (ClientProtocolException e) {
			throw new NetworkException("Protocol error", e);
		} catch (UnknownHostException e) {
			throw new NetworkException("Unknown host", e);
		} catch (UnsupportedEncodingException e) {
			throw new NetworkException("Unsupported string encoding", e);
		} catch (URISyntaxException e) {
		    throw new ClientException("Bad Uri: " + strUri, e);
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		}
		return resEntity;

	}

	public static boolean checkConnection(Context context) {
		try {
			final ConnectivityManager conMgr =  (ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
			if (activeNetwork != null && activeNetwork.isConnected()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean checkConnection() {
		return checkConnection(mContext);
	}

	public static void addHeaders(AbstractHttpMessage request,
			Map<String, String> headers) {
		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				request.setHeader(key, headers.get(key));
			}
		}
	}


	public static void readHeaders(HttpResponse response,
			Map<String, String> headers) {
		if (headers == null) return;
		headers.clear();
		Header[] arrHeaders = response.getAllHeaders();
		for (Header header : arrHeaders)
			headers.put(header.getName(), header.getValue());
	}


	private Node getXmlFromEntity(HttpEntity entity)
			throws AnswerException,
			NetworkException {
		try {
			Document xml = mDomBuilder.parse(entity.getContent());
			if (xml == null)
				throw new AnswerException("Failed to get response element");
			Node response = xml.getFirstChild();
			if (response == null)
				throw new AnswerException("Failed to get response element");
			return response;
		} catch (SAXException e) {
			throw new NetworkException("Answer parse problem", e);
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		}
	}

	private JSONObject getJsonFromEntity(HttpEntity entity)
			throws AnswerException,
			NetworkException {
		try {
			JSONObject obj = new JSONObject(Utils.toString(entity.getContent()));
			return obj;
		} catch (IOException e) {
			throw new NetworkException("Network error", e);
		} catch (IllegalStateException e) {
			throw new NetworkException("Network state error", e);
		} catch (JSONException e) {
			throw new NetworkException("Answer json parse proble", e);
		}
	}

	
}