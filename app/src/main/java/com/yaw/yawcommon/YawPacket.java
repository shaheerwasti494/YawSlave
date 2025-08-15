package com.yaw.yawcommon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class YawPacket {
    public final short yawHundredths;     // [-18000..+18000]
    public final int monotonicMillis;     // elapsedRealtime from sender

    public YawPacket(short yawHundredths, int monotonicMillis) {
        this.yawHundredths = yawHundredths;
        this.monotonicMillis = monotonicMillis;
    }

    public byte[] toBytes() {
        ByteBuffer bb = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(yawHundredths);
        bb.putInt(monotonicMillis);
        return bb.array();
    }

    public static YawPacket fromBytes(byte[] b) {
        if (b == null || b.length < 6) throw new IllegalArgumentException("Bad packet");
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        return new YawPacket(bb.getShort(), bb.getInt());
    }
}

