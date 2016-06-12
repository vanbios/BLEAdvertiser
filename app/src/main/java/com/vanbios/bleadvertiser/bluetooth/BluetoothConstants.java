package com.vanbios.bleadvertiser.bluetooth;

import android.os.ParcelUuid;

/**
 * Constants for use in the Bluetooth Advertisements
 */
public class BluetoothConstants {

    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     * Bluetooth requires a certain format for UUIDs associated with Services.
     */
    public static final ParcelUuid Service_UUID = ParcelUuid.fromString("B206EE5D-17EE-40C1-92BA-462A038A33D2");
    public static final ParcelUuid NAME_UUID = ParcelUuid.fromString("2EFDAD55-5B85-4C78-9DE8-07884DC051FA");
    public static final ParcelUuid ID_UUID = ParcelUuid.fromString("E669893C-F4C2-4604-800A-5252CED237F9");
    public static final ParcelUuid LOCATION_UUID = ParcelUuid.fromString("1EA08229-38D7-4927-98EC-113723C30C1B");

}
