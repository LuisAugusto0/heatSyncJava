package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class that coordinates all Bluetooth components.
 */
public class BluetoothManager implements BluetoothEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothManager.class);
    
    private BluetoothCentralManager centralManager;
    private BluetoothDeviceScanner deviceScanner;
    private BluetoothConnectionHandler connectionHandler;
    private BluetoothDataHandler dataHandler;
    
    private BluetoothEventListener externalEventListener;
    
    /**
     * Callback to manage events from the Bluetooth Central Manager.
     */
    private final BluetoothCentralManagerCallback centralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            String name = peripheral.getName();
            String address = peripheral.getAddress();
            int rssi = scanResult.getRssi();
            
            // Process the discovered device
            deviceScanner.processDiscoveredDevice(peripheral, name, address, rssi);
        }

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            LOGGER.info("Connected to peripheral: {}", peripheral.getAddress());
            connectionHandler.handleDeviceConnected(peripheral);
            
            // Notify the external listener
            if (externalEventListener != null) {
                externalEventListener.onDeviceConnected(peripheral);
            }
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            LOGGER.warn("Disconnected from peripheral: {} with status {}", peripheral.getAddress(), status);
            connectionHandler.handleDeviceDisconnected(peripheral, status);
            
            // Notify the external listener
            if (externalEventListener != null) {
                externalEventListener.onDeviceDisconnected(peripheral, status);
            }
        }

        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            LOGGER.error("Connection failed for peripheral: {} with status {}", peripheral.getAddress(), status);
            connectionHandler.handleConnectionFailed(peripheral, status);
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            LOGGER.error("Scan failed with error code: {}", errorCode);
            
            // Notify the external listener
            if (externalEventListener != null) {
                externalEventListener.onScanFailed(errorCode);
            }
        }
    };
    
    /**
     * Creates a new Bluetooth manager.
     */
    public BluetoothManager() {
        init();
    }
    
    /**
     * Initializes the Bluetooth manager and its components.
     */
    private void init() {
        LOGGER.info("Initializing BluetoothManager...");
        try {
            centralManager = new BluetoothCentralManager(centralManagerCallback);
            deviceScanner = new BluetoothDeviceScanner(centralManager, this);
            connectionHandler = new BluetoothConnectionHandler(centralManager, this);
            dataHandler = new BluetoothDataHandler(connectionHandler);
            LOGGER.info("BluetoothManager initialized successfully.");
        } catch (RuntimeException e) {
            LOGGER.error("Failed to initialize BluetoothManager. Bluetooth functionality might be unavailable.", e);
            centralManager = null;
        }
    }
    
    /**
     * Registers a listener for Bluetooth events.
     * 
     * @param listener The listener to be registered
     */
    public void setEventListener(BluetoothEventListener listener) {
        this.externalEventListener = listener;
    }
    
    /**
     * Sets a minimum RSSI value to filter out devices with weak signals.
     * 
     * @param rssiValue The minimum RSSI value to be considered
     */
    public void setMinimumRssi(int rssiValue) {
        if (deviceScanner != null) {
            deviceScanner.setMinimumRssi(rssiValue);
        }
    }
    
    /**
     * Starts the discovery of BLE devices.
     * 
     * @return true if scanning was started, false otherwise
     */
    public boolean startDeviceDiscovery() {
        if (deviceScanner != null) {
            return deviceScanner.startDeviceDiscovery();
        }
        return false;
    }
    
    /**
     * Stops the discovery of BLE devices.
     */
    public void stopDeviceDiscovery() {
        if (deviceScanner != null) {
            deviceScanner.stopDeviceDiscovery();
        }
    }
    
    /**
     * Attempts to connect to a specific device by MAC address.
     * 
     * @param deviceAddress MAC address of the device
     * @return true if the connection attempt was initiated, false otherwise
     */
    public boolean connectToDevice(String deviceAddress) {
        if (connectionHandler != null) {
            return connectionHandler.connectToDevice(deviceAddress);
        }
        return false;
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
        if (dataHandler != null) {
            return dataHandler.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
        }
        return false;
    }
    
    /**
     * Sends a PWM value to the connected peripheral.
     * 
     * @param pwmValue PWM value (e.g., 0-100)
     * @return true if the command was sent, false otherwise
     */
    public boolean sendPwmCommand(int pwmValue) {
        if (dataHandler != null) {
            return dataHandler.sendPwmCommand(pwmValue);
        }
        return false;
    }
    
    /**
     * Closes the connection with the current peripheral.
     */
    public void closeConnection() {
        if (connectionHandler != null) {
            connectionHandler.closeConnection();
        }
    }
    
    /**
     * Checks if connected to a peripheral.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connectionHandler != null && connectionHandler.isConnected();
    }
    
    /**
     * Checks if Bluetooth was correctly initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return centralManager != null;
    }
    
    /**
     * Checks if scanning is in progress.
     * 
     * @return true if scanning, false otherwise
     */
    public boolean isScanning() {
        return deviceScanner != null && deviceScanner.isScanning();
    }
    
    /**
     * Shuts down the Bluetooth service, stopping scans and disconnecting.
     */
    public void shutdown() {
        LOGGER.info("Shutting down BluetoothManager...");
        if (deviceScanner != null && deviceScanner.isScanning()) {
            deviceScanner.stopDeviceDiscovery();
        }
        if (connectionHandler != null) {
            connectionHandler.closeConnection();
        }
        LOGGER.info("BluetoothManager shutdown complete.");
    }
    
    // BluetoothEventListener implementation (internal bridge)
    @Override
    public void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        if (externalEventListener != null) {
            externalEventListener.onDeviceDiscovered(peripheral, name, address, rssi);
        }
    }

    @Override
    public void onDeviceConnected(BluetoothPeripheral peripheral) {
        if (externalEventListener != null) {
            externalEventListener.onDeviceConnected(peripheral);
        }
    }

    @Override
    public void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        if (externalEventListener != null) {
            externalEventListener.onDeviceDisconnected(peripheral, status);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (externalEventListener != null) {
            externalEventListener.onScanFailed(errorCode);
        }
    }
} 