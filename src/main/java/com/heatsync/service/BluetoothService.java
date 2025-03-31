package com.heatsync.service;

import com.heatsync.service.bluetooth.BluetoothEventListener;
import com.heatsync.service.bluetooth.BluetoothManager;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade for the Bluetooth service.
 * Delegates operations to the underlying Bluetooth manager.
 */
public class BluetoothService implements BluetoothEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothService.class);
    
    private final BluetoothManager bluetoothManager;
    private BluetoothEventListener eventListener;
    
    /**
     * Default constructor. Initializes the BluetoothManager.
     */
    public BluetoothService() {
        LOGGER.info("Initializing BluetoothService with Blessed-Bluez...");
        bluetoothManager = new BluetoothManager();
        bluetoothManager.setEventListener(this);
    }
    
    /**
     * Registers a listener to receive Bluetooth events.
     * @param listener The listener to be registered.
     */
    public void setBluetoothEventListener(BluetoothEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Sets a minimum RSSI value to filter out devices with weak signals.
     * @param rssiValue The minimum RSSI value to be considered (typically between -100 and 0).
     */
    public void setMinimumRssi(int rssiValue) {
        bluetoothManager.setMinimumRssi(rssiValue);
    }
    
    /**
     * Starts the discovery of BLE devices.
     * @return true if scanning was started, false otherwise.
     */
    public boolean startDeviceDiscovery() {
        return bluetoothManager.startDeviceDiscovery();
    }
    
    /**
     * Stops the discovery of BLE devices.
     */
    public void stopDeviceDiscovery() {
        bluetoothManager.stopDeviceDiscovery();
    }
    
    /**
     * Attempts to connect to a specific device by MAC address.
     * @param deviceAddress MAC address of the device.
     * @return true if the connection attempt was initiated, false otherwise.
     */
    public boolean connectToDevice(String deviceAddress) {
        return bluetoothManager.connectToDevice(deviceAddress);
    }
    
    /**
     * Sends temperature data to the connected peripheral.
     * @param cpuTemp CPU temperature.
     * @param gpuTemp GPU temperature.
     * @param diskTemp Disk temperature.
     * @return true if the data was sent, false otherwise.
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        return bluetoothManager.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
    }
    
    /**
     * Sends a PWM value to the connected peripheral.
     * @param pwmValue PWM value (e.g., 0-100).
     * @return true if the command was sent, false otherwise.
     */
    public boolean sendPwmCommand(int pwmValue) {
        return bluetoothManager.sendPwmCommand(pwmValue);
    }
    
    /**
     * Closes the connection with the current peripheral.
     */
    public void closeConnection() {
        bluetoothManager.closeConnection();
    }
    
    /**
     * Checks if connected to a peripheral.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return bluetoothManager.isConnected();
    }
    
    /**
     * Checks if the Bluetooth service was correctly initialized.
     * @return true if initialized, false otherwise.
     */
    public boolean isInitialized() {
        return bluetoothManager.isInitialized();
    }
    
    /**
     * Checks if scanning is in progress.
     * @return true if scanning, false otherwise.
     */
    public boolean isScanning() {
        return bluetoothManager.isScanning();
    }
    
    /**
     * Shuts down the Bluetooth service, stopping scans and disconnecting.
     */
    public void shutdown() {
        bluetoothManager.shutdown();
    }
    
    // Implementation of BluetoothEventListener interface methods
    @Override
    public void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        if (eventListener != null) {
            eventListener.onDeviceDiscovered(peripheral, name, address, rssi);
        }
    }

    @Override
    public void onDeviceConnected(BluetoothPeripheral peripheral) {
        if (eventListener != null) {
            eventListener.onDeviceConnected(peripheral);
        }
    }

    @Override
    public void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        if (eventListener != null) {
            eventListener.onDeviceDisconnected(peripheral, status);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (eventListener != null) {
            eventListener.onScanFailed(errorCode);
        }
    }
}
