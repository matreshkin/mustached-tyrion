package com.testapp.vknews.tools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	
	public static String getNormalizedUri(String strUri, 
			Map<String, String> query) {
		URI uri = URI.create(strUri);
		String path = uri.getPath();
		String res = path + "?" + getNormalizedQuery(query);
		return res;
	}	
	public static String getNormalizedQuery(Map<String, String> query) {
		ArrayList<QueryPair> params = getSortedEncodedQueryList(query);
		String normalizedQuery = joinQueryList(params);
		return normalizedQuery;
	}
	
	public static boolean isEmailValid(String email) {
	    boolean isValid = false;

	    String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
	    CharSequence inputStr = email;

	    Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(inputStr);
	    if (matcher.matches()) {
	        isValid = true;
	    }
	    return isValid;
	}

	public static boolean isSdAvailable(){

		boolean available = false;
		String sdState = Environment.getExternalStorageState();

		if (sdState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
				|| sdState.equals(Environment.MEDIA_MOUNTED)){
			available = true;
		}

		return available;
	}

	/**
	 * @param key
	 * @param src
	 * @return Base64 encoded HMac with SHA-1
	 * @throws ClientException
	 */
	public static String calcHmac(String key, String src) 
			throws ClientException {
	    Mac mac = null;
		try {
			mac = Mac.getInstance("HmacSHA1");
		    SecretKeySpec sk = new SecretKeySpec(
		    		key.getBytes(), mac.getAlgorithm());  
		    mac.init(sk);
		    byte[] result = mac.doFinal(src.getBytes());
		    return Base64.encodeToString(
		    		byte2Hex(result).getBytes(), Base64.DEFAULT);
		} catch (NoSuchAlgorithmException e) {
			throw new ClientException("Algoritm is not suppoted", e);
		} catch (InvalidKeyException e) {
			throw new ClientException("Secret key is invalid", e);
		}
	}
	
	/**
	 * @param query not encoded query
	 * @return {{"a", "b"}, {"c", "i%20am"}} - encode and sorted by RFC
	 */
	public static ArrayList<QueryPair> getSortedEncodedQueryList(
			Map<String, String> query)  
	{   
	    ArrayList<QueryPair> arr =
	    		new ArrayList<QueryPair>(); 
		if (query == null) return arr;
	    String[] params = query.keySet()
	    		.toArray(new String[query.size()]);
	    for (String key : params) {
	    	String val = query.get(key);
			val = encode(val);
			key = encode(key);
	    	arr.add(new QueryPair(key, val));
	    }
	    Collections.sort(arr, new QueryPairComparator());
	    return arr;  
	} 
	
	public static String encode(String val) {
		if (val == null) return "";
		StringBuilder builder = new StringBuilder();
		try {
			byte[] ascii = val.getBytes("UTF-8");
			for (int n = 0; n < ascii.length; n++) {
				byte b = ascii[n];
				if (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z'
						|| b >= '0' && b <= '9'
						|| b == '-' || b == '.' || b == '_' || b == '~')
					builder.append((char)b);
				else builder.append(String.format("%%%02X", b));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	/**
	 * Encode params and <br />
	 * Simply join with "&"
	 * @param params
	 * @return
	 */
	public static String joinQueryList(Map<String, ? extends Object> params) {
		if (params == null || params.size() == 0) return "";
		try {
			StringBuilder str = new StringBuilder();
			Set<String> keys = params.keySet();
			for (String key : keys) {
				if (key == null || key.length() == 0) continue;
				Object valVar = params.get(key);
                if (valVar instanceof Collection<?>) {
					Collection<?> arr = (Collection<?>)valVar;
					Iterator i = arr.iterator();
					while (i.hasNext()) {
						String val;
						Object o = i.next().toString();
                        if (o != null) val = o.toString();
                        else val = "";
                        str.append(URLEncoder.encode(key, "UTF-8"));
                        str.append("=");
                        str.append(URLEncoder.encode(val, "UTF-8"));
                        str.append("&");
                    }
                } else {
                    String val = valVar.toString();
                    if (val == null) val = "";
                    str.append(URLEncoder.encode(key, "UTF-8"));
                    str.append("=");
                    str.append(URLEncoder.encode(val, "UTF-8"));
                    str.append("&");
                }
			}
			if (str.length() > 0) str.deleteCharAt(str.length() - 1);
			return str.toString();
		} catch (UnsupportedEncodingException e) {
			Log.wtf("UTILS", "ENCODE TO UTF8: " + params.toString());
			return null;
		}
	}	

	public static String setToJsonArray(Set<String> set) {
		StringBuilder strJson = new StringBuilder("[");
		for (String key : set) strJson.append("\"" + key + "\",");
		strJson.deleteCharAt(strJson.length() - 1);
		strJson.append("]");
		return strJson.toString();
	}
	
	public static String toString(HttpEntity entity) {
		try {
			if (entity == null || entity.getContent() == null)
				return null;
			return toString(entity.getContent());
//			return EntityUtils.toString(entity);
		} catch (ParseException e) {
		} catch (IOException e) {
		} catch (Exception e) {
		}
		return null;
	}	
	
	public static String toString(JSONObject obj) {
		try {
			return obj.toString();
		} catch (Exception e) {
		}
		return null;
	}
	
	public static String toString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
		}
		String out = sw.toString();
		if (out == null) return  null;
		int l = out.length();
		l = l > 1024 ? 1024 : l;
		return out.substring(0, l).replace('\n', '\\');
	}
	
	public static String toString(InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	
	/**
	 * @param node
	 * @return Map<TagName, ListOfNamedItems<Map<AttrName, AttrValue>>>
	 */
	public static Map<String, List<Map<String, String>>> getNodeChild(Node node) {
		Map<String, List<Map<String, String>>> res = 
				new HashMap<String, List<Map<String,String>>>();
		NodeList items = node.getChildNodes();
		if (items != null) {
			for (int n = 0; n < items.getLength(); n++) {
				Node item = items.item(n);
				if (item.getNodeType() != Node.ELEMENT_NODE)
					continue;
				String tag = item.getNodeValue();
				if (!res.containsKey(tag))
					res.put(tag, new ArrayList<Map<String,String>>());
				res.get(tag).add(getNodeAttr(item));
			}
		}
		return res;
	}	
	
	public static List<Map<String, String>> getNodeChild(Node node, String tag) {
		List<Map<String, String>> res = 
				new ArrayList<Map<String,String>>();
		NodeList items = node.getChildNodes();
		if (items != null) {
			for (int n = 0; n < items.getLength(); n++) {
				Node item = items.item(n);
				if (item.getNodeType() != Node.ELEMENT_NODE)
					continue;
				if (tag.equals(item.getNodeName()))
					res.add(getNodeAttr(item));
			}
		}
		return res;
	}	
	
	public static List<Map<String, String>> getJsonChild(JSONObject obj, String tag) {
		List<Map<String, String>> res = 
				new ArrayList<Map<String,String>>();
		// TODO
		return res;
	}

	
	public static Map<String, String> getNodeAttr(Node node) {
		Map<String, String> attrMap = new HashMap<String, String>();
		if (node == null) return attrMap;
		NamedNodeMap attr = node.getAttributes();
		for (int na = 0; na < attr.getLength(); na++) {
			Node a = attr.item(na);
			String key = a.getNodeName();
			if (key != null) attrMap.put(key, a.getNodeValue());
		}
		return attrMap;
	}	
	
	public static Map<String, String> getJsonParam(JSONObject obj) {
		Map<String, String> attrMap = new HashMap<String, String>();
		if (obj == null) return attrMap;
		Iterator<String> iter = obj.keys();
	    while (iter.hasNext()) {
	        String key = iter.next();
			try {
				Object value = obj.get(key);
	            attrMap.put(key, value.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
		return attrMap;
	}
		
	public static String getNearestResolution(int width, int height,
			String[] thumbResolutions) {
		assert(thumbResolutions != null);
		if (width > height) {
			int t = width;
			width = height;
			height = t;
		}
		String nearest = null;
		int dmin = Integer.MAX_VALUE;
		for (String res : thumbResolutions) {
			String[] r = res.split("x");
			int dw = Integer.parseInt(r[0]) - width;
			int dh = Integer.parseInt(r[1]) - height;
			if (dw > dh) {
				int t = dw;
				dw = dh;
				dh = t;
			}
			dw -= width;
			dh -= height;
			int d = dw * dw + dh * dh;
			if (d < dmin) {
				dmin = d;
				nearest = res;
			}
		}
		return nearest;
	}
	
	public static boolean showToast(Context context, String message) {
		Toast.makeText(context.getApplicationContext(),
	               message, Toast.LENGTH_SHORT).show();
		return true;
	}	
	
	public static void pushNotification(final Context context,
			int icon, String name, String descr, Intent activityIntent,
			Bitmap image) {
		NotificationManager notifyMgr = 
				(NotificationManager)context.getSystemService( 
		        Context.NOTIFICATION_SERVICE); 
		long when = System.currentTimeMillis(); 
		PendingIntent pIntent = PendingIntent.getActivity(
				context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = null;
		if (android.os.Build.VERSION.SDK_INT < 11)
			notification = getNotification8(context, 
					icon, name, descr, when, pIntent);
		else {
			if (image == null)
				notification = getNotification11(context,
						icon, name, descr, when, pIntent);
			else notification = getNotification11Image(context,
					icon, name, descr, when, pIntent, image);
		}
		notifyMgr.notify(NOTIFY_ID, notification);
	}
	
	@SuppressWarnings("deprecation")
	private static Notification getNotification8(Context context,
			int icon, String name, String descr, 
			long when, PendingIntent pIntent) {
		Notification notification = new Notification(icon, name, when);
		notification.setLatestEventInfo(context, name, descr, pIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		return notification;
	}
	@TargetApi(11)
	private static Notification getNotification11(Context context,
					  int icon, String name, String descr,
					  long when, PendingIntent pInten) {
		Notification notification = new Notification.Builder(context)
				.setTicker(name)
				.setContentTitle(name)
				.setContentText(descr)
				.setSmallIcon(icon)
				.setContentIntent(pInten)
				.setAutoCancel(true)
				.setWhen(when)
				.getNotification();
		return notification;
	}
	@TargetApi(11)
	private static Notification getNotification11Image(Context context,
					  int icon, String name, String descr,
					  long when, PendingIntent pInten,
					  Bitmap image) {
		Notification notification = new Notification.Builder(context)
				.setTicker(name)
				.setContentTitle(name)
				.setContentText(descr)
				.setSmallIcon(icon)
				.setContentIntent(pInten)
				.setAutoCancel(true)
				.setWhen(when)
				.setLargeIcon(image)
				.getNotification();
		return notification;
	}
	public static void clearNotifies(Context context) {
		NotificationManager notifyMgr = 
				(NotificationManager)context.getSystemService( 
		        Context.NOTIFICATION_SERVICE); 
		notifyMgr.cancel(NOTIFY_ID);
	}
	
	public static float getFreeStorageSpace(String path) {
		StatFs stat = new StatFs(path);
		//long blSize = stat.getBlockSize();
		long availSize = stat.getAvailableBlocks();
		long allSize = stat.getBlockCount();
		return (float)availSize / (float)allSize;
	}
	
	public static boolean deleteFileRecursive(File file) {
	    if (file.isDirectory()) {
	        File[] children = file.listFiles();
	        for (int i=0; i < children.length; i++)
	            if (!deleteFileRecursive(children[i]))
	                return false;
	    }
	    return file.delete();
	}
	
	/**
	 * Simply join with "&"
	 * @param params
	 * @return
	 */
	private static String joinQueryList(ArrayList<QueryPair> params) {
		if (params == null || params.size() == 0) return "";
		StringBuilder str = new StringBuilder();
		for (QueryPair pair : params) {
			str.append(pair.toString());
			str.append("&");
		}
		str.deleteCharAt(str.length() - 1);
		return str.toString();
	}
	
	private static String byte2Hex(byte[] arr)
    {
		StringBuilder str = new StringBuilder(arr.length * 2);
		for (int i = 0; i < arr.length; i++) {
			String h = Integer.toHexString(arr[i]);
			int l = h.length();
			if (l == 1) h = "0" + h;
			if (l > 2) h = h.substring(l - 2, l);
			str.append(h);
		}
		return str.toString();
	}
	

	public static boolean isWifiConnected(Context c) {
		ConnectivityManager connManager = (ConnectivityManager) 
				c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connManager == null) return false;
		NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifi == null) return false;
		return wifi.isConnected();
	}
	
	public static boolean isMobileInternetConnected(Context c) {
		ConnectivityManager connManager = (ConnectivityManager) 
				c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connManager == null) return false;
		NetworkInfo mobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (mobile == null) return false;
		return mobile.isConnected();
	}
	
	public static String escapeText(String text) {
		StringBuilder strBuilder = new StringBuilder();
		DatabaseUtils.appendEscapedSQLString(strBuilder, text);
		strBuilder.deleteCharAt(0);
		strBuilder.deleteCharAt(strBuilder.length() - 1);
		String str = strBuilder.toString();
		str = str.replaceAll("%", "^%")
			.replaceAll("_", "^_");
		return str;
	}
	
	public static synchronized String getFileMd5(File f, long start, int length) {
		try {
			if (!f.exists()) return null;
			InputStream in = new FileInputStream(f);
			in.skip(start);
			MessageDigest digester = MessageDigest.getInstance("MD5");
			final int maxCount = 8192;
			byte[] bytes = new byte[maxCount];
			int byteCount, readed = 0;
			int l = Math.min(maxCount, length);
			while (l > 0 && (byteCount = in.read(bytes, 0, l)) > 0) {
				digester.update(bytes, 0, byteCount);
				readed += byteCount;
				l = Math.min(maxCount, length - readed);
			}
			byte[] digest = digester.digest();
			return byte2Hex(digest);
			
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	
	public static synchronized float getBatteryLevel(Context context) {		
		//initBatteryListener(context);	
		if (mBatteryStatusReciever == null || mBatteryStatusReciever.mLatchInit.getCount() > 0)
			return 1.0f;
		return mBatteryStatusReciever.getLevel();
	}	
	
	public static synchronized boolean isBatteryCharging(Context context) {
		//initBatteryListener(context);
		if (mBatteryStatusReciever == null || mBatteryStatusReciever.mLatchInit.getCount() > 0)
			return false;
		return mBatteryStatusReciever.isCharging();
	}
	
	public static String getMediaStoreImagePath(ContentResolver cr, long localId) {
		String path = null;
		Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				String.valueOf(localId));
		Cursor c = cr.query(uri, 
				new String[] {MediaStore.Images.Media.DATA},
				null, null, null);
		if (c != null && c.moveToFirst()) {
			path = c.getString(0);
		}
		if (c != null && !c.isClosed()) c.close();
		return path;
	}	
	
	public static String getMediaStoreVideoPath(ContentResolver cr, long localId) {
		String path = null;
		Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				String.valueOf(localId));
		Cursor c = cr.query(uri, 
				new String[] {MediaStore.Images.ImageColumns.DATA},
				null, null, null);
		if (c != null && c.moveToFirst()) {
			path = c.getString(0);
		}
		if (c != null && !c.isClosed()) c.close();
		return path;
	}
	
	private static void initBatteryListener(Context context) {
		if (mBatteryStatusReciever == null) {
			mBatteryStatusReciever = new BatteryStateReciever();
			mBatteryStatusReciever.init(context);
		}
	}

	
	private static BatteryStateReciever mBatteryStatusReciever = null;
	private static class BatteryStateReciever extends BroadcastReceiver {
				
		private float mLevel = 1.0f;
		private boolean mIsCharging = false;
		CountDownLatch mLatchInit = null;
		
		public void init(Context context) {
			mLatchInit = new CountDownLatch(1);
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_BATTERY_CHANGED);
			context.registerReceiver(this, filter);
			try {
				mLatchInit.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public synchronized float getLevel() {
			return mLevel;
		}
		
		public synchronized boolean isCharging() {
			return mIsCharging;
		}
		
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
			mLatchInit.countDown();
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			mIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
			                     status == BatteryManager.BATTERY_STATUS_FULL;
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			mLevel = level / (float)scale;
		}		
	}
	
	private static final int NOTIFY_ID = 101;

	public static boolean isFileSupported(Context c, String mimeType, String path) {
		File f = new File(path);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), mimeType);
        List<ResolveInfo> resolveInfos =
            c.getPackageManager().queryIntentActivities(intent, 0);
        int resolveInfoCount = resolveInfos.size();
		return resolveInfoCount > 0;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void startAsyncTask(AsyncTask<Void, Void, Void> task) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			task.execute();
		}
	}
	

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @SuppressLint("ServiceCast")
	@SuppressWarnings("deprecation")
	public static void copyTextToClipboard(Context c, String title, String text) {
    	int sdk = android.os.Build.VERSION.SDK_INT;
    	if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
    	    android.text.ClipboardManager clipboard = 
    	    		(android.text.ClipboardManager) 
    	    		c.getSystemService(Context.CLIPBOARD_SERVICE);
    	    clipboard.setText((title == null ? "" : title + "\n") + text);
    	} else {
    	    android.content.ClipboardManager clipboard =
    	    		(android.content.ClipboardManager)
    	    		c.getSystemService(Context.CLIPBOARD_SERVICE); 
    	    android.content.ClipData clip = 
    	    		android.content.ClipData.newPlainText(title, text);
    	    clipboard.setPrimaryClip(clip);
    	}
	}

    public static void launchInitUtils(Context context) {
    	if (mBatteryStatusReciever == null) {
    		mBatteryStatusReciever = new BatteryStateReciever();
    		
    		BatteryListenerInitThread t = new BatteryListenerInitThread(context);
    		t.setPriority(Thread.MIN_PRIORITY);
    		t.start();
    	}
    }	
    
    private static class BatteryListenerInitThread extends Thread {
    	private Context mContext = null;    	
    	
    	public BatteryListenerInitThread(Context context) {
    		mContext = context;
    	}
    	
    	@Override
		public void run() {
    		if (mBatteryStatusReciever != null) {
    			mBatteryStatusReciever.init(mContext);
    			interrupt();
    		}
    	}
    }
}


class QueryPairComparator implements Comparator<QueryPair> {
    @Override
    public int compare(QueryPair o1, QueryPair o2) {
    	int cn = o1.mKey.compareTo(o2.mKey);
    	if (cn != 0) return cn; 
        return o1.mVal.compareTo(o2.mVal);
    }
}

class QueryPair {
	public QueryPair(String key, String val) {
		mKey = key;
		mVal = val;
	}
	@Override
	public String toString() {
		return mKey + "=" + mVal;
	}
	
	String mKey = null;
	String mVal = null;
}