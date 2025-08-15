package com.yaw.yawslave;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yaw.yawcommon.BleIds;
// SlaveAdvertiser.java
public class SlaveAdvertiser {
    public interface Listener {
        void onStarted();
        void onFailed(int errorCode);
    }

    private final BluetoothLeAdvertiser adv;
    private final Context appCtx;
    private final Listener listener;
    private final AdvertiseCallback cb = new AdvertiseCallback() {
        @Override public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            if (listener != null) listener.onStarted();
        }
        @Override public void onStartFailure(int errorCode) {
            if (listener != null) listener.onFailed(errorCode);
        }
    };

    public SlaveAdvertiser(Context ctx, Listener l) {
        this.appCtx = ctx.getApplicationContext();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.adv = (adapter != null) ? adapter.getBluetoothLeAdvertiser() : null;
        this.listener = l;
    }

    public boolean start() {
        if (adv == null) return false;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        // Primary ADV: UUID ONLY
        AdvertiseData advData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false) // IMPORTANT: keep false here
                .addServiceUuid(new ParcelUuid(BleIds.SERVICE_UUID))
                .build();

        // Scan response: put the name here
        AdvertiseData scanResp = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        try {
            adv.startAdvertising(settings, advData, scanResp, cb); // 4-arg overload
            return true;
        } catch (SecurityException se) {
            Log.w("SlaveAdvertiser", "startAdvertising denied", se);
            return false;
        }
    }


    public void stop() {
        if (adv == null) return;
        try { adv.stopAdvertising(cb); } catch (SecurityException ignored) {}
    }
}
