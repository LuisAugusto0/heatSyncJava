package com.heatsync.service;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.components.Disk;
import com.profesorfalken.jsensors.model.components.Gpu;
import com.profesorfalken.jsensors.model.sensors.Temperature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for monitoring system component temperatures.
 * Uses the JSensors library to obtain sensor information.
 */
public class TemperatureMonitor {
    private static final Logger LOGGER = Logger.getLogger(TemperatureMonitor.class.getName());
    
    // Temperature threshold values for normalization
    private static final double MILLIGRADE_THRESHOLD = 1000.0;
    private static final double DECIGRADE_THRESHOLD = 200.0;
    private static final double FAHRENHEIT_THRESHOLD = 100.0;
    
    // Conversion factors
    private static final double MILLIGRADE_FACTOR = 100.0;
    private static final double DECIGRADE_FACTOR = 10.0;
    
    private List<Cpu> cpus;
    private List<Gpu> gpus;
    private List<Disk> disks;


    private static Map<String, String> config;

    /**
     * Default constructor, initializes components and their sensors.
     */
    public TemperatureMonitor() {
        initializeComponents();
    }

    /**
     * Initializes and updates components and their sensors.
     * Enables debug mode in JSensors and logs detailed information.
     */
    private void initializeComponents() {
        // Enable debug mode in JSensors
        config = new HashMap<>();
        config.put("debugMode", "false");
        
        cpus = JSensors.get.config(config).components().cpus;
        gpus = JSensors.get.config(config).components().gpus;
        disks = JSensors.get.config(config).components().disks;
        
        // Log detailed sensor information
        // logSensorDetails();
    }

    private void updateComponents() {
        cpus = JSensors.get.config(config).components().cpus;
        gpus = JSensors.get.config(config).components().gpus;
        disks = JSensors.get.config(config).components().disks;
        
        // Log detailed sensor information
        // logSensorDetails();
    }
    
    
    /**
     * Logs detailed information about all sensors found.
     */
    // private void logSensorDetails() {
    //     LOGGER.info("===== DETAILED SENSOR INFORMATION =====");
        
    //     logComponentDetails("CPU", cpus);
    //     logComponentDetails("GPU", gpus);
    //     logComponentDetails("DISK", disks);
        
    //     LOGGER.info("=======================================");
    // }
    
    /**
     * Logs detailed information for a specific category of components.
     * 
     * @param componentType The component type (CPU, GPU, DISK)
     * @param components The list of components to log
     */
    // private <T> void logComponentDetails(String componentType, List<T> components) {
    //     if (components == null || components.isEmpty()) {
    //         LOGGER.info(componentType + " SENSORS: None found");
    //         return;
    //     }
        
    //     LOGGER.info(componentType + " SENSORS:");
        
    //     components.forEach(component -> {
    //         String name = "";
    //         List<Temperature> temperatures = null;
            
    //         if (component instanceof Cpu) {
    //             Cpu cpu = (Cpu) component;
    //             name = cpu.name;
    //             temperatures = cpu.sensors.temperatures;
    //         } else if (component instanceof Gpu) {
    //             Gpu gpu = (Gpu) component;
    //             name = gpu.name;
    //             temperatures = gpu.sensors.temperatures;
    //         } else if (component instanceof Disk) {
    //             Disk disk = (Disk) component;
    //             name = disk.name;
    //             temperatures = disk.sensors.temperatures;
    //         }
            
    //         LOGGER.info(componentType + " Name: " + name);
            
    //         if (temperatures != null) {
    //             temperatures.forEach(temp -> 
    //                 LOGGER.info("  Temperature: " + temp.name + " = " + temp.value + 
    //                            " (Normalized: " + normalizeTemperature(temp.value) + "°C)"));
    //         }
    //     });
    // }

    /**
     * Gets the average CPU temperature.
     * 
     * @return The average CPU temperature in degrees Celsius
     */
    public double getCpuTemperature() {
        return cpus.stream()
                .flatMap(cpu -> cpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Gets the average GPU temperature.
     * 
     * @return The average GPU temperature in degrees Celsius
     */
    public double getGpuTemperature() {
        return gpus.stream()
                .flatMap(gpu -> gpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Gets the average disk temperature.
     * 
     * @return The average disk temperature in degrees Celsius
     */
    public double getDiskTemperature() {
        return disks.stream()
                .flatMap(disk -> disk.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Normalizes temperatures to the appropriate Celsius scale based on the reported value.
     * 
     * @param value The raw temperature value
     * @return The normalized value in Celsius
     */
    private double normalizeTemperature(double value) {
        LOGGER.log(Level.FINE, "Normalizing temperature: {0}", value);
        
        if (value > MILLIGRADE_THRESHOLD) {
            // If the value is in thousands, it's probably milligrade (mC)
            // Example: 3385 mC = 33.85 C
            return value / MILLIGRADE_FACTOR;
        } else if (value > DECIGRADE_THRESHOLD) {
            // If the value is between 200 and 1000, it's probably decigrade (dC)
            // Example: 538 dC = 53.8 C
            return value / DECIGRADE_FACTOR;
        } else if (value > FAHRENHEIT_THRESHOLD) {
            // Some sensors may report in Fahrenheit
            // Convert to Celsius: (F - 32) * 5/9
            return (value - 32.0) * 5.0 / 9.0;
        } else {
            // Values below 100 are probably already in Celsius
            return value;
        }
    }

    /**
     * Updates temperature information for all components.
     */
    public void updateTemperatures() {
        // Update component data
        updateComponents();
    }
} 