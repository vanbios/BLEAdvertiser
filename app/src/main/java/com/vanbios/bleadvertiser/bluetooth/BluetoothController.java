package com.vanbios.bleadvertiser.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.vanbios.bleadvertiser.MainActivity;
import com.vanbios.bleadvertiser.R;
import com.vanbios.bleadvertiser.object.Device;
import com.vanbios.bleadvertiser.singleton.InfoSingleton;
import com.vanbios.bleadvertiser.util.ToastUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BluetoothController {

    private static final String TAG = BluetoothController.class.getSimpleName();
    private boolean scanning = false;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private final Map<String, BluetoothGatt> gattMap = new HashMap<>();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Context context;


    public BluetoothController(Context context, BluetoothManager bluetoothManager) {
        this.context = context;
        this.bluetoothManager = bluetoothManager;
    }


    private class MyBluetoothGattCallback extends BluetoothGattCallback {
        private UUID userID;
        private BluetoothGattService service;

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            executor.execute(new Runnable() {
                public void run() {
                    Log.d(TAG, "onConnectionStateChange received: " + status + " newState = " + BluetoothUtils.getConnectionState(newState));
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connectNewGatt(gatt.getDevice());
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            gatt.discoverServices();
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "device disconnected " + gatt.getDevice().getAddress());
                        InfoSingleton.getInstance().removeDevice(gatt.getDevice().getAddress());
                        sendBroadcast();
                        synchronized (gattMap) {
                            gattMap.remove(gatt.getDevice().getAddress());
                        }
                    }
                }
            });
            //super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.d(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (final BluetoothGattService service : gatt.getServices()) {
                    this.service = service;
                    executor.execute(new Runnable() {
                        public void run() {
                            UUID uuid = service.getUuid();
                            if (BluetoothConstants.Service_UUID.getUuid().equals(uuid)) {
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothConstants.LOCATION_UUID.getUuid());
                                gatt.readCharacteristic(characteristic);
                                gatt.setCharacteristicNotification(characteristic, true);
                                characteristic = service.getCharacteristic(BluetoothConstants.NAME_UUID.getUuid());
                                gatt.readCharacteristic(characteristic);
                                characteristic = service.getCharacteristic(BluetoothConstants.ID_UUID.getUuid());
                                gatt.readCharacteristic(characteristic);
                            }
                        }
                    });
                }
            }
            // super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         final int status) {
            executor.execute(new Runnable() {
                public void run() {
                    Log.d(TAG, "onCharacteristicRead " + characteristic.getUuid() + " status: " + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (characteristic.getUuid().equals(BluetoothConstants.LOCATION_UUID.getUuid())) {
                            try {
                                BluetoothGattCharacteristic characteristicToSend = service.getCharacteristic(BluetoothConstants.ID_UUID.getUuid());
                                gatt.readCharacteristic(characteristicToSend);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (characteristic.getUuid().equals(BluetoothConstants.ID_UUID.getUuid())) {
                            try {
                                userID = UuidUtils.asUuid(characteristic.getValue());
                                Log.e(TAG, "user ID " + userID + " " + userID.toString());
                                BluetoothGattCharacteristic characteristicToSend = service.getCharacteristic(BluetoothConstants.NAME_UUID.getUuid());
                                gatt.readCharacteristic(characteristicToSend);
                                //connectNewGatt(gatt.getDevice());
                                Device device = new Device(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                                device.setId(userID.toString());
                                InfoSingleton.getInstance().setId(device);
                                sendBroadcast();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (characteristic.getUuid().equals(BluetoothConstants.NAME_UUID.getUuid())) {
                            try {
                                Log.e(TAG, "user  " + Arrays.toString(characteristic.getValue()));
                                // connectNewGatt(gatt.getDevice());
                                Device device = new Device(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                                device.setName(new String(characteristic.getValue()));
                                InfoSingleton.getInstance().setName(device);
                                sendBroadcast();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            // super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            executor.execute(new Runnable() {
                public void run() {
                    Log.w(TAG, "onCharacteristicChanged uuid: " + characteristic.getUuid());

                    if (characteristic.getUuid().equals(BluetoothConstants.NAME_UUID.getUuid())) {
                        try {
                            Device device = new Device(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                            device.setName(new String(characteristic.getValue()));
                            InfoSingleton.getInstance().setName(device);
                            sendBroadcast();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (characteristic.getUuid().equals(BluetoothConstants.ID_UUID.getUuid())) {
                        try {
                            UUID userID = UuidUtils.asUuid(characteristic.getValue());
                            Log.e(TAG, "user ID " + userID + " " + userID.toString());

                            Device device = new Device(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                            device.setId(userID.toString());
                            InfoSingleton.getInstance().setId(device);
                            sendBroadcast();
                        } catch (Exception e) {
                            Log.e(TAG, "Something wrong while reading ID_UUID ", e);
                        }
                    }
                }
            });
            // super.onCharacteristicChanged(gatt, characteristic);
        }
    }


    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private SampleAdvertiseCallback advertiseCallback;
    private BluetoothGattServer gattServer;
    private BluetoothLeScanner bluetoothLeScanner;


    public boolean hasBluetoothLE() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    public void initBLE() throws BluetoothException {
        Log.d(TAG, "init BLE");
        gattMap.clear();
        deviceLastScan.clear();
        InfoSingleton.getInstance().removeAllDevices();
        if (bluetoothAdapter == null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            Log.d(TAG, "init BLE bluetoothAdapter");
        }
        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            Log.d(TAG, "init BLE bluetoothLeScanner");
        }
        if (bluetoothAdapter == null) {
            throw new BluetoothException(context.getString(R.string.error_bluetooth_not_supported));
        }
        if (bluetoothLeAdvertiser == null) {
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            Log.d(TAG, "init BLE bluetoothLeAdvertiser");
        }
    }

    public void startScan() {
        if (bluetoothManager.getAdapter().isEnabled()) {
            stopScan();
            ScanSettings.Builder b = new ScanSettings.Builder();
            b.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            ScanSettings settings = b.build();
            ScanFilter.Builder sfb = new ScanFilter.Builder();
            sfb.setServiceUuid(BluetoothConstants.Service_UUID);
            List<ScanFilter> filter = new ArrayList<>();
            filter.add(sfb.build());
            bluetoothLeScanner.startScan(filter, settings, leScanCallback);
            scanning = true;
            Log.d(TAG, "Scan started");
        } else
            ToastUtils.showClosableToast(context, "Bluetooth is not turned on. Please turn on bluetooth", 2);
    }

    public void stopScan() {
        if (bluetoothManager.getAdapter().isEnabled()) {
            if (scanning) {
                Log.d(TAG, "stop scan");
                bluetoothLeScanner.stopScan(leScanCallback);
                scanning = false;
            }
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            foundDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "scan failed: " + errorCode);
        }
    };

    private final HashMap<String, BluetoothGatt> deviceLastScan = new HashMap<>();

    void foundDevice(BluetoothDevice device) {
        synchronized (gattMap) {
            BluetoothGatt gatt = gattMap.get(device.getAddress());
            if (gatt == null) {
                Log.i(TAG, "Found device " + device.getName() + " = " + device.getAddress());
                if (!deviceLastScan.containsKey(device.getAddress())) {
                    BluetoothGatt gattNew = connectNewGatt(device);
                    if (gattNew != null) {
                        deviceLastScan.put(device.getAddress(), gattNew);
                    }
                }
            }
        }
    }

    private BluetoothGatt connectNewGatt(BluetoothDevice device) {
        if (gattMap.get(device.getAddress()) == null) {
            Log.d(TAG, "connectNewGatt" + " " + device.getAddress());
            BluetoothGatt gatt = device.connectGatt(context, true, new MyBluetoothGattCallback());
            gattMap.put(device.getAddress(), gatt);
            boolean refresh = refreshDeviceCache(gatt);
            Log.d(TAG, refresh ? "device cash refreshed" : "device cash is not refreshed");
            InfoSingleton.getInstance().putDevice(new Device(device.getName(), device.getAddress()));
            sendBroadcast();

            Log.d(TAG, "gattMap count" + " " + gattMap.size());
            return gatt;
        }
        return null;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    /**
     * Starts BLE Advertising.
     */
    public void startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising");

        if (advertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            advertiseCallback = new SampleAdvertiseCallback();

            if (bluetoothLeAdvertiser != null)
                bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
        }
    }

    /**
     * Stops BLE Advertising.
     */
    public void stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising");

        if (gattServer != null) {
            gattServer.close();
            Log.d(TAG, "Gatt Closed");
        }

        if (bluetoothLeAdvertiser != null) {
            Log.d(TAG, "callback:" + advertiseCallback);
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
        }
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(BluetoothConstants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);
        //dataBuilder.setIncludeTxPowerLevel(true);
        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTimeout(0);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return settingsBuilder.build();
    }

    public void start() {
        if (bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.d(TAG, "Advertisement is supported!!!");
            startAdvertising();
        } else {
            Log.d(TAG, "Advertisement is not supported by this device!!!");
        }
        startScan();
    }

    public void stop() {
        stopScan();
        synchronized (deviceLastScan) {
            for (BluetoothGatt bleGatt : deviceLastScan.values()) {
                Log.d(TAG, "disconnect" + bleGatt.getDevice().getAddress());
                bleGatt.close();
            }
        }
        if (bluetoothAdapter.isMultipleAdvertisementSupported()) {
            stopAdvertising();
        }
        InfoSingleton.getInstance().removeAllDevices();
        sendBroadcast();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed " + errorCode);
            ToastUtils.showClosableToast(context, "Advertising failed", 2);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started " + settingsInEffect.toString());
            startGattServer();
        }
    }

    private void startGattServer() {
        gattServer = bluetoothManager.openGattServer(context, new MyBluetoothGattServerCallback());
        addDeviceInfoService();
    }

    private void addDeviceInfoService() {
        BluetoothGattService deviceInfoService = new BluetoothGattService(
                BluetoothConstants.Service_UUID.getUuid(),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic nameCharacteristic = new BluetoothGattCharacteristic(
                BluetoothConstants.NAME_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        nameCharacteristic.setValue(BluetoothUtils.getUsername(context));
        deviceInfoService.addCharacteristic(nameCharacteristic);

        BluetoothGattCharacteristic idCharacteristic = new BluetoothGattCharacteristic(
                BluetoothConstants.ID_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        idCharacteristic.setValue(UuidUtils.asBytes(UUID.randomUUID()));
        deviceInfoService.addCharacteristic(idCharacteristic);

        BluetoothGattCharacteristic locCharacteristic = new BluetoothGattCharacteristic(
                BluetoothConstants.LOCATION_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        double latitude = 50.232323;
        double longitude = 50.232323;
        locCharacteristic.setValue(BluetoothUtils.coordinateAsBytes(latitude, longitude));
        deviceInfoService.addCharacteristic(locCharacteristic);
        gattServer.addService(deviceInfoService);
    }


    private class MyBluetoothGattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onCharacteristicReadRequest(final BluetoothDevice device,
                                                final int requestId,
                                                final int offset,
                                                final BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "MyBluetoothGattServerCallback.onCharacteristicReadRequest from " + device.getName() + " / " + device.getAddress());

            byte[] src = characteristic.getValue();
            int len = src.length - offset;
            byte[] dest = new byte[len];
            System.arraycopy(src, offset, dest, 0, len);
            src = dest;

            Log.d(TAG, String.format("offset %d length %d data: %s", offset, len, new String(src)));
            if (gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, src)) {
                Log.d(TAG, "Characteristic " + characteristic.getUuid().toString() + " has been sent");
            }
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }
    }

    private void sendBroadcast() {
        Intent intent = new Intent(MainActivity.BROADCAST_DEVICES_LIST_UPDATED);
        intent.putExtra(MainActivity.PARAM_STATUS_DEVICES_LIST_UPDATED, MainActivity.STATUS_DEVICES_LIST_UPDATED);
        context.sendBroadcast(intent);
    }
}
