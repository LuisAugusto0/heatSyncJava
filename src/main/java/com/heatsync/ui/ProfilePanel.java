package com.heatsync.ui;

import com.heatsync.service.*;

import javax.swing.*;
import java.awt.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Panel for displaying temperature and power consumption information.
 */
public class ProfilePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProfilePanel.class.getName());
    
    private MainWindow mainWindow;
    private BluetoothService bluetoothService; // Declare bluetoothService
    // private JLabel cpuPowerLabel;
    // private JLabel gpuPowerLabel;
    // private JLabel totalPowerLabel;
    
    /**
     * Creates a new temperature panel with all labels.
     */
    public ProfilePanel(MainWindow mainWindow) {
        this.bluetoothService = mainWindow.getBluetoothService(); // Initialize bluetoothService
        initializeUI();
        initializeUI();
    }

    /**
     * Initialize the UI components and layout.
     */
    private void initializeUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new GridLayout(5, 1, 5, 5));
        setBorder(BorderFactory.createTitledBorder("Temperatures and Rpm"));
        
         // Create profile configuration section
         JPanel configPanel = new JPanel(new GridLayout(6, 2, 10, 10));
         configPanel.setBorder(BorderFactory.createTitledBorder("Fan Profile Configuration"));
         
         // Temperature thresholds
         configPanel.add(new JLabel("CPU Min Temp (°C):"));
         JSpinner cpuMinSpinner = new JSpinner(new SpinnerNumberModel(40, 30, 70, 1));
         configPanel.add(cpuMinSpinner);
         
         configPanel.add(new JLabel("CPU Max Temp (°C):"));
         JSpinner cpuMaxSpinner = new JSpinner(new SpinnerNumberModel(75, 50, 95, 1));
         configPanel.add(cpuMaxSpinner);
         
         configPanel.add(new JLabel("GPU Min Temp (°C):"));
         JSpinner gpuMinSpinner = new JSpinner(new SpinnerNumberModel(45, 30, 70, 1));
         configPanel.add(gpuMinSpinner);
         
         configPanel.add(new JLabel("GPU Max Temp (°C):"));
         JSpinner gpuMaxSpinner = new JSpinner(new SpinnerNumberModel(80, 50, 95, 1));
         configPanel.add(gpuMaxSpinner);
         
         // Fan speed settings
         configPanel.add(new JLabel("Min Fan Speed (%):"));
         JSpinner minSpeedSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 100, 5));
         configPanel.add(minSpeedSpinner);
         
         configPanel.add(new JLabel("Max Fan Speed (%):"));
         JSpinner maxSpeedSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 100, 5));
         configPanel.add(maxSpeedSpinner);
         
         // Button to send profile to device
         JButton sendProfileButton = new JButton("Send Profile to Device");
         sendProfileButton.addActionListener(e -> {
             if (bluetoothService != null && bluetoothService.isConnected()) {
                 // Get values from spinners
                 int cpuMin = (Integer) cpuMinSpinner.getValue();
                 int cpuMax = (Integer) cpuMaxSpinner.getValue();
                 int gpuMin = (Integer) gpuMinSpinner.getValue();
                 int gpuMax = (Integer) gpuMaxSpinner.getValue();
                 
                 // Send data to device
                 // boolean success = bluetoothService.sendProfileData('D', cpuMin, gpuMin, cpuMax, gpuMax);
                 
                 // if (success) {
                 //     logMessage("Fan profile sent successfully");
                 // } else {
                 //     logMessage("Failed to send fan profile");
                 // }
             } else {
                 logMessage("Cannot send profile: No device connected");
             }
         });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(sendProfileButton);
        panel.add(buttonPanel);
    }
    
    /**
     * Logs a message to the logger.
     *
     * @param message The message to log.
     */
    private void logMessage(String message) {
        LOGGER.info(message);
    }
} 