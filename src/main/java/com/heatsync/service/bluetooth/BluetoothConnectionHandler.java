package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothGattCharacteristic;
import com.welie.blessed.BluetoothGattService;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Responsible for managing connections with Bluetooth devices.
 */
public class BluetoothConnectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothConnectionHandler.class);
    
    private final BluetoothCentralManager centralManager;
    private final BluetoothEventListener eventListener;
    
    private BluetoothPeripheral connectedPeripheral = null;
    private boolean connected = false;
    
    /**
     * Callback to handle events from a specific Bluetooth peripheral.
     */
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral, List<BluetoothGattService> services) {
            LOGGER.info("Services discovered for {}", peripheral.getAddress());
            
            // Check if we found the device name after discovering services
            String name = peripheral.getName();
            if (name != null && !name.isEmpty()) {
                LOGGER.info("Device name after service discovery: {}", name);
                
                // Notify UI about the updated name
                if (eventListener != null) {
                    eventListener.onDeviceDiscovered(peripheral, name, peripheral.getAddress(), 0);
                }
            }
        }

        @Override
        public void onNotificationStateUpdate(BluetoothPeripheral peripheral, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            LOGGER.info("Notification state updated for characteristic {} on {}, status: {}", 
                characteristic.getUuid(), peripheral.getAddress(), status);
        }

        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            LOGGER.info("Characteristic {} updated for {}: {} (status={})", characteristic.getUuid(), peripheral.getAddress(), bytesToHex(value), status);
            // Handle incoming data (notifications/indications)
        }

        @Override
        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            if (status == BluetoothCommandStatus.COMMAND_SUCCESS) {
                LOGGER.info("Successfully wrote value {} to characteristic {} for {}", bytesToHex(value), characteristic.getUuid(), peripheral.getAddress());
            } else {
                 LOGGER.error("Failed to write characteristic {} for {}, status: {}", characteristic.getUuid(), peripheral.getAddress(), status);
            }
        }
        
        // Helper to convert bytes to hex string for logging
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }
    };
    
    /**
     * Creates a new Bluetooth connection handler.
     * 
     * @param centralManager The Bluetooth central manager
     * @param eventListener The listener for Bluetooth events
     */
    public BluetoothConnectionHandler(BluetoothCentralManager centralManager, BluetoothEventListener eventListener) {
        this.centralManager = centralManager;
        this.eventListener = eventListener;
    }
    
    /**
     * Attempts to connect to a specific device by MAC address.
     * 
     * @param deviceAddress MAC address of the device
     * @return true if the connection attempt was initiated, false otherwise
     */
    public boolean connectToDevice(String deviceAddress) {
        if (centralManager == null) {
            LOGGER.error("Cannot connect, BluetoothCentralManager not initialized.");
            return false;
        }
        
        // Try to get the peripheral by address (may have been discovered previously)
        BluetoothPeripheral peripheral = centralManager.getPeripheral(deviceAddress);
        if (peripheral != null) {
            LOGGER.info("Attempting to connect to: {} using stored peripheral object", deviceAddress);
            // Pass the peripheral-specific callback
            centralManager.connectPeripheral(peripheral, peripheralCallback);
            return true; // Attempt initiated (success/failure will be via callback)
        } else {
            // If not known, may need to scan first or connect directly (if supported)
            LOGGER.error("Peripheral with address {} not found in central manager's list. Ensure device was discovered.", deviceAddress);
            return false;
        }
    }
    
    /**
     * Handles the event of connection with a device.
     * 
     * @param peripheral The connected device
     */
    public void handleDeviceConnected(BluetoothPeripheral peripheral) {
        connected = true;
        connectedPeripheral = peripheral;
    }
    
    /**
     * Handles the event of disconnection from a device.
     * 
     * @param peripheral The disconnected device
     * @param status The status of the disconnection
     */
    public void handleDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        connected = false;
        connectedPeripheral = null;
    }
    
    /**
     * Handles the event of connection failure.
     * 
     * @param peripheral The device that failed to connect
     * @param status The status of the failure
     */
    public void handleConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        connected = false;
        connectedPeripheral = null;
    }
    
    /**
     * Closes the connection with the current peripheral.
     */
    public void closeConnection() {
        if (connectedPeripheral != null && centralManager != null) {
            LOGGER.info("Closing connection to peripheral: {}", connectedPeripheral.getAddress());
            centralManager.cancelConnection(connectedPeripheral); // Request disconnection
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
        return connected && connectedPeripheral != null;
    }
    
    /**
     * Gets the currently connected peripheral.
     * 
     * @return The connected peripheral or null if there is no connection
     */
    public BluetoothPeripheral getConnectedPeripheral() {
        return connectedPeripheral;
    }
} 