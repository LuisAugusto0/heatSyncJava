package com.heatsync.service;

// Blessed-Bluez imports
import com.welie.blessed.*; // Import core Blessed classes
import com.welie.blessed.BluetoothCommandStatus; // Import the status enum
import org.bluez.exceptions.*; // Import BlueZ specific exceptions used by Blessed-Bluez
import org.slf4j.Logger; // Use SLF4J Logger
import org.slf4j.LoggerFactory; // Use SLF4J LoggerFactory

// Java standard imports (mantendo os necessários)
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID; // Needed for service/characteristic UUIDs later
// Removed java.util.logging imports

/**
 * Serviço que gerencia a comunicação Bluetooth Low Energy (BLE) usando Blessed-Bluez.
 */
public class BluetoothService {
    // Use SLF4J Logger instead of java.util.logging.Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothService.class);
    
    private BluetoothCentralManager centralManager;
    private boolean connected = false; // Mantém estado de conexão (será atualizado pelos callbacks)
    private BluetoothPeripheral connectedPeripheral = null; // Referência ao periférico conectado
    private boolean isScanning = false; // Added flag to track scanning state
    
    // Lista para armazenar os dispositivos descobertos
    private final List<BluetoothPeripheral> discoveredPeripherals = new ArrayList<>();
    
    private int minRssi = -100; // Valor padrão (praticamente sem filtragem)
    
    // Interface para notificar a UI sobre eventos Bluetooth
    public interface BluetoothEventListener {
        void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi);
        void onDeviceConnected(BluetoothPeripheral peripheral);
        void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status);
        void onScanFailed(int errorCode);
    }
    
    private BluetoothEventListener eventListener = null;
    
    /**
     * Registra um listener para receber eventos Bluetooth.
     * @param listener O listener a ser registrado.
     */
    public void setBluetoothEventListener(BluetoothEventListener listener) {
        this.eventListener = listener;
    }

    // Exemplo: UUID do serviço e característica do ESP32 (Ajustar conforme necessário)
    // Estes serão usados mais tarde para comunicação
    // private static final UUID COOLER_SERVICE_UUID = UUID.fromString("..."); 
    // private static final UUID TEMP_CHARACTERISTIC_UUID = UUID.fromString("...");
    // private static final UUID RPM_CHARACTERISTIC_UUID = UUID.fromString("...");
    // private static final UUID PWM_CHARACTERISTIC_UUID = UUID.fromString("...");

    /**
     * Define um valor mínimo de RSSI para filtrar dispositivos com sinal fraco.
     * Valores mais próximos de zero são sinais mais fortes (ex: -50 é mais forte que -80).
     * @param rssiValue O valor mínimo de RSSI a ser considerado (tipicamente entre -100 e 0).
     */
    public void setMinimumRssi(int rssiValue) {
        this.minRssi = rssiValue;
        LOGGER.info("Minimum RSSI filter set to {} dBm", rssiValue);
    }
    
    /**
     * Obtém o valor mínimo de RSSI configurado para filtragem.
     * @return O valor atual do filtro de RSSI.
     */
    public int getMinimumRssi() {
        return minRssi;
    }

    /**
     * Callback para lidar com eventos do Bluetooth Central Manager.
     */
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            String name = peripheral.getName();
            String address = peripheral.getAddress();
            int rssi = scanResult.getRssi();
            
            // Verificar se o dispositivo atende ao filtro de RSSI
            if (rssi < minRssi) {
                LOGGER.debug("Device filtered out due to weak signal: {} ({}) RSSI: {}dBm", 
                    (name != null && !name.isEmpty()) ? name : "Unknown", address, rssi);
                return;
            }
            
            // Verificar informações do dispositivo
            LOGGER.debug("Discovered device - Address: {}, Raw name: {}, RSSI: {}", address, name, rssi);
            
            // Se o nome estiver vazio, usar o endereço como identificação e adicionar indicador para o usuário saber que é nome ausente
            if (name == null || name.isEmpty()) {
                name = "Dispositivo " + address;
            }
            
            // Verificar se já temos este dispositivo na lista
            boolean isNewDevice = true;
            for (int i = 0; i < discoveredPeripherals.size(); i++) {
                BluetoothPeripheral existingPeripheral = discoveredPeripherals.get(i);
                if (existingPeripheral.getAddress().equals(address)) {
                    // Dispositivo já está na lista, substituir com o mais recente
                    discoveredPeripherals.set(i, peripheral);
                    isNewDevice = false;
                    LOGGER.debug("Updated existing device in list: {} ({})", name, address);
                    break;
                }
            }
            
            // Adicionar à lista se for um novo dispositivo
            if (isNewDevice) {
                discoveredPeripherals.add(peripheral);
                LOGGER.info("Discovered new peripheral: {} ({}) RSSI: {}dBm", name, address, rssi);
            }
            
            // Notificar UI se o listener estiver registrado
            if (eventListener != null) {
                eventListener.onDeviceDiscovered(peripheral, name, address, rssi);
            }
        }

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            LOGGER.info("Connected to peripheral: {}", peripheral.getAddress());
            connected = true;
            connectedPeripheral = peripheral;
            
            // Tentar ler nome do dispositivo após conexão
            String name = peripheral.getName();
            if (name == null || name.isEmpty()) {
                LOGGER.info("Device connected without name, will attempt to discover services to find name");
            } else {
                LOGGER.info("Connected device name: {}", name);
            }
            
            // Notificar UI
            if (eventListener != null) {
                eventListener.onDeviceConnected(peripheral);
            }
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) { 
            LOGGER.warn("Disconnected from peripheral: {} with status {}", peripheral.getAddress(), status);
            connected = false;
            connectedPeripheral = null;
            
            // Notificar UI
            if (eventListener != null) {
                eventListener.onDeviceDisconnected(peripheral, status);
            }
        }

        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) { 
            LOGGER.error("Connection failed for peripheral: {} with status {}", peripheral.getAddress(), status);
            connected = false;
            connectedPeripheral = null;
        }
        
        @Override
        public void onScanFailed(int errorCode) {
             LOGGER.error("Scan failed with error code: {}", errorCode);
             
             // Notificar UI
             if (eventListener != null) {
                 eventListener.onScanFailed(errorCode);
             }
        }
    };

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
            
            // Now we can interact with characteristics
            // Ex: peripheral.readCharacteristic(SERVICE_UUID, CHARACTERISTIC_UUID);
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
            // Handle confirmation of write
        }
        
        // Helper to convert bytes to hex string for logging
        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }
        
        // Add other overrides like onCharacteristicRead, onMtuChanged etc. if needed
    };


    /**
     * Construtor padrão. Inicializa o BluetoothCentralManager.
     */
    public BluetoothService() {
        LOGGER.info("Initializing BluetoothService with Blessed-Bluez...");
        try {
            centralManager = new BluetoothCentralManager(bluetoothCentralManagerCallback);
            LOGGER.info("BluetoothCentralManager initialized successfully.");
            // Pode iniciar a varredura aqui ou deixar para um método separado
            // startDeviceDiscovery(); 
        } catch (RuntimeException e) {
            LOGGER.error("Failed to initialize BluetoothCentralManager. Bluetooth functionality might be unavailable.", e);
            // Tratar erro - talvez o adaptador BT não esteja ligado ou BlueZ/DBus não esteja acessível
            centralManager = null; // Garantir que está nulo se a inicialização falhar
        }
    }

    /**
     * Inicia a descoberta de dispositivos BLE.
     */
    public boolean startDeviceDiscovery() {
        if (centralManager != null) {
            if (isScanning) { // Check internal flag
                 LOGGER.debug("Scan already in progress. Stopping previous one.");
                 stopDeviceDiscovery(); // Call internal stop method
            }
            LOGGER.info("Attempting to start BLE scan...");
            centralManager.scanForPeripherals(); 
            isScanning = true; // Set internal flag
            LOGGER.info("Scan initiated.");
            return true; 
        } else {
            LOGGER.error("Cannot start discovery, BluetoothCentralManager is not initialized.");
        return false;
        }
    }
    
    /**
     * Para a descoberta de dispositivos BLE.
     */
     public void stopDeviceDiscovery() {
         if (centralManager != null) {
             if (isScanning) { // Check internal flag
                 LOGGER.info("Stopping BLE scan...");
                 centralManager.stopScan();
                 isScanning = false; // Clear internal flag
             } else {
                 LOGGER.debug("Scan not active, no need to stop.");
             }
         } else {
              LOGGER.warn("Cannot stop scan, BluetoothCentralManager is not initialized.");
         }
     }

    /**
     * Limpa a lista de dispositivos descobertos.
     */
    public void clearDiscoveredDevices() {
        discoveredPeripherals.clear();
    }

    /**
     * Retorna uma lista de dispositivos descobertos.
     * @return Lista de dispositivos BLE encontrados durante a varredura.
     */
    public List<BluetoothPeripheral> getDiscoveredDevices() {
        return new ArrayList<>(discoveredPeripherals); // Retorna uma cópia para evitar problemas de concorrência
    }

    /**
     * Tenta conectar a um dispositivo específico pelo endereço MAC.
     * @param deviceAddress Endereço MAC do dispositivo.
     * @return true se a tentativa de conexão foi iniciada, false caso contrário.
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
            // Opcional: tentar autoScanConnect (se aplicável e desejado)
            // try { centralManager.autoConnectPeripheral(deviceAddress, peripheralCallback); return true; } catch (Exception e) { LOGGER.error(...); return false; }
            return false;
        }
    }
    
    /**
     * Conecta ao primeiro ESP32 encontrado (exemplo simplificado).
     * TODO: Melhorar a lógica de seleção de dispositivo (usar lista interna gerenciada pelo callback).
     * @return True se iniciou a tentativa de conexão, False caso contrário.
     */
    public boolean connectToESP32() {
         if (centralManager == null) {
             LOGGER.error("Cannot connect, BluetoothCentralManager not initialized.");
             return false;
         }
         LOGGER.warn("connectToESP32() needs implementation to use a managed list of discovered devices.");
         // Removed call to getDiscoveredPeripherals()
         // Lógica atual não funciona sem acesso à lista. Precisa ser redesenhada
         // para usar uma lista populada pelo onDiscoveredPeripheral callback.
        return false;
    }


    /**
     * Envia dados de temperatura para o periférico conectado.
     * (Placeholder - Precisa implementar escrita em característica BLE)
     * @return Sempre false por enquanto.
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        if (!isConnected() || connectedPeripheral == null) {
            LOGGER.warn("Cannot send data: Not connected to a peripheral.");
            return false;
        }
        LOGGER.warn("sendTemperatureData() not implemented yet. Would send: CPU={}, GPU={}, Disk={}", cpuTemp, gpuTemp, diskTemp);
        // Lógica para formatar os dados e escrever na característica BLE apropriada
        // Exemplo: byte[] data = formatTemperatureData(cpuTemp, gpuTemp, diskTemp);
        // connectedPeripheral.writeCharacteristic(COOLER_SERVICE_UUID, TEMP_CHARACTERISTIC_UUID, data, WriteType.WITH_RESPONSE);
        return false;
    }

    /**
     * Envia um valor PWM para o periférico conectado.
     * (Placeholder - Precisa implementar escrita em característica BLE)
     * @param pwmValue Valor PWM (ex: 0-100).
     * @return Sempre false por enquanto.
     */
     public boolean sendPwmCommand(int pwmValue) {
         if (!isConnected() || connectedPeripheral == null) {
            LOGGER.warn("Cannot send PWM command: Not connected.");
            return false;
         }
         LOGGER.warn("sendPwmCommand() not implemented yet. Would send PWM={}", pwmValue);
        // Lógica para formatar o comando PWM e escrever na característica BLE
        // Exemplo: byte[] data = new byte[]{(byte) pwmValue};
        // connectedPeripheral.writeCharacteristic(COOLER_SERVICE_UUID, PWM_CHARACTERISTIC_UUID, data, WriteType.WITHOUT_RESPONSE);
         return false;
     }

    /**
     * Lê dados do periférico (ex: RPM).
     * (Placeholder - Precisa implementar leitura/notificação de característica BLE)
     * @return Sempre null por enquanto.
     */
    public String receiveData() {
        if (!isConnected() || connectedPeripheral == null) {
            LOGGER.warn("Cannot receive data: Not connected.");
            return null;
        }
        LOGGER.warn("receiveData() not implemented yet (use notifications or read characteristic).");
        // Lógica para ler característica ou esperar notificação
        // Exemplo: byte[] value = connectedPeripheral.readCharacteristic(COOLER_SERVICE_UUID, RPM_CHARACTERISTIC_UUID);
        // return parseRpmData(value);
        return null;
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
        // O callback onDisconnectedPeripheral cuidará de atualizar 'connected' e 'connectedPeripheral'
    }

    /**
     * Verifica se está conectado a um periférico.
     */
    public boolean isConnected() {
        // A variável 'connected' é atualizada pelos callbacks
        return connected && connectedPeripheral != null; 
    }

    /**
     * Verifica se o serviço Bluetooth foi inicializado corretamente.
     * @return true se inicializado, false caso contrário.
     */
    public boolean isInitialized() {
        return centralManager != null;
    }

    /**
     * Verifica se está em processo de varredura.
     * @return true se está em varredura, false caso contrário.
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * Desliga o serviço Bluetooth, parando scans e desconectando.
     */
    public void shutdown() {
        LOGGER.info("Shutting down BluetoothService...");
        if (centralManager != null) {
            if (isScanning) { // Check internal flag
                stopDeviceDiscovery(); // Use internal method to stop and clear flag
            }
            closeConnection(); 
        }
        LOGGER.info("BluetoothService shutdown complete.");
    }
}
