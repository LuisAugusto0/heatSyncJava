package com.heatsync.service.bluetooth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.bluetooth.RemoteDevice;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for scanning and discovering Bluetooth devices.
 */
public class BluetoothDeviceScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothDeviceScanner.class);
    
    private final BluetoothManager bluetoothManager;
    private final Map<String, RemoteDevice> discoveredDevices = new HashMap<>();
    private final BluetoothEventListener eventListener;
    
    private boolean isScanning = false;
    private int minRssi = -100; // Default value (practically no filtering)
    
    /**
     * Creates a new device scanner.
     * 
     * @param bluetoothManager The Bluetooth manager
     * @param eventListener The listener for Bluetooth events
     */
    public BluetoothDeviceScanner(BluetoothManager bluetoothManager, BluetoothEventListener eventListener) {
        this.bluetoothManager = bluetoothManager;
        this.eventListener = eventListener;
        
        // Register this scanner to receive events from the Bluetooth manager
        if (bluetoothManager != null) {
            bluetoothManager.setEventListener(eventListener);
        }
    }
    
    /**
     * Sets a minimum RSSI value to filter out devices with weak signals.
     * Values closer to zero are stronger signals (e.g., -50 is stronger than -80).
     * 
     * @param rssiValue The minimum RSSI value to be considered (typically between -100 and 0)
     */
    public void setMinimumRssi(int rssiValue) {
        this.minRssi = rssiValue;
        LOGGER.info("Minimum RSSI filter set to {} dBm", rssiValue);
        
        // Pass the RSSI filter to the Bluetooth manager
        if (bluetoothManager != null) {
            bluetoothManager.setMinimumRssi(rssiValue);
        }
    }
    
    /**
     * Gets the configured minimum RSSI value for filtering.
     * 
     * @return The current RSSI filter value
     */
    public int getMinimumRssi() {
        return minRssi;
    }
    
    /**
     * Starts the discovery of Bluetooth devices.
     * 
     * @return true if scanning was started, false otherwise
     */
    public boolean startDeviceDiscovery() {
        if (bluetoothManager != null) {
            if (isScanning) {
                LOGGER.debug("Scan already in progress. Stopping previous one.");
                stopDeviceDiscovery();
            }
            LOGGER.info("Attempting to start Bluetooth device discovery...");
            boolean success = bluetoothManager.startDeviceDiscovery();
            isScanning = success;
            if (success) {
                LOGGER.info("Bluetooth device discovery initiated.");
            } else {
                LOGGER.error("Failed to start Bluetooth device discovery.");
            }
            return success;
        } else {
            LOGGER.error("Cannot start discovery, BluetoothManager is not initialized.");
            return false;
        }
    }
    
    /**
     * Stops the discovery of Bluetooth devices.
     */
    public void stopDeviceDiscovery() {
        if (bluetoothManager != null) {
            if (isScanning) {
                LOGGER.info("Stopping Bluetooth device discovery...");
                bluetoothManager.stopDeviceDiscovery();
                isScanning = false;
            } else {
                LOGGER.debug("Scan not active, no need to stop.");
            }
        } else {
            LOGGER.warn("Cannot stop scan, BluetoothManager is not initialized.");
        }
    }
    
    /**
     * Clears the list of discovered devices.
     */
    public void clearDiscoveredDevices() {
        discoveredDevices.clear();
    }
    
    /**
     * Returns a list of discovered devices.
     * 
     * @return List of Bluetooth devices found during scanning
     */
    public List<RemoteDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices.values()); // Returns a copy to avoid concurrency issues
    }
    
    /**
     * Checks if scanning is in progress.
     * 
     * @return true if scanning, false otherwise
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Processes a discovered device.
     * 
     * @param device The remote device
     * @param name The device name
     * @param address The MAC address of the device
     * @param rssi The signal strength (RSSI)
     */
    public void processDiscoveredDevice(RemoteDevice device, String name, String address, int rssi) {
        // Check if the device meets the RSSI filter criteria
        if (rssi < minRssi) {
            LOGGER.debug("Device filtered out due to weak signal: {} ({}) RSSI: {}dBm", 
                (name != null && !name.isEmpty()) ? name : "Unknown", address, rssi);
            return;
        }
        
        // Check device information
        LOGGER.debug("Discovered device - Address: {}, Raw name: {}, RSSI: {}", address, name, rssi);
        
        // If the name is empty, try to get it from the device or use the address as identification
        if (name == null || name.isEmpty()) {
            try {
                name = device.getFriendlyName(false);
            } catch (IOException e) {
                LOGGER.warn("Could not retrieve device name", e);
                name = "Device " + address;
            }
        }
        
        // Store in the map
        discoveredDevices.put(address, device);
        LOGGER.info("Discovered device: {} ({}) RSSI: {}dBm", name, address, rssi);
        
        // Notify UI if the listener is registered
        if (eventListener != null) {
            eventListener.onDeviceDiscovered(device, name, address, rssi);
        }
    }
} 