package com.yaw.yawslave;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.yaw.yawcommon.BleIds;
import com.yaw.yawcommon.YawPacket;

import java.util.ArrayList;
import java.util.List;

/** GATT server that exposes a notifiable YAW characteristic. */
public class SlaveGattServer {
    private static final String TAG = "SlaveGattServer";

    private final Context appCtx;
    private final BluetoothManager btMgr;

    private BluetoothGattServer gattServer; // null if permission missing / failed to open
    private final BluetoothGattCharacteristic yawChar;
    private final List<BluetoothDevice> subscribers = new ArrayList<>();
    private byte[] lastValue = new byte[6];

    public SlaveGattServer(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        this.btMgr = appCtx.getSystemService(BluetoothManager.class);

        // Prepare characteristic + CCCD
        yawChar = new BluetoothGattCharacteristic(
                BleIds.CHAR_YAW_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        BluetoothGattDescriptor cccd = new BluetoothGattDescriptor(
                BleIds.CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        );
        yawChar.addDescriptor(cccd);

        // Open the GATT server if permitted; also catch possible SecurityException
        if (hasConnectPermission()) {
            try {
                gattServer = btMgr.openGattServer(appCtx, callback);
                if (gattServer != null) {
                    BluetoothGattService svc = new BluetoothGattService(
                            BleIds.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                    svc.addCharacteristic(yawChar);
                    gattServer.addService(svc);
                }
            } catch (SecurityException se) {
                Log.w(TAG, "openGattServer() denied by platform permissions", se);
                gattServer = null;
            }
        } else {
            gattServer = null;
        }
    }

    /** Send latest YawPacket to all connected subscribers (notifications). */
    public void push(YawPacket pkt) {
        lastValue = pkt.toBytes();
        if (gattServer == null || !hasConnectPermission()) return;

        yawChar.setValue(lastValue);
        // Notify each subscriber; guard and catch for safety
        for (BluetoothDevice d : new ArrayList<>(subscribers)) {
            try {
                gattServer.notifyCharacteristicChanged(d, yawChar, false);
            } catch (SecurityException se) {
                Log.w(TAG, "notifyCharacteristicChanged() denied", se);
                // If permissions were revoked, we can stop trying further
                break;
            }
        }
    }

    /** Close the GATT server. Safe to call multiple times. */
    public void close() {
        if (gattServer != null) {
            if (hasConnectPermission()) {
                try {
                    gattServer.close();
                } catch (SecurityException se) {
                    Log.w(TAG, "gattServer.close() denied", se);
                }
            }
            gattServer = null;
        }
        subscribers.clear();
    }

    // ----------------- Helpers -----------------

    /** True when BLUETOOTH_CONNECT is granted (or OS < 31 where it's not required). */
    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        return ContextCompat.checkSelfPermission(appCtx, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ----------------- GATT callbacks -----------------

    private final BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!subscribers.contains(device)) subscribers.add(device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                subscribers.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            if (gattServer == null || !hasConnectPermission()) return;
            if (BleIds.CHAR_YAW_UUID.equals(characteristic.getUuid())) {
                int off = Math.max(0, Math.min(offset, lastValue.length));
                byte[] slice = new byte[lastValue.length - off];
                System.arraycopy(lastValue, off, slice, 0, slice.length);
                try {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, off, slice);
                } catch (SecurityException se) {
                    Log.w(TAG, "sendResponse() denied", se);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (gattServer == null || !hasConnectPermission()) return;
            if (BleIds.CCCD_UUID.equals(descriptor.getUuid())) {
                if (responseNeeded) {
                    try {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    } catch (SecurityException se) {
                        Log.w(TAG, "sendResponse(CCCD) denied", se);
                    }
                }
            }
        }
    };
}
