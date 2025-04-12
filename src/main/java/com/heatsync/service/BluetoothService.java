package com.heatsync.service;

import com.heatsync.service.bluetooth.BluetoothEventListener;
import com.heatsync.service.bluetooth.BluetoothManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that handles Bluetooth communication with peripherals.
 */
public class BluetoothService implements BluetoothEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothService.class);
    
    private final BluetoothManager bluetoothManager;
    private BluetoothEventListener eventListener;
    
    /**
     * Creates a new Bluetooth service.
     */
    public BluetoothService() {
        LOGGER.info("Initializing BluetoothService with BlueCove (SPP)...");
        try {
            bluetoothManager = new BluetoothManager();
            bluetoothManager.setEventListener(this);
            if (!bluetoothManager.isInitialized()) {
                LOGGER.error("BluetoothManager initialized but reported isInitialized() = false");
            } else {
                LOGGER.info("BluetoothManager successfully initialized and reported isInitialized() = true");
            }
        } catch (Exception e) {
            LOGGER.error("Exception during BluetoothManager initialization", e);
            throw new RuntimeException("Failed to initialize BluetoothService", e);
        }
    }
    
    /**
     * Sets the event listener for Bluetooth events.
     * 
     * @param listener The event listener
     */
    public void setEventListener(BluetoothEventListener listener) {
        // Apenas atualiza o listener local, mantendo o BluetoothService como
        // listener do BluetoothManager
        this.eventListener = listener;
        // NÃO sobrescreva o listener do BluetoothManager
        // bluetoothManager.setEventListener(listener); <- remova esta linha
    }
    
    /**
     * Sets a minimum RSSI value to filter out devices with weak signals.
     * @param rssiValue The minimum RSSI value to be considered (typically between -100 and 0).
     */
    public void setMinimumRssi(int rssiValue) {
        bluetoothManager.setMinimumRssi(rssiValue);
    }
    
    /**
     * Starts the discovery of Bluetooth devices.
     * @return true if scanning was started, false otherwise.
     */
    public boolean startDeviceDiscovery() {
        return bluetoothManager.startDeviceDiscovery();
    }
    
    /**
     * Stops the discovery of Bluetooth devices.
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
     * Envia um comando de perfil fixo para o periférico conectado.
     * 
     * Formato do comando: C<percentage>
     *
     * @param percentage O valor (0-100) a enviar
     * @return true se o comando for enviado, false caso contrário
     */
    public boolean sendConstantCommand(int percentage) {
        return bluetoothManager.sendConstantCommand(percentage);
    }
    
    /**
     * Envia dados de perfil para o periférico conectado.
     * 
     * Formato do comando: L<cpuMinTemp>:<gpuMinTemp>:<cpuMaxTemp>:<gpuMaxTemp>\n
     *
     * @param cpuMinTemp Temperatura mínima da CPU
     * @param gpuMinTemp Temperatura mínima da GPU
     * @param cpuMaxTemp Temperatura máxima da CPU
     * @param gpuMaxTemp Temperatura máxima da GPU
     * @return true se os dados forem enviados, false caso contrário
     */
    public boolean sendProfileData(int cpuMinTemp, int gpuMinTemp, int cpuMaxTemp, int gpuMaxTemp) {
        return bluetoothManager.sendProfileData(cpuMinTemp, gpuMinTemp, cpuMaxTemp, gpuMaxTemp);
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
    public void onDeviceDiscovered(Object deviceObj, String name, String address, int rssi) {
        if (eventListener != null) {
            eventListener.onDeviceDiscovered(deviceObj, name, address, rssi);
        }
    }

    @Override
    public void onDeviceConnected(Object deviceObj) {
        if (eventListener != null) {
            eventListener.onDeviceConnected(deviceObj);
        }
    }

    @Override
    public void onDeviceDisconnected(Object deviceObj, int status) {
        if (eventListener != null) {
            eventListener.onDeviceDisconnected(deviceObj, status);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (eventListener != null) {
            eventListener.onScanFailed(errorCode);
        }
    }

    @Override
    public void onFanRpmReceived(int rpm) {
        if (eventListener != null) {
            LOGGER.info("BFan RPM received: {}", rpm);
            eventListener.onFanRpmReceived(rpm);
        }
    }
}
