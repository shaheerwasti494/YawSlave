package com.yaw.yawcommon;

import java.util.UUID;

public final class BleIds {
    public static final UUID SERVICE_UUID  = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_YAW_UUID = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD_UUID     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BleIds() {}
}
