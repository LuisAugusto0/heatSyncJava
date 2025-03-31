package com.heatsync.ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Panel for displaying temperature and power consumption information.
 */
public class TemperaturePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(TemperaturePanel.class.getName());
    
    private JLabel cpuTempLabel;
    private JLabel gpuTempLabel;
    private JLabel diskTempLabel;
    private JLabel cpuPowerLabel;
    private JLabel gpuPowerLabel;
    private JLabel totalPowerLabel;
    
    /**
     * Creates a new temperature panel with all labels.
     */
    public TemperaturePanel() {
        initializeUI();
    }
    
    /**
     * Initialize the UI components and layout.
     */
    private void initializeUI() {
        setLayout(new GridLayout(6, 1, 5, 5));
        setBorder(BorderFactory.createTitledBorder("Temperaturas e Consumo"));
        
        cpuTempLabel = new JLabel("CPU Temperature: --°C");
        gpuTempLabel = new JLabel("GPU Temperature: --°C");
        diskTempLabel = new JLabel("Disk Temperature: --°C");
        cpuPowerLabel = new JLabel("CPU Power: --W");
        gpuPowerLabel = new JLabel("GPU Power: --W");
        totalPowerLabel = new JLabel("Total Power: --W");
        
        add(cpuTempLabel);
        add(gpuTempLabel);
        add(diskTempLabel);
        add(cpuPowerLabel);
        add(gpuPowerLabel);
        add(totalPowerLabel);
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
     * Updates the CPU power consumption display.
     * 
     * @param watts The CPU power consumption in watts
     */
    public void updateCpuPower(double watts) {
        SwingUtilities.invokeLater(() -> 
            cpuPowerLabel.setText(String.format("CPU Power: %.2fW", watts)));
    }
    
    /**
     * Updates the GPU power consumption display.
     * 
     * @param watts The GPU power consumption in watts
     */
    public void updateGpuPower(double watts) {
        SwingUtilities.invokeLater(() -> 
            gpuPowerLabel.setText(String.format("GPU Power: %.2fW", watts)));
    }
    
    /**
     * Updates the total power consumption display.
     * 
     * @param watts The total power consumption in watts
     */
    public void updateTotalPower(double watts) {
        SwingUtilities.invokeLater(() -> 
            totalPowerLabel.setText(String.format("Total Power: %.2fW", watts)));
    }
} 