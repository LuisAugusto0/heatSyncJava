package com.heatsync.service.bluetooth;

/**
 * Interface to notify the UI about Bluetooth events.
 */
public interface BluetoothEventListener extends BluetoothDataListener {
    /**
     * Called when a Bluetooth device is discovered.
     * 
     * @param deviceObj The discovered device object
     * @param name The device name
     * @param address The MAC address of the device
     * @param rssi The signal strength (RSSI)
     */
    void onDeviceDiscovered(Object deviceObj, String name, String address, int rssi);
    
    /**
     * Called when a device is successfully connected.
     * 
     * @param deviceObj The connected device object
     */
    void onDeviceConnected(Object deviceObj);
    
    /**
     * Called when a device is disconnected.
     * 
     * @param deviceObj The disconnected device object
     * @param status The disconnection status
     */
    void onDeviceDisconnected(Object deviceObj, int status);
    
    /**
     * Called when a scan failure occurs.
     * 
     * @param errorCode The error code
     */
    void onScanFailed(int errorCode);
}