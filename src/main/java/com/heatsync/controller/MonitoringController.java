package com.heatsync.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.TemperatureMonitor;
import com.heatsync.service.bluetooth.BluetoothEventListener;
import com.heatsync.ui.BluetoothPanel;
import com.heatsync.ui.TemperaturePanel;

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
    private int temperatureTolerance = 3;
    private int cpuTolerancyCount = 0;
    private int gpuTolerancyCount = 0;
    private int cpuTempSnapshot = 0;
    private int gpuTempSnapshot = 0;
    private boolean canSendTemperature = false;
    
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
        }, 0, 1000); // Update every 1 seconds
    }
    
    /**
     * Updates temperature readings and updates the UI.
     */
    private void updateTemperatures() {
        temperatureMonitor.updateTemperatures();
        
        double cpuTemp = temperatureMonitor.getCpuTemperature();
        double gpuTemp = temperatureMonitor.getGpuTemperature();

        updateTemperatureCouters((int) cpuTemp, (int) gpuTemp);
        
        temperaturePanel.updateCpuTemperature(cpuTemp);
        temperaturePanel.updateGpuTemperature(gpuTemp);
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
        if (!bluetoothPanel.isAutoMode()) {
            return;
        }
        if (bluetoothService.isConnected()) {
            if (canSendTemperature) {
                double cpuTemp = temperatureMonitor.getCpuTemperature();
                double gpuTemp = temperatureMonitor.getGpuTemperature();
                double diskTemp = temperatureMonitor.getDiskTemperature();
                boolean sent = bluetoothService.sendTemperatureData(cpuTemp, gpuTemp, diskTemp);
                if (!sent && bluetoothService.isInitialized()) {
                    LOGGER.info("Bluetooth send failed. Attempting reconnect using saved MAC address...");
                    bluetoothService.reconnectToDevice();
                } else if (sent) {
                    LOGGER.info("Temperature data sent via Bluetooth: CPU=" + cpuTemp + "°C, GPU=" + gpuTemp + "°C, Disk=" + diskTemp + "°C");
                    canSendTemperature = false; // Reset flag until next significant change
                }
            } else {
                LOGGER.info("Temperature change within tolerance. Skipping Bluetooth update.");
            }
        } else if (bluetoothService.isInitialized()) {
            LOGGER.info("Bluetooth is disconnected. Attempting reconnect using saved MAC address...");
            bluetoothService.reconnectToDevice();
        }
    }

    private void updateCpuCount(int cpuTemp){
        if(cpuTemp > cpuTempSnapshot){
            if(cpuTolerancyCount < 0){
                cpuTolerancyCount = 0;
                cpuTempSnapshot = cpuTemp;
            } else {
                cpuTolerancyCount++;
            } 
            
        } else if(cpuTemp < cpuTempSnapshot){
            if(cpuTolerancyCount > 0) {
                cpuTolerancyCount = 0;
                cpuTempSnapshot = cpuTemp;
            }
            else cpuTolerancyCount--;
        }
        LOGGER.info("Tolerância cpu: " + cpuTolerancyCount + " Limite: " + temperatureTolerance);
    }

    private void updateGpuCount(int gpuTemp){
        if(gpuTemp > gpuTempSnapshot){
            if(gpuTolerancyCount < 0){
                gpuTolerancyCount = 0;
                gpuTempSnapshot = gpuTemp;
            } else {
                gpuTolerancyCount++;
            } 
            
        } else if(gpuTemp < gpuTempSnapshot){
            if(gpuTolerancyCount > 0) {
                gpuTolerancyCount = 0;
                gpuTempSnapshot = gpuTemp;
            }
            else gpuTolerancyCount--;
        }
        LOGGER.info("Tolerância gpu: " + gpuTolerancyCount + " Limite: " + temperatureTolerance);
    }

    private void updateTemperatureCouters(int cpuTemp, int gpuTemp){
        if(isInTolerance()) {
            canSendTemperature = false;
            updateCpuCount(cpuTemp);
            updateGpuCount(gpuTemp); 
        } else {
            canSendTemperature = true;
            gpuTolerancyCount = 0;
            cpuTolerancyCount = 0;
            gpuTempSnapshot = gpuTemp;
            cpuTempSnapshot = cpuTemp;
        }
        
    }

    private boolean isInTolerance(){
        return Math.abs(gpuTolerancyCount) < temperatureTolerance && Math.abs(cpuTolerancyCount) < temperatureTolerance;
    }

    public void setTemperatureTolerance(int temperatureTolerance) {
        this.temperatureTolerance = Math.max(1, temperatureTolerance);
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