package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for scanning and discovering Bluetooth devices.
 */
public class BluetoothDeviceScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothDeviceScanner.class);
    
    private final BluetoothCentralManager centralManager;
    private final List<BluetoothPeripheral> discoveredPeripherals = new ArrayList<>();
    private final BluetoothEventListener eventListener;
    
    private boolean isScanning = false;
    private int minRssi = -100; // Default value (practically no filtering)
    
    /**
     * Creates a new device scanner.
     * 
     * @param centralManager The Bluetooth central manager
     * @param eventListener The listener for Bluetooth events
     */
    public BluetoothDeviceScanner(BluetoothCentralManager centralManager, BluetoothEventListener eventListener) {
        this.centralManager = centralManager;
        this.eventListener = eventListener;
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
     * Starts the discovery of BLE devices.
     * 
     * @return true if scanning was started, false otherwise
     */
    public boolean startDeviceDiscovery() {
        if (centralManager != null) {
            if (isScanning) {
                LOGGER.debug("Scan already in progress. Stopping previous one.");
                stopDeviceDiscovery();
            }
            LOGGER.info("Attempting to start BLE scan...");
            centralManager.scanForPeripherals();
            isScanning = true;
            LOGGER.info("Scan initiated.");
            return true;
        } else {
            LOGGER.error("Cannot start discovery, BluetoothCentralManager is not initialized.");
            return false;
        }
    }
    
    /**
     * Stops the discovery of BLE devices.
     */
    public void stopDeviceDiscovery() {
        if (centralManager != null) {
            if (isScanning) {
                LOGGER.info("Stopping BLE scan...");
                centralManager.stopScan();
                isScanning = false;
            } else {
                LOGGER.debug("Scan not active, no need to stop.");
            }
        } else {
            LOGGER.warn("Cannot stop scan, BluetoothCentralManager is not initialized.");
        }
    }
    
    /**
     * Clears the list of discovered devices.
     */
    public void clearDiscoveredDevices() {
        discoveredPeripherals.clear();
    }
    
    /**
     * Returns a list of discovered devices.
     * 
     * @return List of BLE devices found during scanning
     */
    public List<BluetoothPeripheral> getDiscoveredDevices() {
        return new ArrayList<>(discoveredPeripherals); // Returns a copy to avoid concurrency issues
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
     * @param peripheral The peripheral device
     * @param name The device name
     * @param address The MAC address of the device
     * @param rssi The signal strength (RSSI)
     */
    public void processDiscoveredDevice(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        // Check if the device meets the RSSI filter criteria
        if (rssi < minRssi) {
            LOGGER.debug("Device filtered out due to weak signal: {} ({}) RSSI: {}dBm", 
                (name != null && !name.isEmpty()) ? name : "Unknown", address, rssi);
            return;
        }
        
        // Check device information
        LOGGER.debug("Discovered device - Address: {}, Raw name: {}, RSSI: {}", address, name, rssi);
        
        // If the name is empty, use the address as identification
        if (name == null || name.isEmpty()) {
            name = "Device " + address;
        }
        
        // Check if we already have this device in the list
        boolean isNewDevice = true;
        for (int i = 0; i < discoveredPeripherals.size(); i++) {
            BluetoothPeripheral existingPeripheral = discoveredPeripherals.get(i);
            if (existingPeripheral.getAddress().equals(address)) {
                // Device is already in the list, replace with the most recent one
                discoveredPeripherals.set(i, peripheral);
                isNewDevice = false;
                LOGGER.debug("Updated existing device in list: {} ({})", name, address);
                break;
            }
        }
        
        // Add to the list if it's a new device
        if (isNewDevice) {
            discoveredPeripherals.add(peripheral);
            LOGGER.info("Discovered new peripheral: {} ({}) RSSI: {}dBm", name, address, rssi);
        }
        
        // Notify UI if the listener is registered
        if (eventListener != null) {
            eventListener.onDeviceDiscovered(peripheral, name, address, rssi);
        }
    }
} 