package com.heatsync.service.bluetooth;

import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;

/**
 * Interface para notificar a UI sobre eventos Bluetooth.
 */
public interface BluetoothEventListener {
    /**
     * Chamado quando um dispositivo Bluetooth é descoberto.
     * 
     * @param peripheral O dispositivo periférico
     * @param name O nome do dispositivo
     * @param address O endereço MAC do dispositivo
     * @param rssi A força do sinal (RSSI)
     */
    void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi);
    
    /**
     * Chamado quando um dispositivo é conectado com sucesso.
     * 
     * @param peripheral O dispositivo conectado
     */
    void onDeviceConnected(BluetoothPeripheral peripheral);
    
    /**
     * Chamado quando um dispositivo é desconectado.
     * 
     * @param peripheral O dispositivo desconectado
     * @param status O status da desconexão
     */
    void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status);
    
    /**
     * Chamado quando ocorre uma falha na varredura.
     * 
     * @param errorCode O código de erro
     */
    void onScanFailed(int errorCode);
} 