package com.heatsync.service;

import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for monitoring CPU and GPU power consumption.
 * Uses the JPowerMonitor library to obtain power information.
 */
public class PowerMonitor {
    private static final Logger LOGGER = Logger.getLogger(PowerMonitor.class.getName());
    
    private static final String CONFIG_FILE = "jpowermonitor.yaml";
    private double cpuPowerWatts = 0.0;
    private double gpuPowerWatts = 0.0;
    private boolean initialized = false;
    
    // Estimated consumption values for different CPU loads
    private static final double CPU_MAX_WATTS = 65.0; // Maximum CPU consumption value in watts (depends on processor)
    private static final double CPU_MIN_WATTS = 10.0; // CPU consumption value in idle
    
    // Estimated consumption values for different GPU loads
    private static final double GPU_MAX_WATTS = 75.0; // Maximum GPU consumption value (depends on graphics card)
    private static final double GPU_MIN_WATTS = 5.0;  // GPU consumption value in idle
    
    private final TemperatureMonitor temperatureMonitor;
    
    /**
     * Constructor that receives a TemperatureMonitor instance for access to load data.
     * 
     * @param temperatureMonitor The existing temperature monitor
     */
    public PowerMonitor(TemperatureMonitor temperatureMonitor) {
        this.temperatureMonitor = temperatureMonitor;
        initialize();
    }
    
    /**
     * Initializes the power monitor.
     * Checks if the JPowerMonitor configuration file exists.
     */
    private void initialize() {
        // Check if the configuration file exists
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            LOGGER.info("JPowerMonitor configuration file not found. Using load-based estimates.");
            createDefaultConfigFile();
        } else {
            LOGGER.info("Using existing JPowerMonitor configuration file: " + configFile.getAbsolutePath());
        }
        
        initialized = true;
    }
    
    /**
     * Creates a default configuration file for JPowerMonitor.
     */
    private void createDefaultConfigFile() {
        String defaultConfig = 
            "measurement:\n" +
            "  method: est\n" +
            "  est:\n" +
            "    cpuMinWatts: " + CPU_MIN_WATTS + "\n" +
            "    cpuMaxWatts: " + CPU_MAX_WATTS + "\n";
        
        try {
            Files.write(Paths.get(CONFIG_FILE), defaultConfig.getBytes());
            LOGGER.info("Default JPowerMonitor configuration file created: " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.warning("Could not create default configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Updates power consumption information.
     * Calculates power consumption based on CPU/GPU load.
     */
    public void updatePowerConsumption() {
        if (!initialized) {
            initialize();
        }
        
        // Get CPU load via temperatureMonitor
        // This is an estimated implementation based on temperature
        // A more accurate algorithm would be based on actual CPU load
        double cpuTemp = temperatureMonitor.getCpuTemperature();
        double gpuTemp = temperatureMonitor.getGpuTemperature();
        
        // Estimate load based on temperature (simplification)
        // Assumes idle temperature of 30°C and maximum temperature of 90°C
        double cpuLoadPercentage = estimateLoadFromTemperature(cpuTemp, 30.0, 90.0);
        double gpuLoadPercentage = estimateLoadFromTemperature(gpuTemp, 30.0, 90.0);
        
        // Calculate estimated power consumption
        cpuPowerWatts = CPU_MIN_WATTS + (cpuLoadPercentage/100.0) * (CPU_MAX_WATTS - CPU_MIN_WATTS);
        gpuPowerWatts = GPU_MIN_WATTS + (gpuLoadPercentage/100.0) * (GPU_MAX_WATTS - GPU_MIN_WATTS);
        
        LOGGER.fine(String.format("Estimated consumption: CPU=%.2fW (load: %.1f%%), GPU=%.2fW (load: %.1f%%)", 
            cpuPowerWatts, cpuLoadPercentage, gpuPowerWatts, gpuLoadPercentage));
    }
    
    /**
     * Estimates load percentage based on temperature.
     * 
     * @param currentTemp The current component temperature
     * @param idleTemp The expected temperature when the component is idle
     * @param maxTemp The maximum expected temperature when the component is under full load
     * @return The estimated load percentage (0-100)
     */
    private double estimateLoadFromTemperature(double currentTemp, double idleTemp, double maxTemp) {
        // Limit values to avoid negative percentages or above 100%
        if (currentTemp <= idleTemp) return 0.0;
        if (currentTemp >= maxTemp) return 100.0;
        
        // Calculate percentage linearly
        return ((currentTemp - idleTemp) / (maxTemp - idleTemp)) * 100.0;
    }
    
    /**
     * Gets the estimated CPU power consumption in watts.
     * 
     * @return CPU consumption in watts
     */
    public double getCpuPowerWatts() {
        return cpuPowerWatts;
    }
    
    /**
     * Gets the estimated GPU power consumption in watts.
     * 
     * @return GPU consumption in watts
     */
    public double getGpuPowerWatts() {
        return gpuPowerWatts;
    }
    
    /**
     * Gets the estimated total consumption (CPU + GPU) in watts.
     * 
     * @return Total consumption in watts
     */
    public double getTotalPowerWatts() {
        return cpuPowerWatts + gpuPowerWatts;
    }
} 