package com.vanbios.bleadvertiser.bluetooth;


public class BluetoothException extends Exception {

    public BluetoothException(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothException(Throwable throwable) {
        super(throwable);
    }
}
