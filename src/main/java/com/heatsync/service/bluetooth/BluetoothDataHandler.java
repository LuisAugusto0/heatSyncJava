package com.heatsync.service.bluetooth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.RemoteDevice;
import java.io.IOException;

/**
 * Responsible for data transfer between the application and Bluetooth devices.
 */
public class BluetoothDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothDataHandler.class);
    
    // UUIDs for services and characteristics (to be implemented)
    // private static final UUID COOLER_SERVICE_UUID = UUID.fromString("..."); 
    // private static final UUID TEMP_CHARACTERISTIC_UUID = UUID.fromString("...");
    // private static final UUID RPM_CHARACTERISTIC_UUID = UUID.fromString("...");
    // private static final UUID PWM_CHARACTERISTIC_UUID = UUID.fromString("...");
    
    private final BluetoothConnectionHandler connectionHandler;
    
    /**
     * Creates a new Bluetooth data handler.
     * 
     * @param connectionHandler The Bluetooth connection handler
     */
    public BluetoothDataHandler(BluetoothConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }
    
    /**
     * Sends temperature data to the connected peripheral.
     * 
     * @param cpuTemp CPU temperature
     * @param gpuTemp GPU temperature
     * @param diskTemp Disk temperature
     * @return true if the data was sent, false otherwise
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        if (!connectionHandler.isConnected()) {
            LOGGER.warn("Cannot send data: Not connected to a device.");
            return false;
        }
        
        RemoteDevice device = connectionHandler.getConnectedDevice();
        if (device == null) {
            LOGGER.warn("Cannot send data: Connected device is null.");
            return false;
        }
        
        LOGGER.info("Sending temperature data: CPU={}, GPU={}, Disk={}", cpuTemp, gpuTemp, diskTemp);
        
        // Format the temperature data as a string for SPP
        // T:CPU:GPU:DISK\n format (easily parseable by Arduino or similar)
        String data = String.format("T:%.1f:%.1f:%.1f\n", cpuTemp, gpuTemp, diskTemp);
        
        return connectionHandler.sendData(data.getBytes());
    }
    
    /**
     * Sends a PWM value to the connected peripheral.
     * 
     * @param pwmValue PWM value (e.g., 0-100)
     * @return true if the command was sent, false otherwise
     */
    public boolean sendPwmCommand(int pwmValue) {
        if (!connectionHandler.isConnected()) {
            LOGGER.warn("Cannot send PWM command: Not connected.");
            return false;
        }
        
        RemoteDevice device = connectionHandler.getConnectedDevice();
        if (device == null) {
            LOGGER.warn("Cannot send PWM command: Connected device is null.");
            return false;
        }
        
        LOGGER.info("Sending PWM command: {}", pwmValue);
        
        // Format the PWM command as a string for SPP
        // P:VALUE\n format (easily parseable by Arduino or similar)
        String command = String.format("P:%d\n", pwmValue);
        
        return connectionHandler.sendData(command.getBytes());
    }
    
    /**
     * Gets the name of the connected device.
     * 
     * @return The device name or null if not connected
     */
    public String getConnectedDeviceName() {
        if (!connectionHandler.isConnected()) {
            return null;
        }
        
        RemoteDevice device = connectionHandler.getConnectedDevice();
        if (device == null) {
            return null;
        }
        
        try {
            return device.getFriendlyName(false);
        } catch (IOException e) {
            LOGGER.error("Error getting device name", e);
            return device.getBluetoothAddress();
        }
    }
    
    /**
     * Gets the address of the connected device.
     * 
     * @return The device address or null if not connected
     */
    public String getConnectedDeviceAddress() {
        if (!connectionHandler.isConnected()) {
            return null;
        }
        
        RemoteDevice device = connectionHandler.getConnectedDevice();
        if (device == null) {
            return null;
        }
        
        return device.getBluetoothAddress();
    }
} 