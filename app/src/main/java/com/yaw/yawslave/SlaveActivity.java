package com.yaw.yawslave;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.WindowInsets;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.yaw.yawslave.databinding.ActivitySlaveBinding;
import com.yaw.yawcommon.AngleUtils;
import com.yaw.yawcommon.YawPacket;
import com.yaw.yawcommon.YawProvider;

import java.util.Map;

public class SlaveActivity extends AppCompatActivity {

    private ActivitySlaveBinding vb;
    private YawProvider yawProvider;

    // Created only when we actually start; closed on stop
    private SlaveGattServer gatt = null;
    private SlaveAdvertiser adv = null;
    private boolean started = false;

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        vb = ActivitySlaveBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.getRoot().setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    top = insets.getInsets(WindowInsets.Type.statusBars()).top;
                }
            }
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                }
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        vb.dialSlave.setTitle("Slave Heading");

        // Safe to keep one provider instance for the Activity’s lifetime
        yawProvider = new YawProvider(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPerms();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopStreaming(); // close BLE + sensors cleanly
    }

    // ---------- Permissions ----------

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= 31) {
            permLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            });
        } else {
            // Some ROMs gate BLE behind Location on ≤ Android 11
            permLauncher.launch(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION });
        }
    }

    private void onPermResult(@NonNull Map<String, Boolean> result) {
        if (Build.VERSION.SDK_INT >= 31) {
            boolean scan = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_SCAN));
            boolean connect = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_CONNECT));
            boolean advertise = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_ADVERTISE));
            if (!scan || !connect || !advertise) {
                vb.tvStatus.setText("Grant Bluetooth permissions to start");
                return;
            }
        }
        startIfReady();
    }

    // ---------- Start/Stop streaming ----------

    private void startIfReady() {
        if (started) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            vb.tvStatus.setText("Bluetooth not supported on this device");
            return;
        }
        if (!adapter.isEnabled()) {
            vb.tvStatus.setText("Enable Bluetooth");
            return;
        }
        if (!hasAllRuntimePerms()) {
            vb.tvStatus.setText("Missing permissions");
            return;
        }

        // (Re)create BLE components fresh each start
        gatt = new SlaveGattServer(this);
        adv = new SlaveAdvertiser(this, new SlaveAdvertiser.Listener() {
            @Override public void onStarted() { runOnUiThread(() -> vb.tvStatus.setText("Advertising + GATT server started")); }
            @Override public void onFailed(int code) { runOnUiThread(() -> vb.tvStatus.setText("Advertise failed: " + code)); }
        });
        boolean ok = adv.start();
        if (!ok) { vb.tvStatus.setText("Advertising unsupported"); return; }


        vb.tvStatus.setText("Advertising + GATT server started");
        yawProvider.start(yaw0to360 -> {
            vb.dialSlave.setAngleDeg0to360(yaw0to360);
            vb.tvYaw.setText(String.format("Yaw: %.1f°", yaw0to360));

            YawPacket pkt = new YawPacket(
                    AngleUtils.degToHundredths(yaw0to360),
                    (int) SystemClock.elapsedRealtime()
            );
            if (gatt != null) gatt.push(pkt);
        });

        started = true;
    }

    private void stopStreaming() {
        if (!started) return;
        started = false;

        yawProvider.stop();

        if (adv != null) {
            adv.stop();
            adv = null;
        }
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
        vb.tvStatus.setText("Stopped");
    }

    // ---------- Helpers ----------

    private boolean hasAllRuntimePerms() {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT < 23;
        }
    }
}
