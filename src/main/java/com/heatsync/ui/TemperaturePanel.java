package com.heatsync.ui;

import javax.swing.*;

import com.heatsync.service.CsvLogger;

import java.awt.*;
import java.util.logging.Logger;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Panel for displaying temperature and power consumption information.
 */
public class TemperaturePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TemperaturePanel.class.getName());
    
    private JLabel cpuTempLabel;
    private JLabel gpuTempLabel;
    private JLabel diskTempLabel;
    private JLabel fanRpmLabel;

    private JButton editFanProfileButton;
    private MainWindow mainWindow;
    private int currentMode;
    private final CsvLogger csvLogger;

    /**
     * Creates a new temperature panel with all labels.
     * @param mainWindow The main window reference for layout changes
     * @param mode The mode for the panel layout (0 for default, 1 for profile edit mode)
     */
    public TemperaturePanel(MainWindow mainWindow, int mode, CsvLogger csvLogger) {
        this.csvLogger = csvLogger;
        this.mainWindow = mainWindow;
        this.currentMode = mode;
        
        initializeUI();
        updateUIForMode(mode);
    }
    
    /**
     * Initialize the UI components.
     */
    private void initializeUI() {
        setBorder(BorderFactory.createTitledBorder("Temperatures and Rpm"));
        
        cpuTempLabel = new JLabel("CPU Temperature: --,--°C");
        gpuTempLabel = new JLabel("GPU Temperature: --,--°C");
        diskTempLabel = new JLabel("Disk Temperature: --,--°C");
        fanRpmLabel = new JLabel("Fan RPM: ---RPM");
        

        editFanProfileButton = new JButton("Edit Fan Profile");
        editFanProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainWindow.applyLayout(1);
            }
        });
    }
    
    /**
     * Updates the UI layout based on the specified mode.
     * 
     * @param mode The mode for the panel layout (0 for default with edit button, 1 for profile edit mode without button)
     */
    public void updateUIForMode(int mode) {
        if (mode == 0){
            setLayout(new GridLayout(6, 1, 5, 5));
        } else {
            setLayout(new GridLayout(0, 4, 1, 1));
        }
        this.currentMode = mode;
        
        // Remove all components first
        removeAll();
        
        // Add the temperature labels (common to both modes)
        add(cpuTempLabel);
        add(gpuTempLabel);
        add(diskTempLabel);
        add(fanRpmLabel);
        // Only add edit button in mode 0 (default mode)
        if (mode == 0) {
            // Enable or disable the button based on the Bluetooth connection status.
            // Assumes mainWindow.getBluetoothService().isConnected() returns a boolean.
            editFanProfileButton.setEnabled(mainWindow.getBluetoothService().isConnected());
            add(editFanProfileButton);
            
            LOGGER.fine("Temperature panel in default mode with edit button");
        } else {
            add(new JLabel("")); // Placeholder in profile edit mode
            LOGGER.fine("Temperature panel in profile edit mode without edit button");
        }
        
        // These steps are crucial to refresh the UI
        revalidate();
        repaint();
    }
    
    /**
     * Sets the mode of the panel and updates its UI accordingly.
     * 
     * @param mode The mode for the panel layout (0 for default, 1 for profile edit mode)
     */
    public void setMode(int mode) {
        if (this.currentMode != mode) {
            updateUIForMode(mode);
        }
    }
    
    /**
     * Gets the current mode of the panel.
     * 
     * @return The current mode (0 for default, 1 for profile edit mode)
     */
    public int getMode() {
        return this.currentMode;
    }

    
    /**
     * Updates the CPU temperature display.
     * 
     * @param temperature The CPU temperature in Celsius
     */
    public void updateCpuTemperature(double temperature) {
        SwingUtilities.invokeLater(() -> 
            cpuTempLabel.setText(String.format("CPU Temperature: %.2f°C", temperature)));
    }
    
    /**
     * Updates the GPU temperature display.
     * 
     * @param temperature The GPU temperature in Celsius
     */
    public void updateGpuTemperature(double temperature) {
        SwingUtilities.invokeLater(() -> 
            gpuTempLabel.setText(String.format("GPU Temperature: %.2f°C", temperature)));
    }
    
    /**
     * Updates the disk temperature display.
     * 
     * @param temperature The disk temperature in Celsius
     */
    public void updateDiskTemperature(double temperature) {
        SwingUtilities.invokeLater(() -> 
            diskTempLabel.setText(String.format("Disk Temperature: %.2f°C", temperature)));
    }
    
    /**
     * Updates the fan RPM display.
     * 
     * @param rpm The fan RPM
     */
    public void updateFanRpm(int rpm) {
        LOGGER.info("TemperaturePanel.updateFanRpm() chamado com rpm = " + rpm);
        SwingUtilities.invokeLater(() -> 
            fanRpmLabel.setText(String.format("Fan RPM: %d", rpm)));
    }
}