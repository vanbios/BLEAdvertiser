package com.vanbios.bleadvertiser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.vanbios.bleadvertiser.adapter.DevicesRecyclerAdapter;
import com.vanbios.bleadvertiser.bluetooth.BluetoothController;
import com.vanbios.bleadvertiser.bluetooth.BluetoothException;
import com.vanbios.bleadvertiser.object.Device;
import com.vanbios.bleadvertiser.singleton.InfoSingleton;
import com.vanbios.bleadvertiser.util.ToastUtils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String BROADCAST_DEVICES_LIST_UPDATED = "com.vanbios.bleadvertiser.devices.list.broadcast";
    public final static String PARAM_STATUS_DEVICES_LIST_UPDATED = "devices_list_updated";
    public final static int STATUS_DEVICES_LIST_UPDATED = 3;
    private BluetoothManager bluetoothManager;
    private BluetoothController bluetoothController;
    private static final int REQUEST_ENABLE_BT = 11;

    private TextView tvEmptyList;
    private RecyclerView recyclerView;
    private DevicesRecyclerAdapter recyclerAdapter;
    private ArrayList<Device> devicesList;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //BluetoothUtils.setBluetooth(true);

        devicesList = new ArrayList<>();
        initViews();
        makeBroadcastReceiver();
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        bluetoothManager = ((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE));

        if (bluetoothManager != null) {
            if (bluetoothManager.getAdapter().isEnabled())
                initBluetoothController();
            else {
                // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void initBluetoothController() {
        if (bluetoothController == null)
            bluetoothController = new BluetoothController(this, bluetoothManager);
        if (!bluetoothController.hasBluetoothLE())
            ToastUtils.showClosableToast(this, "BLE is not supported!", 2);
        try {
            bluetoothController.initBLE();
            bluetoothController.start();
        } catch (BluetoothException e) {
            ToastUtils.showClosableToast(this, "BLE is not supported!", 2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothController != null) bluetoothController.stop();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(bluetoothStateReceiver);
    }

    private void initViews() {
        tvEmptyList = (TextView) findViewById(R.id.tvDevicesListEmpty);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerDevicesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView != null ? recyclerView.getContext() : null));
        recyclerAdapter = new DevicesRecyclerAdapter(devicesList, this);
        recyclerView.setAdapter(recyclerAdapter);
        setVisibility();
    }

    private void makeBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(PARAM_STATUS_DEVICES_LIST_UPDATED, 0);
                switch (status) {
                    case STATUS_DEVICES_LIST_UPDATED: {
                        devicesList.clear();
                        devicesList.addAll(InfoSingleton.getInstance().getDevicesList());
                        recyclerAdapter.notifyDataSetChanged();
                        setVisibility();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BROADCAST_DEVICES_LIST_UPDATED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setVisibility() {
        recyclerView.setVisibility(devicesList.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyList.setVisibility(devicesList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int bluetoothStatus = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (bluetoothStatus == BluetoothAdapter.STATE_OFF) {
                    if (bluetoothController != null) bluetoothController.stop();
                } else if (bluetoothStatus == BluetoothAdapter.STATE_ON) {
                    initBluetoothController();
                    if (bluetoothManager != null) {
                        if (bluetoothManager.getAdapter().isEnabled() && bluetoothController != null)
                            bluetoothController.start();
                    }
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            initBluetoothController();
        } else {
            // User declined to enable Bluetooth, exit the app.
            finish();
        }
    }
}
