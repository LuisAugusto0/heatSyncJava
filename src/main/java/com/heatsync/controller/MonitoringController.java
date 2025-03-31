package com.heatsync.controller;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.PowerMonitor;
import com.heatsync.service.TemperatureMonitor;
import com.heatsync.ui.BluetoothPanel;
import com.heatsync.ui.TemperaturePanel;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Controller that coordinates temperature and power monitoring services
 * and updates the UI accordingly.
 */
public class MonitoringController {
    private static final Logger LOGGER = Logger.getLogger(MonitoringController.class.getName());
    
    private final TemperatureMonitor temperatureMonitor;
    private final PowerMonitor powerMonitor;
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
            PowerMonitor powerMonitor,
            BluetoothService bluetoothService,
            TemperaturePanel temperaturePanel,
            BluetoothPanel bluetoothPanel) {
        this.temperatureMonitor = temperatureMonitor;
        this.powerMonitor = powerMonitor;
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
                updatePowerConsumption();
                sendDataIfNeeded();
            }
        }, 0, 2000); // Update every 2 seconds
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
    }
    
    /**
     * Updates power consumption readings and updates the UI.
     */
    private void updatePowerConsumption() {
        powerMonitor.updatePowerConsumption();
        
        double cpuPower = powerMonitor.getCpuPowerWatts();
        double gpuPower = powerMonitor.getGpuPowerWatts();
        double totalPower = powerMonitor.getTotalPowerWatts();
        
        temperaturePanel.updateCpuPower(cpuPower);
        temperaturePanel.updateGpuPower(gpuPower);
        temperaturePanel.updateTotalPower(totalPower);
    }
    
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
} 