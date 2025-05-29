package com.heatsync;

import com.heatsync.controller.MonitoringController;
import com.heatsync.service.BluetoothService;
import com.heatsync.service.TemperatureMonitor;
import com.heatsync.service.configIO.ConfigIOException;
import com.heatsync.service.configIO.FanProfileIOService;
import com.heatsync.ui.MainWindow;
import com.profesorfalken.jsensors.model.sensors.Fan;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Main application class for HeatSync.
 * Initializes services and UI components.
 */
public class HeatSyncApp {
    private static final Logger LOGGER = Logger.getLogger(HeatSyncApp.class.getName());
    
    private TemperatureMonitor temperatureMonitor;
    private BluetoothService bluetoothService;
    private MainWindow mainWindow;
    private MonitoringController monitoringController;
    
    /**
     * Initializes the application components.
     */
    public HeatSyncApp() {
        // Initialize services
        initializeServices();
        
        // Initialize UI
        mainWindow = new MainWindow(temperatureMonitor, bluetoothService);
        
        // Initialize controller
        monitoringController = new MonitoringController(
                temperatureMonitor,
                bluetoothService,
                mainWindow.getTemperaturePanel(),
                mainWindow.getBluetoothPanel());
        
        // Start monitoring
        monitoringController.startMonitoring();
    }
    
    /**
     * Initializes services.
     */
    private void initializeServices() {
        temperatureMonitor = new TemperatureMonitor();
        bluetoothService = new BluetoothService();

        
        if(FanProfileIOService.getMacAddress() == null && bluetoothService.isInitialized()) {
            bluetoothService.connectToDevice(FanProfileIOService.getMacAddress());
        }
    }
    
    /**
     * Shows the main application window.
     */
    public void show() {
        mainWindow.show();
    }

    /**
     * Application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.warning("Could not set the system Look and Feel.");
        }
        
        try {
            // If keepStateFlag is set to true, missing fields of the config file
            // will not be updated with defaults. Instead warn about the errors and exits
            // On false, no exceptions are received, even with no reading permission
            FanProfileIOService.initiate(false);
        } catch (ConfigIOException e) {
            e.printStackTrace();
            System.exit(1); //User opted exit from keepStateFlag being true
        } 
        
        SwingUtilities.invokeLater(() -> {
            HeatSyncApp app = new HeatSyncApp();
            app.show();
        });
    }
} 