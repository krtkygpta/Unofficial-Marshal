package com.airoha.libNativePeq;

/** JNI surface from the official Marshall / Airoha libnative-peq.so. */
public final class NativePeq {
    static {
        System.loadLibrary("native-peq");
    }

    private NativePeq() {}

    public static native void calcZ(double d8);

    public static native int changeRescaleCofe(int i7, double d8);

    public static native void destroy(int i7);

    public static native int generateCofe(int i7);

    public static native int generateFreqResp(int i7);

    public static native int getCofeCount(int i7);

    public static native short[] getCofeParam(int i7);

    public static native double[] getRespFreq(int i7);

    public static native double[] getRespY(int i7);

    public static native void setHsfPoint(int i7, int i8, double d8, double d9, double d10);

    public static native void setLsfPoint(int i7, int i8, double d8, double d9, double d10);

    public static native void setParam(int i7, double d8, int i8, int i9, int i10, int i11);

    public static native void setPeqPoint(int i7, int i8, double d8, double d9, double d10);

    public static native void setXpfPoint(int i7, int i8, double d8, double d9, int i9);
}
