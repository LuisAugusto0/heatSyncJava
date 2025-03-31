package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principal que coordena todos os componentes Bluetooth.
 */
public class BluetoothManager implements BluetoothEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothManager.class);
    
    private BluetoothCentralManager centralManager;
    private BluetoothDeviceScanner deviceScanner;
    private BluetoothConnectionHandler connectionHandler;
    private BluetoothDataHandler dataHandler;
    
    private BluetoothEventListener externalEventListener;
    
    /**
     * Callback para gerenciar eventos do Bluetooth Central Manager.
     */
    private final BluetoothCentralManagerCallback centralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            String name = peripheral.getName();
            String address = peripheral.getAddress();
            int rssi = scanResult.getRssi();
            
            // Processar o dispositivo descoberto
            deviceScanner.processDiscoveredDevice(peripheral, name, address, rssi);
        }

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            LOGGER.info("Connected to peripheral: {}", peripheral.getAddress());
            connectionHandler.handleDeviceConnected(peripheral);
            
            // Notificar o listener externo
            if (externalEventListener != null) {
                externalEventListener.onDeviceConnected(peripheral);
            }
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            LOGGER.warn("Disconnected from peripheral: {} with status {}", peripheral.getAddress(), status);
            connectionHandler.handleDeviceDisconnected(peripheral, status);
            
            // Notificar o listener externo
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
            
            // Notificar o listener externo
            if (externalEventListener != null) {
                externalEventListener.onScanFailed(errorCode);
            }
        }
    };
    
    /**
     * Cria um novo gerenciador Bluetooth.
     */
    public BluetoothManager() {
        init();
    }
    
    /**
     * Inicializa o gerenciador Bluetooth e seus componentes.
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
     * Registra um listener para eventos Bluetooth.
     * 
     * @param listener O listener a ser registrado
     */
    public void setEventListener(BluetoothEventListener listener) {
        this.externalEventListener = listener;
    }
    
    /**
     * Define um valor mínimo de RSSI para filtrar dispositivos com sinal fraco.
     * 
     * @param rssiValue O valor mínimo de RSSI a ser considerado
     */
    public void setMinimumRssi(int rssiValue) {
        if (deviceScanner != null) {
            deviceScanner.setMinimumRssi(rssiValue);
        }
    }
    
    /**
     * Inicia a descoberta de dispositivos BLE.
     * 
     * @return true se a varredura foi iniciada, false caso contrário
     */
    public boolean startDeviceDiscovery() {
        if (deviceScanner != null) {
            return deviceScanner.startDeviceDiscovery();
        }
        return false;
    }
    
    /**
     * Para a descoberta de dispositivos BLE.
     */
    public void stopDeviceDiscovery() {
        if (deviceScanner != null) {
            deviceScanner.stopDeviceDiscovery();
        }
    }
    
    /**
     * Tenta conectar a um dispositivo específico pelo endereço MAC.
     * 
     * @param deviceAddress Endereço MAC do dispositivo
     * @return true se a tentativa de conexão foi iniciada, false caso contrário
     */
    public boolean connectToDevice(String deviceAddress) {
        if (connectionHandler != null) {
            return connectionHandler.connectToDevice(deviceAddress);
        }
        return false;
    }
    
    /**
     * Envia dados de temperatura para o periférico conectado.
     * 
     * @param cpuTemp Temperatura da CPU
     * @param gpuTemp Temperatura da GPU
     * @param diskTemp Temperatura do disco
     * @return true se os dados foram enviados, false caso contrário
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        if (dataHandler != null) {
            return dataHandler.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
        }
        return false;
    }
    
    /**
     * Envia um valor PWM para o periférico conectado.
     * 
     * @param pwmValue Valor PWM (ex: 0-100)
     * @return true se o comando foi enviado, false caso contrário
     */
    public boolean sendPwmCommand(int pwmValue) {
        if (dataHandler != null) {
            return dataHandler.sendPwmCommand(pwmValue);
        }
        return false;
    }
    
    /**
     * Fecha a conexão com o periférico atual.
     */
    public void closeConnection() {
        if (connectionHandler != null) {
            connectionHandler.closeConnection();
        }
    }
    
    /**
     * Verifica se está conectado a um periférico.
     * 
     * @return true se conectado, false caso contrário
     */
    public boolean isConnected() {
        return connectionHandler != null && connectionHandler.isConnected();
    }
    
    /**
     * Verifica se o Bluetooth foi inicializado corretamente.
     * 
     * @return true se inicializado, false caso contrário
     */
    public boolean isInitialized() {
        return centralManager != null;
    }
    
    /**
     * Verifica se está em processo de varredura.
     * 
     * @return true se está em varredura, false caso contrário
     */
    public boolean isScanning() {
        return deviceScanner != null && deviceScanner.isScanning();
    }
    
    /**
     * Desliga o serviço Bluetooth, parando varreduras e desconectando.
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
    
    // Implementação de BluetoothEventListener (bridge interno)
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