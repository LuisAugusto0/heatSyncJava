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
 * Fachada (Facade) para o serviço Bluetooth.
 * Delega operações para o gerenciador Bluetooth subjacente.
 */
public class BluetoothService implements BluetoothEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothService.class);
    
    private final BluetoothManager bluetoothManager;
    private BluetoothEventListener eventListener;
    
    /**
     * Construtor padrão. Inicializa o BluetoothManager.
     */
    public BluetoothService() {
        LOGGER.info("Initializing BluetoothService with Blessed-Bluez...");
        bluetoothManager = new BluetoothManager();
        bluetoothManager.setEventListener(this);
    }
    
    /**
     * Registra um listener para receber eventos Bluetooth.
     * @param listener O listener a ser registrado.
     */
    public void setBluetoothEventListener(BluetoothEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Define um valor mínimo de RSSI para filtrar dispositivos com sinal fraco.
     * @param rssiValue O valor mínimo de RSSI a ser considerado (tipicamente entre -100 e 0).
     */
    public void setMinimumRssi(int rssiValue) {
        bluetoothManager.setMinimumRssi(rssiValue);
    }
    
    /**
     * Inicia a descoberta de dispositivos BLE.
     * @return true se a varredura foi iniciada, false caso contrário.
     */
    public boolean startDeviceDiscovery() {
        return bluetoothManager.startDeviceDiscovery();
    }
    
    /**
     * Para a descoberta de dispositivos BLE.
     */
    public void stopDeviceDiscovery() {
        bluetoothManager.stopDeviceDiscovery();
    }
    
    /**
     * Tenta conectar a um dispositivo específico pelo endereço MAC.
     * @param deviceAddress Endereço MAC do dispositivo.
     * @return true se a tentativa de conexão foi iniciada, false caso contrário.
     */
    public boolean connectToDevice(String deviceAddress) {
        return bluetoothManager.connectToDevice(deviceAddress);
    }
    
    /**
     * Envia dados de temperatura para o periférico conectado.
     * @param cpuTemp Temperatura da CPU.
     * @param gpuTemp Temperatura da GPU.
     * @param diskTemp Temperatura do disco.
     * @return true se os dados foram enviados, false caso contrário.
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        return bluetoothManager.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
    }
    
    /**
     * Envia um valor PWM para o periférico conectado.
     * @param pwmValue Valor PWM (ex: 0-100).
     * @return true se o comando foi enviado, false caso contrário.
     */
    public boolean sendPwmCommand(int pwmValue) {
        return bluetoothManager.sendPwmCommand(pwmValue);
    }
    
    /**
     * Fecha a conexão com o periférico atual.
     */
    public void closeConnection() {
        bluetoothManager.closeConnection();
    }
    
    /**
     * Verifica se está conectado a um periférico.
     * @return true se conectado, false caso contrário.
     */
    public boolean isConnected() {
        return bluetoothManager.isConnected();
    }
    
    /**
     * Verifica se o serviço Bluetooth foi inicializado corretamente.
     * @return true se inicializado, false caso contrário.
     */
    public boolean isInitialized() {
        return bluetoothManager.isInitialized();
    }
    
    /**
     * Verifica se está em processo de varredura.
     * @return true se está em varredura, false caso contrário.
     */
    public boolean isScanning() {
        return bluetoothManager.isScanning();
    }
    
    /**
     * Desliga o serviço Bluetooth, parando scans e desconectando.
     */
    public void shutdown() {
        bluetoothManager.shutdown();
    }
    
    // Implementação dos métodos da interface BluetoothEventListener
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
