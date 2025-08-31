package com.heatsync.service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Component;
import com.profesorfalken.jsensors.model.components.Components;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.components.Disk;
import com.profesorfalken.jsensors.model.components.Gpu;
import com.profesorfalken.jsensors.model.sensors.Temperature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.components.Disk;
import com.profesorfalken.jsensors.model.components.Gpu;
public class TemperatureMonitor {
    private static final Logger LOGGER = Logger.getLogger(TemperatureMonitor.class.getName());
    
    // Temperature threshold values for normalization
    private static final double MILLIGRADE_THRESHOLD = 1000.0;
    private static final double DECIGRADE_THRESHOLD = 200.0;
    private static final double FAHRENHEIT_THRESHOLD = 100.0;
    
    // Conversion factors
    private static final double MILLIGRADE_FACTOR = 100.0;
    private static final double DECIGRADE_FACTOR = 10.0;
    
    // Components for Jsensors
    private List<Cpu> cpus;
    private double cpuTemperature;
    private List<Gpu> gpus;
    private double gpuTemperature;
    // private List<Disk> disks;
    private double diskTemperature = 0;

    // Libre Hardware info reading components
    private URL hwInfo;
    private String hwInfoPort = "8085";
    private boolean isHwInfoWorking = false;
    private InputStream hwInputStream;

    private List<Disk> disks;
    JSensors jSensorsTerminal;

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
    private void initializeComponents(){
        try {
            isHwInfoWorking = openInputStream();
        } catch (SocketException e) {
            System.out.println("SocketException occurred while initializing components: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException occurred while initializing components: " + e.getMessage());
        }

        if (!isHwInfoWorking){
            // Enable debug mode in JSensors
            config = new HashMap<>();
            config.put("debugMode", "false");
            
            cpus = JSensors.get.config(config).components().cpus;
            gpus = JSensors.get.config(config).components().gpus;
            // disks = JSensors.get.config(config).components().disks;
            
            // Log detailed sensor information
            // logSensorDetails();
        }
    }

    private void refreshHwInputStream() throws SocketException {
        try {
            if (hwInputStream != null) {
                hwInputStream.close();
            }
            hwInputStream = hwInfo.openStream();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error refreshing input stream: {0}", e.getMessage());
            isHwInfoWorking = false;
            LOGGER.info("Attempting to reopen connection to Libre Hardware Monitor...");
            initializeComponents();
        }
    }

    /**
     * Updates temperature readings.
     */
    public void updateTemperatures() {
        if (!isHwInfoWorking){
            // Update component data using JSensors
            updateJsensorsData();
        } else {
            // Update component data using Libre Hardware Monitor
            updateHmonitorData();
            try {
                refreshHwInputStream();
            } catch (SocketException e) {
                LOGGER.log(Level.WARNING, "Error refreshing input stream: {0}", e.getMessage());
            }
        }
    }

    /**
     * Updates component data from JSensors.
     */
    private void updateJsensorsData() {
        cpus = JSensors.get.components().cpus;
        cpuTemperature = cpus.stream()
                .flatMap(cpu -> cpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
        gpus = JSensors.get.components().gpus;
        gpuTemperature = gpus.stream()
                .flatMap(gpu -> gpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Updates component data from Libre Hardware Monitor.
     */
    private void updateHmonitorData() {
        // Implementation for updating data from Libre Hardware Monitor
        BufferedReader bf = new BufferedReader(new InputStreamReader(hwInputStream));
        String jsonData = "";
        String line;
        try {
            while ((line = bf.readLine()) != null) {
                jsonData += line;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading data from Libre Hardware Monitor: {0}", e.getMessage());
        }

        if (jsonData.isEmpty()) {
            LOGGER.warning("No data received from Libre Hardware Monitor.");
            initializeComponents(); // Try to reinitialize connection
            return;
        }

        // Extract CPU temperatures
        List<Double> cpuTemperatures = extractTemperatures(jsonData, "CPU");
        if (!cpuTemperatures.isEmpty()) {
            cpuTemperature = cpuTemperatures.get(1); // Use first CPU temperature found
        }
        
        // Extract GPU temperatures
        List<Double> gpuTemperatures = extractTemperatures(jsonData, "GPU");
        if (!gpuTemperatures.isEmpty()) {
            gpuTemperature = gpuTemperatures.get(0); // Use first GPU temperature found
        }
    }

    /*
     * Opens an input stream to the Libre Hardware Monitor server.
     */
    boolean openInputStream() throws SocketException {
        boolean successfulConnection = false;
 
        // List<InetAddress> IPs = new ArrayList<>();
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while(e.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                String ipAddress = i.getHostAddress();
                System.out.println(ipAddress);
                try{
                    URI tempURI = new URI("http://" + ipAddress + ":" + hwInfoPort + "/data.json");
                    hwInfo = tempURI.toURL();
                    hwInputStream = hwInfo.openStream();
                    successfulConnection = true;
                    System.out.println("Conection created with libre hardware monitor local server");
                } catch (ConnectException connectException){
                    System.out.println("An error ocurred while trying to connect to " + ipAddress + ":" + hwInfoPort);
                } catch (URISyntaxException syntaxException){
                    System.out.println("Malformed URI - " + "http://" + ipAddress + ":" + hwInfoPort + "/data.json");
                } catch (MalformedURLException malformedURLException){
                    System.out.println("Malformed URL - " + "http://" + ipAddress + ":" + hwInfoPort + "/data.json");
                } catch (IOException ioException){
                    System.out.println("I/O error occurred while trying to connect to " + ipAddress + ":" + hwInfoPort);
                }
            }
        }
        
        return successfulConnection;
    }

    /**
     * Extracts temperature values from JSON data for specific component types.
     * 
     * @param jsonData JSON response from LibreHardwareMonitor
     * @param componentPattern Regex pattern to match component names
     * @return List of temperature values found
     */
    private List<Double> extractTemperatures(String jsonData, String componentPattern) {
        List<Double> temperatures = new ArrayList<>();
        
        try {
            // Pattern to match temperature entries in LibreHardwareMonitor JSON
            // LibreHardwareMonitor uses format like: "Text":"CPU Core #1","Value":"45,0 °C"
            // or "Text":"GPU Temperature","Value":"52,5 °C"
            
            Pattern sensorPattern = Pattern.compile(
                "\"Text\"\\s*:\\s*\"[^\"]*(?:" + componentPattern + ")[^\"]*\"[^}]*?\"Value\"\\s*:\\s*\"([0-9]+(?:[,.]?[0-9]+)?)\\s*°C\"", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            Matcher matcher = sensorPattern.matcher(jsonData);
            while (matcher.find()) {
                try {
                    String tempStr = matcher.group(1).replace(",", "."); // Handle European decimal format
                    double temp = Double.parseDouble(tempStr);
                    if (temp > 0 && temp < 150) { // Reasonable temperature range
                        temperatures.add(temp);
                        LOGGER.log(Level.FINE, "Found temperature: {0}°C for pattern: {1}", 
                                  new Object[]{temp, componentPattern});
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINE, "Failed to parse temperature value: " + matcher.group(1), e);
                }
            }
            
            // Alternative pattern for different JSON structures - try "Type":"Temperature"
            if (temperatures.isEmpty()) {
                Pattern typePattern = Pattern.compile(
                    "\"Text\"\\s*:\\s*\"[^\"]*(?:" + componentPattern + ")[^\"]*\"[^}]*?\"Type\"\\s*:\\s*\"Temperature\"[^}]*?\"Value\"\\s*:\\s*\"([0-9]+(?:[,.]?[0-9]+)?)\\s*°C\"", 
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                
                Matcher typeMatcher = typePattern.matcher(jsonData);
                while (typeMatcher.find()) {
                    try {
                        String tempStr = typeMatcher.group(1).replace(",", "."); // Handle European decimal format
                        double temp = Double.parseDouble(tempStr);
                        if (temp > 0 && temp < 150) {
                            temperatures.add(temp);
                            LOGGER.log(Level.FINE, "Found temperature (type): {0}°C for pattern: {1}", 
                                      new Object[]{temp, componentPattern});
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.FINE, "Failed to parse temperature value: " + typeMatcher.group(1), e);
                    }
                }
            }
            
            // Third pattern: Look for any temperature sensor with matching component name
            if (temperatures.isEmpty()) {
                Pattern genericPattern = Pattern.compile(
                    "\"Text\"\\s*:\\s*\"[^\"]*(?:" + componentPattern + ")[^\"]*\"[^}]*?\"Value\"\\s*:\\s*\"([0-9]+(?:[,.]?[0-9]+)?)\\s*°C\"", 
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                
                Matcher genericMatcher = genericPattern.matcher(jsonData);
                while (genericMatcher.find()) {
                    try {
                        String tempStr = genericMatcher.group(1).replace(",", "."); // Handle European decimal format
                        double temp = Double.parseDouble(tempStr);
                        if (temp > 0 && temp < 150) {
                            temperatures.add(temp);
                            LOGGER.log(Level.FINE, "Found temperature (generic): {0}°C for pattern: {1}", 
                                      new Object[]{temp, componentPattern});
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.FINE, "Failed to parse temperature value: " + genericMatcher.group(1), e);
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error extracting temperatures for pattern: " + componentPattern, e);
        }
        
        LOGGER.log(Level.FINE, "Extracted {0} temperature values for pattern: {1}", 
                  new Object[]{temperatures.size(), componentPattern});
        return temperatures;
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
        return cpuTemperature;
    }

    /**
     * Gets the average GPU temperature.
     * 
     * @return The average GPU temperature in degrees Celsius
     */
    public double getGpuTemperature() {
        return gpuTemperature;
    }

    /**
     * Gets the average disk temperature.
     * 
     * @return The average disk temperature in degrees Celsius
     */
    public double getDiskTemperature() {
        return diskTemperature;
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
} 