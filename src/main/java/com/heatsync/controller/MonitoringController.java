package com.heatsync.controller;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.TemperatureMonitor;
import com.heatsync.ui.BluetoothPanel;
import com.heatsync.ui.TemperaturePanel;
import com.heatsync.service.bluetooth.BluetoothEventListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Controller that coordinates temperature and power monitoring services
 * and updates the UI accordingly.
 */
public class MonitoringController implements BluetoothEventListener {
    private static final Logger LOGGER = Logger.getLogger(MonitoringController.class.getName());
    
    private final TemperatureMonitor temperatureMonitor;
    private final BluetoothService bluetoothService;
    private final TemperaturePanel temperaturePanel;
    private final BluetoothPanel bluetoothPanel;
    
    private Timer updateTimer;
    
    /**
     * Creates a new controller for temperature and power monitoring.
     * 
     * @param temperatureMonitor The temperature monitoring service
     * @param powerMonitor The power monitoring service
     * @param bluetoothService The Bluetooth service
     * @param temperaturePanel The temperature display panel
     * @param bluetoothPanel The Bluetooth control panel
     */
    public MonitoringController(
            TemperatureMonitor temperatureMonitor, 
            BluetoothService bluetoothService,
            TemperaturePanel temperaturePanel,
            BluetoothPanel bluetoothPanel) {
        this.temperatureMonitor = temperatureMonitor;
        this.bluetoothService = bluetoothService;
        this.temperaturePanel = temperaturePanel;
        this.bluetoothPanel = bluetoothPanel;
    }
    
    /**
     * Starts periodic updates for temperature and power monitoring.
     */
    public void startMonitoring() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTemperatures();
                // updatePowerConsumption();
                sendDataIfNeeded();
            }
        }, 0, 3000); // Update every 3 seconds
    }
    
    /**
     * Updates temperature readings and updates the UI.
     */
    private void updateTemperatures() {
        temperatureMonitor.updateTemperatures();
        
        double cpuTemp = temperatureMonitor.getCpuTemperature();
        double gpuTemp = temperatureMonitor.getGpuTemperature();
        double diskTemp = temperatureMonitor.getDiskTemperature();
        
        temperaturePanel.updateCpuTemperature(cpuTemp);
        temperaturePanel.updateGpuTemperature(gpuTemp);
        temperaturePanel.updateDiskTemperature(diskTemp);
        // LOGGER.info("updateTemperatures() chamando temperaturePanel.updateFanRpm com rpm = " + currentFanRpm);
        // temperaturePanel.updateFanRpm(currentFanRpm);
    }
    
    /**
     * Updates the fan RPM value and updates the UI.
     * 
     * @param rpm The new fan RPM value
     */
    // public void updateFanRpmValue(int rpm) {
    //     LOGGER.info("updateFanRpmValue() chamado com rpm = " + rpm);
    //     currentFanRpm = rpm;
    //     temperaturePanel.updateFanRpm(rpm);
    // }
    
    /**
     * Updates power consumption readings and updates the UI.
     */
    // private void updatePowerConsumption() {
    //     powerMonitor.updatePowerConsumption();
        
    //     double cpuPower = powerMonitor.getCpuPowerWatts();
    //     double gpuPower = powerMonitor.getGpuPowerWatts();
    //     double totalPower = powerMonitor.getTotalPowerWatts();
        
    //     temperaturePanel.updateCpuPower(cpuPower);
    //     temperaturePanel.updateGpuPower(gpuPower);
    //     temperaturePanel.updateTotalPower(totalPower);
    // }
    
    /**
     * Sends temperature data to connected Bluetooth device if needed.
     */
    private void sendDataIfNeeded() {
        if (bluetoothService.isConnected()) {
            // In auto mode, send temperature data to device
            if (bluetoothPanel.isAutoMode()) {
                double cpuTemp = temperatureMonitor.getCpuTemperature();
                double gpuTemp = temperatureMonitor.getGpuTemperature();
                double diskTemp = temperatureMonitor.getDiskTemperature();
                
                bluetoothService.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
            }
        }
    }
    
    /**
     * Stops monitoring and cleans up resources.
     */
    public void stopMonitoring() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    @Override
    public void onFanRpmReceived(int rpm) {
    //     LOGGER.info("onFanRpmReceived() chamado com rpm = " + rpm);
    //     updateFanRpmValue(rpm);
    //     LOGGER.info("Fan RPM atualizado via Bluetooth: " + rpm);
    }

    @Override
    public void onDeviceDisconnected(Object device, int status) {
        // Ação mínima ou lógica de tratamento de desconexão
        LOGGER.info("Device disconnected: " + device + " with status: " + status);
    }
    
    @Override
    public void onDeviceDiscovered(Object device, String name, String address, int rssi) {
        LOGGER.info("Device discovered: " + name + " (" + address + ") RSSI: " + rssi);
    }

    @Override
    public void onScanFailed(int errorCode) {
        LOGGER.info("Bluetooth scan failed with error code: " + errorCode);
    }

    @Override
    public void onDeviceConnected(Object device) {
        LOGGER.info("Device connected: " + device);
    }

    @Override
    public void onScanStopped() {
        LOGGER.info("Bluetooth scan stopped.");
    }
}