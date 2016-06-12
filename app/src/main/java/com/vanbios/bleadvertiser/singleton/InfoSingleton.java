package com.vanbios.bleadvertiser.singleton;


import com.vanbios.bleadvertiser.object.Device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class InfoSingleton {

    private static volatile InfoSingleton instance;
    private HashMap<String, Device> devicesMap;
    private HashSet<String> idUuidSet;

    private InfoSingleton() {
        devicesMap = new HashMap<>();
        idUuidSet = new HashSet<>();
    }

    public void putDevice(Device device) {
        if (devicesMap.get(device.getDeviceAddress()) == null)
            devicesMap.put(device.getDeviceAddress(), device);
    }

    public void removeDevice(String deviceAddress) {
        Device device = devicesMap.remove(deviceAddress);
        if (device != null && device.getId().length() > 0)
            idUuidSet.remove(device.getId());
    }

    public void removeAllDevices() {
        devicesMap.clear();
        idUuidSet.clear();
    }

    public ArrayList<Device> getDevicesList() {
        ArrayList<Device> devices = new ArrayList<>();
        for (Device device : devicesMap.values())
            if (isDeviceShown(device)) devices.add(device);
        ArrayList<Device> result = new ArrayList<>();
        for (String id : idUuidSet) {
            for (Device device : devices) {
                if (id.equals(device.getId())) {
                    result.add(device);
                    break;
                }
            }
        }
        return result;
    }

    private boolean isDeviceShown(Device device) {
        return device.getDeviceName().length() > 0
                && device.getDeviceAddress().length() > 0
                && device.getName().length() > 0
                && device.getId().length() > 0;
    }

    public HashSet<String> getIdUuidSet() {
        return new HashSet<>(idUuidSet);
    }

    public void setId(Device device) {
        Device deviceFromMap = devicesMap.get(device.getDeviceAddress());
        if (deviceFromMap != null) {
            String oldId = deviceFromMap.getId();
            if (oldId.length() > 0)
                idUuidSet.remove(oldId);
            deviceFromMap.setId(device.getId());
            idUuidSet.add(device.getId());
        }
    }

    public void setName(Device device) {
        Device deviceFromMap = devicesMap.get(device.getDeviceAddress());
        if (deviceFromMap != null)
            deviceFromMap.setName(device.getName());
    }


    public static InfoSingleton getInstance() {
        InfoSingleton localInstance = instance;
        if (localInstance == null) {
            synchronized (InfoSingleton.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new InfoSingleton();
                }
            }
        }
        return localInstance;
    }
}
