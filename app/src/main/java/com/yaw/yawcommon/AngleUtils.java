package com.yaw.yawcommon;

public final class AngleUtils {
    private AngleUtils() {}

    public static short degToHundredths(float deg0to360) {
        float signed = (deg0to360 <= 180f) ? deg0to360 : (deg0to360 - 360f);
        int hundredths = Math.round(signed * 100f);
        if (hundredths < -18000) hundredths = -18000;
        if (hundredths >  18000) hundredths =  18000;
        return (short) hundredths;
    }

    public static float hundredthsToSignedDeg(short h) { return h / 100f; }

    public static float relativeYaw(float slaveDeg0to360, float masterDeg0to360) {
        float d = slaveDeg0to360 - masterDeg0to360;
        while (d > 180f) d -= 360f;
        while (d < -180f) d += 360f;
        return d;
    }
}