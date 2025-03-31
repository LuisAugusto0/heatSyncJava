package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothPeripheral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsável pela varredura e descoberta de dispositivos Bluetooth.
 */
public class BluetoothDeviceScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothDeviceScanner.class);
    
    private final BluetoothCentralManager centralManager;
    private final List<BluetoothPeripheral> discoveredPeripherals = new ArrayList<>();
    private final BluetoothEventListener eventListener;
    
    private boolean isScanning = false;
    private int minRssi = -100; // Valor padrão (praticamente sem filtragem)
    
    /**
     * Cria um novo scanner de dispositivos.
     * 
     * @param centralManager O gerenciador central Bluetooth
     * @param eventListener O listener para eventos Bluetooth
     */
    public BluetoothDeviceScanner(BluetoothCentralManager centralManager, BluetoothEventListener eventListener) {
        this.centralManager = centralManager;
        this.eventListener = eventListener;
    }
    
    /**
     * Define um valor mínimo de RSSI para filtrar dispositivos com sinal fraco.
     * Valores mais próximos de zero são sinais mais fortes (ex: -50 é mais forte que -80).
     * 
     * @param rssiValue O valor mínimo de RSSI a ser considerado (tipicamente entre -100 e 0)
     */
    public void setMinimumRssi(int rssiValue) {
        this.minRssi = rssiValue;
        LOGGER.info("Minimum RSSI filter set to {} dBm", rssiValue);
    }
    
    /**
     * Obtém o valor mínimo de RSSI configurado para filtragem.
     * 
     * @return O valor atual do filtro de RSSI
     */
    public int getMinimumRssi() {
        return minRssi;
    }
    
    /**
     * Inicia a descoberta de dispositivos BLE.
     * 
     * @return true se a varredura foi iniciada, false caso contrário
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
     * Para a descoberta de dispositivos BLE.
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
     * Limpa a lista de dispositivos descobertos.
     */
    public void clearDiscoveredDevices() {
        discoveredPeripherals.clear();
    }
    
    /**
     * Retorna uma lista de dispositivos descobertos.
     * 
     * @return Lista de dispositivos BLE encontrados durante a varredura
     */
    public List<BluetoothPeripheral> getDiscoveredDevices() {
        return new ArrayList<>(discoveredPeripherals); // Retorna uma cópia para evitar problemas de concorrência
    }
    
    /**
     * Verifica se está em processo de varredura.
     * 
     * @return true se está em varredura, false caso contrário
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Processa o dispositivo descoberto.
     * 
     * @param peripheral O dispositivo periférico
     * @param name O nome do dispositivo
     * @param address O endereço MAC do dispositivo
     * @param rssi A força do sinal (RSSI)
     */
    public void processDiscoveredDevice(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        // Verificar se o dispositivo atende ao filtro de RSSI
        if (rssi < minRssi) {
            LOGGER.debug("Device filtered out due to weak signal: {} ({}) RSSI: {}dBm", 
                (name != null && !name.isEmpty()) ? name : "Unknown", address, rssi);
            return;
        }
        
        // Verificar informações do dispositivo
        LOGGER.debug("Discovered device - Address: {}, Raw name: {}, RSSI: {}", address, name, rssi);
        
        // Se o nome estiver vazio, usar o endereço como identificação
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
} 