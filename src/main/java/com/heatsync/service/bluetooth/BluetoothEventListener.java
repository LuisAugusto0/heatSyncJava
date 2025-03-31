package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;

/**
 * Interface to notify the UI about Bluetooth events.
 */
public interface BluetoothEventListener {
    /**
     * Called when a Bluetooth device is discovered.
     * 
     * @param peripheral The peripheral device
     * @param name The device name
     * @param address The MAC address of the device
     * @param rssi The signal strength (RSSI)
     */
    void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi);
    
    /**
     * Called when a device is successfully connected.
     * 
     * @param peripheral The connected device
     */
    void onDeviceConnected(BluetoothPeripheral peripheral);
    
    /**
     * Called when a device is disconnected.
     * 
     * @param peripheral The disconnected device
     * @param status The disconnection status
     */
    void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status);
    
    /**
     * Called when a scan failure occurs.
     * 
     * @param errorCode The error code
     */
    void onScanFailed(int errorCode);
} 