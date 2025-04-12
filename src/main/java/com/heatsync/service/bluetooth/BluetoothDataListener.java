package com.heatsync.service.bluetooth;

public interface BluetoothDataListener {
    /**
     * Notifica quando um novo valor de RPM é recebido.
     * 
     * @param rpm O valor de RPM recebido
     */
    void onFanRpmReceived(int rpm);
}