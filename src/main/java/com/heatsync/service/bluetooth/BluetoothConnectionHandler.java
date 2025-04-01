package com.heatsync.service.bluetooth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for managing connections with Bluetooth devices.
 */
public class BluetoothConnectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothConnectionHandler.class);
    
    private final BluetoothManager bluetoothManager;
    private final BluetoothEventListener eventListener;
    
    private RemoteDevice connectedDevice = null;
    private StreamConnection streamConnection = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private boolean connected = false;
    
    // Executor for async operations
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Thread readThread;
    private volatile boolean keepReading = false;
    
    /**
     * Creates a new Bluetooth connection handler.
     * 
     * @param bluetoothManager The Bluetooth manager
     * @param eventListener The listener for Bluetooth events
     */
    public BluetoothConnectionHandler(BluetoothManager bluetoothManager, BluetoothEventListener eventListener) {
        this.bluetoothManager = bluetoothManager;
        this.eventListener = eventListener;
    }
    
    /**
     * Attempts to connect to a specific device by MAC address.
     * 
     * @param deviceAddress MAC address of the device
     * @return true if the connection attempt was initiated, false otherwise
     */
    public boolean connectToDevice(String deviceAddress) {
        if (bluetoothManager == null) {
            LOGGER.error("Cannot connect, BluetoothManager not initialized.");
            return false;
        }
        
        return bluetoothManager.connectToDevice(deviceAddress);
    }
    
    /**
     * Handles the event of connection with a device.
     * 
     * @param device The connected device
     */
    public void handleDeviceConnected(RemoteDevice device) {
        connected = true;
        connectedDevice = device;
        
        LOGGER.info("Successfully connected to device: {}", device.getBluetoothAddress());
        
        if (eventListener != null) {
            eventListener.onDeviceConnected(device);
        }
    }
    
    /**
     * Handles the event of disconnection from a device.
     * 
     * @param device The disconnected device
     * @param status The status of the disconnection
     */
    public void handleDeviceDisconnected(RemoteDevice device, int status) {
        connected = false;
        connectedDevice = null;
        
        LOGGER.info("Disconnected from device: {} (status: {})", device.getBluetoothAddress(), status);
        
        if (eventListener != null) {
            eventListener.onDeviceDisconnected(device, status);
        }
    }
    
    /**
     * Closes the connection with the current peripheral.
     */
    public void closeConnection() {
        if (connected && bluetoothManager != null) {
            LOGGER.info("Closing connection to device...");
            bluetoothManager.closeConnection();
            connected = false;
            connectedDevice = null;
        } else {
            LOGGER.info("No active connection to close.");
        }
    }
    
    /**
     * Checks if connected to a peripheral.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && connectedDevice != null;
    }
    
    /**
     * Gets the currently connected peripheral.
     * 
     * @return The connected peripheral or null if there is no connection
     */
    public RemoteDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * Sends data to the connected device.
     * 
     * @param data The data to send
     * @return true if sending was successful, false otherwise
     */
    public boolean sendData(byte[] data) {
        if (!connected || bluetoothManager == null) {
            LOGGER.error("Cannot send data, not connected to any device.");
            return false;
        }
        
        try {
            // Use the BluetoothManager to send the data
            OutputStream output = bluetoothManager.getOutputStream();
            if (output != null) {
                output.write(data);
                output.flush();
                return true;
            } else {
                LOGGER.error("Output stream is null, cannot send data.");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Error sending data to device", e);
            return false;
        }
    }
} 