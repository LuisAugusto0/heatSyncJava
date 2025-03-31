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
 * Responsável pelo gerenciamento de conexões com dispositivos Bluetooth.
 */
public class BluetoothConnectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothConnectionHandler.class);
    
    private final BluetoothCentralManager centralManager;
    private final BluetoothEventListener eventListener;
    
    private BluetoothPeripheral connectedPeripheral = null;
    private boolean connected = false;
    
    /**
     * Callback para lidar com eventos de um Periférico Bluetooth específico.
     */
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral, List<BluetoothGattService> services) {
            LOGGER.info("Services discovered for {}", peripheral.getAddress());
            
            // Verificar se encontramos o nome do dispositivo após descoberta de serviços
            String name = peripheral.getName();
            if (name != null && !name.isEmpty()) {
                LOGGER.info("Device name after service discovery: {}", name);
                
                // Notificar a UI sobre o nome atualizado
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
     * Cria um novo gerenciador de conexões Bluetooth.
     * 
     * @param centralManager O gerenciador central Bluetooth
     * @param eventListener O listener para eventos Bluetooth
     */
    public BluetoothConnectionHandler(BluetoothCentralManager centralManager, BluetoothEventListener eventListener) {
        this.centralManager = centralManager;
        this.eventListener = eventListener;
    }
    
    /**
     * Tenta conectar a um dispositivo específico pelo endereço MAC.
     * 
     * @param deviceAddress Endereço MAC do dispositivo
     * @return true se a tentativa de conexão foi iniciada, false caso contrário
     */
    public boolean connectToDevice(String deviceAddress) {
        if (centralManager == null) {
            LOGGER.error("Cannot connect, BluetoothCentralManager not initialized.");
            return false;
        }
        
        // Tenta obter o periférico pelo endereço (pode ter sido descoberto anteriormente)
        BluetoothPeripheral peripheral = centralManager.getPeripheral(deviceAddress);
        if (peripheral != null) {
            LOGGER.info("Attempting to connect to: {} using stored peripheral object", deviceAddress);
            // Pass the peripheral-specific callback
            centralManager.connectPeripheral(peripheral, peripheralCallback);
            return true; // Tentativa iniciada (sucesso/falha será via callback)
        } else {
            // Se não conhecido, talvez precise escanear primeiro ou conectar diretamente (se suportado)
            LOGGER.error("Peripheral with address {} not found in central manager's list. Ensure device was discovered.", deviceAddress);
            return false;
        }
    }
    
    /**
     * Trata o evento de conexão com um dispositivo.
     * 
     * @param peripheral O dispositivo conectado
     */
    public void handleDeviceConnected(BluetoothPeripheral peripheral) {
        connected = true;
        connectedPeripheral = peripheral;
    }
    
    /**
     * Trata o evento de desconexão com um dispositivo.
     * 
     * @param peripheral O dispositivo desconectado
     * @param status O status da desconexão
     */
    public void handleDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        connected = false;
        connectedPeripheral = null;
    }
    
    /**
     * Trata o evento de falha na conexão.
     * 
     * @param peripheral O dispositivo que falhou ao conectar
     * @param status O status da falha
     */
    public void handleConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        connected = false;
        connectedPeripheral = null;
    }
    
    /**
     * Fecha a conexão com o periférico atual.
     */
    public void closeConnection() {
        if (connectedPeripheral != null && centralManager != null) {
            LOGGER.info("Closing connection to peripheral: {}", connectedPeripheral.getAddress());
            centralManager.cancelConnection(connectedPeripheral); // Solicita desconexão
        } else {
             LOGGER.info("No active connection to close.");
        }
    }
    
    /**
     * Verifica se está conectado a um periférico.
     * 
     * @return true se conectado, false caso contrário
     */
    public boolean isConnected() {
        return connected && connectedPeripheral != null;
    }
    
    /**
     * Obtém o periférico conectado atualmente.
     * 
     * @return O periférico conectado ou null se não houver conexão
     */
    public BluetoothPeripheral getConnectedPeripheral() {
        return connectedPeripheral;
    }
} 