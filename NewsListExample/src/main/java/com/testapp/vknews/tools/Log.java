package com.testapp.vknews.tools;

public class Log {
    public static final boolean DEBUG = true;
    public static final boolean TEST = false;
    public static final boolean BENCHMARK = true;

    private static final String LOG_HEADER = "Application Log {{{\n";
    private static final String LOG_FOOTER = "}}} Application log on ";
    private static final int MAX_LOG_LENGTH = 1024 * 1024 / 2; // 1Mb

    private static StringBuilder sSessionLog =
            new StringBuilder(LOG_HEADER);

    public static int i(String tag, String msg) {
        android.util.Log.i(tag, msg);
        return appendToLog("I: " + tag, msg);
    }

    public static int d(String tag, String msg) {
        if (!DEBUG) return 0;
        android.util.Log.i(tag, msg);
        return appendToLog("D: " + tag, msg);
    }

    public static int v(String tag, String msg) {
        android.util.Log.v(tag, msg);
        return appendToLog("V: " + tag, msg);
    }

    public static int w(String tag, String msg) {
        android.util.Log.w(tag, msg);
        return appendToLog("W: " + tag, msg);
    }

    public static int e(String tag, String msg) {
        android.util.Log.e(tag, msg);
        return appendToLog("E: " + tag, msg);
    }

    public static int e(String tag, Exception e) {
        String msg = e == null ? "" : e.getMessage();
        return e(tag, msg == null ? "" : msg);
    }

    public static int b(String tag, String msg) {
        if (!BENCHMARK) return 0;
        android.util.Log.i(tag, msg);
        return appendToLog("B: " + tag, msg);
    }

    public static int wtf(String tag, String msg) {
        android.util.Log.i(tag, msg);
        return appendToLog("WTF: " + tag, msg);
    }


    public static int d(String tag, String msg, Throwable th) {
        if (!DEBUG) return 0;
        String res = msg + "\n" + android.util.Log.getStackTraceString(th);
        android.util.Log.i(tag, msg, th);
        return appendToLog("D: " + tag, res);
    }
    public static int i(String tag, String msg, Throwable th) {
        String res = msg + "\n" + android.util.Log.getStackTraceString(th);
        android.util.Log.i(tag, msg, th);
        return appendToLog("I: " + tag, res);
    }
    public static int w(String tag, String msg, Throwable th) {
        String res = msg + "\n" + android.util.Log.getStackTraceString(th);
        android.util.Log.i(tag, msg, th);
        return appendToLog("W: " + tag, res);
    }
    public static int e(String tag, String msg, Throwable th) {
        String res = msg + "\n" + android.util.Log.getStackTraceString(th);
        android.util.Log.i(tag, msg, th);
        return appendToLog("E: " + tag, res);
    }
    public static int wtf(String tag, String msg, Throwable th) {
        String res = msg + "\n" + android.util.Log.getStackTraceString(th);
        android.util.Log.i(tag, msg, th);
        return appendToLog("WTF: " + tag, res);
    }
    public static int e(Throwable th) {
        return e("NO TAG", "", th);
    }
    public static int e(String tag, Throwable th) {
        return e(tag, "", th);
    }

    public static synchronized String getSessionLog() {
        truncateLog();
        sSessionLog.insert(0, LOG_HEADER);
        sSessionLog.append(LOG_FOOTER);
        String log = sSessionLog.toString();
        sSessionLog.delete(0, sSessionLog.length() - 1);
        return log;
    }


    private static synchronized int appendToLog(String tag, String msg) {
        try {
            msg = msg.replace("\n", "\n\t");
            msg = tag + "\t:\t" + msg + "\n";
            sSessionLog.append(msg);
            truncateLog();
            return msg.length();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void truncateLog() {
        int dl = sSessionLog.length() - (MAX_LOG_LENGTH -
                (LOG_FOOTER.length() + LOG_HEADER.length()));
        if (dl > 0) sSessionLog.delete(0, dl);
    }
}