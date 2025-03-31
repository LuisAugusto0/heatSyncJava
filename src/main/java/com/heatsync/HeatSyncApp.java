package com.heatsync;

import com.heatsync.controller.MonitoringController;
import com.heatsync.service.BluetoothService;
import com.heatsync.service.PowerMonitor;
import com.heatsync.service.TemperatureMonitor;
import com.heatsync.ui.MainWindow;
import javax.swing.*;
import java.util.logging.Logger;

/**
 * Main application class for HeatSync.
 * Initializes services and UI components.
 */
public class HeatSyncApp {
    private static final Logger LOGGER = Logger.getLogger(HeatSyncApp.class.getName());
    
    private TemperatureMonitor temperatureMonitor;
    private PowerMonitor powerMonitor;
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
        mainWindow = new MainWindow(temperatureMonitor, powerMonitor, bluetoothService);
        
        // Initialize controller
        monitoringController = new MonitoringController(
                temperatureMonitor,
                powerMonitor,
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
        powerMonitor = new PowerMonitor(temperatureMonitor);
        bluetoothService = new BluetoothService();
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
            LOGGER.warning("Não foi possível definir o Look and Feel do sistema.");
        }

        SwingUtilities.invokeLater(() -> {
            HeatSyncApp app = new HeatSyncApp();
            app.show();
        });
    }
} 