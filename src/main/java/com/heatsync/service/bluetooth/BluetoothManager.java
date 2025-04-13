package com.heatsync.service.bluetooth;

import javax.bluetooth.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import java.text.DecimalFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.heatsync.service.BluetoothService;

/**
 * BlueCove implementation of the Bluetooth manager.
 * Handles discovering and connecting to SPP devices.
 */
public class BluetoothManager implements DiscoveryListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(BluetoothManager.class);
    
    // Constants for discovery
    private static final int DISCOVERY_TIMEOUT_SECONDS = 10;
    private static final UUID SPP_UUID = new UUID("1101", true); // Serial Port Profile UUID
    
    private DiscoveryAgent discoveryAgent;
    private final Map<String, RemoteDevice> discoveredDevices = new HashMap<>();
    private BluetoothEventListener eventListener;
    private boolean isInitialized = false;
    private boolean isScanning = false;
    private int minimumRssi = -80; // Default RSSI filter threshold
    
    // Connection related fields
    private StreamConnection streamConnection;
    private InputStream inputStream;
    private OutputStream outputStream;
    private RemoteDevice connectedDevice;
    private boolean isConnected = false;
    private Thread readThread;
    private volatile boolean keepReading = false;
    
    // Executor for async operations
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * Creates a new Bluetooth manager using BlueCove.
     */
    public BluetoothManager() {
        init();
    }
    
    /**
     * Initializes the Bluetooth manager and its components.
     */
    private void init() {
        // Force loading of the BlueCove native library debug logs
        System.setProperty("bluecove.debug", "true");
        LOGGER.info("Initializing BluetoothManager with BlueCove...");
        LOGGER.info("Java temp directory (java.io.tmpdir): {}", System.getProperty("java.io.tmpdir"));
        try {
            // Check if BlueCove is in the classpath
            try {
                Class.forName("javax.bluetooth.LocalDevice");
                LOGGER.info("BlueCove API classes found in classpath");
            } catch (ClassNotFoundException e) {
                LOGGER.error("BlueCove API classes not found in classpath. Make sure BlueCove JAR is included.", e);
                isInitialized = false;
                return;
            }
            
            // Detect operating system
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isWindows = osName.contains("windows");
            boolean isLinux = osName.contains("linux");
            
            LOGGER.info("Operating system detected: {}", osName);
            
            // Set system properties for BlueCove based on OS
            if (isLinux) {
                LOGGER.info("Setting up BlueCove for Linux");
                // The native bluecove-gpl library should be loaded automatically 
                // if it's in the classpath
                System.setProperty("bluecove.native.path", "");
                
                // Optional: Force BlueZ stack
                System.setProperty("bluecove.stack", "BlueZ");
            } else if (isWindows) {
                LOGGER.info("Setting up BlueCove for Windows");
                // Windows uses the default stack
                System.setProperty("bluecove.stack", "winsock");
            }
            
            // Try to get the local Bluetooth device
            LOGGER.info("Attempting to get local Bluetooth device...");
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            
            LOGGER.info("Local device obtained successfully");
            discoveryAgent = localDevice.getDiscoveryAgent();
            isInitialized = true;
            LOGGER.info("BluetoothManager initialized successfully.");
            LOGGER.info("Local device address: {}", localDevice.getBluetoothAddress());
            LOGGER.info("Local device name: {}", localDevice.getFriendlyName());
        } catch (BluetoothStateException e) {
            LOGGER.error("Failed to initialize BlueCove. Bluetooth functionality might be unavailable.", e);
            LOGGER.error("BluetoothStateException details: {}", e.getMessage());
            
            // Display helpful information for troubleshooting
            if (e.getMessage() != null && e.getMessage().contains("library")) {
                LOGGER.error("Native library error detected. Make sure the Bluetooth is turned on and proper libraries are installed.");
                LOGGER.error("Windows users: Make sure your Bluetooth adapter is enabled.");
                LOGGER.error("Linux users: Make sure you have bluez and libbluetooth-dev installed.");
            }
            
            isInitialized = false;
            
            // Try an alternative initialization method
            tryAlternativeInit();
        } catch (Exception e) {
            LOGGER.error("Unexpected error during BlueCove initialization", e);
            isInitialized = false;
            
            // Try an alternative initialization method
            tryAlternativeInit();
        }
    }
    
    /**
     * Tries an alternative initialization method for BlueCove
     * which sometimes resolves issues with the default initialization
     */
    private void tryAlternativeInit() {
        LOGGER.info("Attempting alternative BlueCove initialization...");
        try {
            // Debugging was already enabled in init()
            // System.setProperty("bluecove.debug", "true");
            
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                // For Linux, try directly loading the BlueZ implementation
                Class.forName("com.intel.bluetooth.BluetoothStackBlueZ");
                LOGGER.info("Manually loaded BlueZ stack implementation");
            } else if (osName.contains("windows")) {
                // For Windows, try directly loading the Winsock implementation
                Class.forName("com.intel.bluetooth.BluetoothStackMicrosoft");
                LOGGER.info("Manually loaded Microsoft Winsock stack implementation");
            }
            
            // Try again to initialize the LocalDevice
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();
            isInitialized = true;
            LOGGER.info("Alternative initialization successful!");
            LOGGER.info("Local device address: {}", localDevice.getBluetoothAddress());
            LOGGER.info("Local device name: {}", localDevice.getFriendlyName());
        } catch (Exception e) {
            LOGGER.error("Alternative initialization failed", e);
            isInitialized = false;
        }
    }
    
    /**
     * Registers a listener for Bluetooth events.
     * 
     * @param listener The listener to be registered
     */
    public void setEventListener(BluetoothEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Sets a minimum RSSI value to filter out devices with weak signals.
     * Note: BlueCove doesn't provide RSSI directly, but we'll store the value for reference.
     * 
     * @param rssiValue The minimum RSSI value to be considered
     */
    public void setMinimumRssi(int rssiValue) {
        this.minimumRssi = rssiValue;
        LOGGER.info("Set minimum RSSI to: {} (Note: BlueCove might not support RSSI filtering directly)", rssiValue);
    }
    
    /**
     * Starts the discovery of Bluetooth devices.
     * 
     * @return true if scanning was started, false otherwise
     */
    public boolean startDeviceDiscovery() {
        if (!isInitialized) {
            LOGGER.error("Cannot start discovery, BlueCove is not initialized.");
            return false;
        }
        
        if (isScanning) {
            LOGGER.warn("Discovery already in progress.");
            return true;
        }
        
        try {
            LOGGER.info("Starting Bluetooth device discovery (indefinite)...");
            discoveredDevices.clear();
            boolean started = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
            if (started) {
                isScanning = true;
                LOGGER.info("Bluetooth discovery started successfully.");
            } else {
                LOGGER.error("Failed to start Bluetooth discovery.");
            }
            return started;
        } catch (BluetoothStateException e) {
            LOGGER.error("Error starting Bluetooth discovery", e);
            if (eventListener != null) {
                eventListener.onScanFailed(-1);
            }
            isScanning = false; // Ensure isScanning is false on failure
            return false;
        }
    }
    
    /**
     * Stops the discovery of Bluetooth devices.
     */
    public void stopDeviceDiscovery() {
        if (isScanning) {
            LOGGER.info("Stopping Bluetooth discovery...");
            try {
                discoveryAgent.cancelInquiry(this);
                LOGGER.info("Bluetooth discovery stopped.");
            } catch (Exception e) {
                LOGGER.error("Error stopping Bluetooth discovery", e);
            }
            isScanning = false;
        }
    }
    
    /**
     * Attempts to connect to a specific device by MAC address.
     * 
     * @param deviceAddress MAC address of the device
     * @return true if the connection attempt was initiated, false otherwise
     */
    public boolean connectToDevice(String deviceAddress) {
        if (!isInitialized) {
            LOGGER.error("Cannot connect, BlueCove is not initialized.");
            return false;
        }
        
        if (isConnected) {
            LOGGER.warn("Already connected to a device. Disconnecting first...");
            closeConnection();
        }
        
        final RemoteDevice device = discoveredDevices.get(deviceAddress);
        if (device == null) {
            LOGGER.error("Device with address {} not found in discovered devices.", deviceAddress);
            return false;
        }
        
        // Run the connection process asynchronously
        executor.submit(() -> {
            try {
                LOGGER.info("Attempting to connect to device: {}", deviceAddress);
                
                // First, search for services on the remote device
                LOGGER.info("Searching for SPP service on device: {}", deviceAddress);
                String connectionUrl = searchService(device);
                
                if (connectionUrl == null) {
                    LOGGER.error("No SPP service found on device: {}", deviceAddress);
                    if (eventListener != null) {
                        eventListener.onDeviceDisconnected(device, -1);
                    }
                    return;
                }
                
                // Connect to the service
                LOGGER.info("Connecting to: {}", connectionUrl);
                streamConnection = (StreamConnection) Connector.open(connectionUrl);
                
                // Get the input and output streams
                inputStream = streamConnection.openInputStream();
                outputStream = streamConnection.openOutputStream();
                
                // Mark as connected
                connectedDevice = device;
                isConnected = true;
                
                // Start the read thread
                startReadThread();
                
                // Notify success
                if (eventListener != null) {
                    eventListener.onDeviceConnected(device);
                }
                
                LOGGER.info("Successfully connected to device: {}", deviceAddress);
                
            } catch (IOException e) {
                LOGGER.error("Error connecting to device: {}", deviceAddress, e);
                closeConnection();
                
                if (eventListener != null) {
                    eventListener.onDeviceDisconnected(device, -2);
                }
            }
        });
        
        return true;
    }
    
    /**
     * Creates a SPP connection URL for a remote device.
     * 
     * @param device The remote device
     * @return The connection URL
     */
    private String searchService(RemoteDevice device) {
        // For SPP connections, we can create a direct connection URL
        // without explicit service discovery in many cases
        String deviceAddress = device.getBluetoothAddress();
        String url = "btspp://" + deviceAddress + ":1;authenticate=false;encrypt=false;master=false";
        LOGGER.info("Created SPP connection URL: {}", url);
        return url;
    }
    
    /**
     * Starts a thread that continuously reads from the input stream.
     */
    private void startReadThread() {
        keepReading = true;
        readThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (keepReading && isConnected) {
                try {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            LOGGER.debug("Received message: {}", line);
                            // Supondo que a mensagem seja somente o valor do RPM enviado pelo Arduino
                            try {
                                int rpm = Integer.parseInt(line.trim());
                                // Notifica a camada de UI ou controlador com o novo valor de RPM
                                notifyFanRpmReceived(rpm);
                            } catch (NumberFormatException ex) {
                                LOGGER.error("Mensagem de RPM inválida: {}", line, ex);
                            }
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (IOException e) {
                    if (keepReading) {
                        LOGGER.error("Erro ao ler dados do dispositivo", e);
                        closeConnection();
                        if (eventListener != null) {
                            eventListener.onDeviceDisconnected(connectedDevice, -3);
                        }
                    }
                    break;
                } catch (InterruptedException e) {
                    LOGGER.error("Thread de leitura interrompida", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            LOGGER.info("Thread de leitura finalizada");
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    /**
     * Método para notificar o RPM recebido.
     */
    private void notifyFanRpmReceived(int rpm) {
        // Try to use eventListener directly if it's a BluetoothDataListener
        if (eventListener != null) {
            LOGGER.info("notifyFanRpmReceived() chamado com rpm = " + rpm);
            eventListener.onFanRpmReceived(rpm);
        } 
        else {
            LOGGER.warn("No BluetoothEventListener available to handle RPM update: RPM value = {}", rpm);
        }
    }
    
    /**
     * Sends temperature data to the connected peripheral.
     * 
     * @param cpuTemp CPU temperature
     * @param gpuTemp GPU temperature
     * @param diskTemp Disk temperature
     * @return true if the data was sent, false otherwise
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        if (!isConnected || outputStream == null) {
            LOGGER.error("Cannot send temperature data. Not connected to any device.");
            return false;
        }
        
        try {
            DecimalFormat df = new DecimalFormat("00.0");
            // Format the temperature data as a string
            // T:CPU:GPU:DISK\n format (easily parseable by Arduino or similar)
            String data = String.format("T%s:%s:%s\n", df.format(cpuTemp), df.format(gpuTemp), df.format(diskTemp));
            byte[] bytes = data.getBytes();
            
            // Send the data
            outputStream.write(bytes);
            outputStream.flush();
            
            LOGGER.debug("Sent temperature data: {}", data.trim());
            return true;
        } catch (IOException e) {
            LOGGER.error("Error sending temperature data", e);
            return false;
        }
    }
    
    /**
     * Sends a PWM value to the connected peripheral.
     * 
     * @param pwmValue PWM value (e.g., 0-100)
     * @return true if the command was sent, false otherwise
     */
    public boolean sendPwmCommand(int pwmValue) {
        if (!isConnected || outputStream == null) {
            LOGGER.error("Cannot send PWM command. Not connected to any device.");
            return false;
        }
        
        try {
            // Format the PWM command as a string
            // P:VALUE\n format (easily parseable by Arduino or similar)
            String command = String.format("P:%d\n", pwmValue);
            byte[] bytes = command.getBytes();
            
            // Send the command
            outputStream.write(bytes);
            outputStream.flush();
            
            LOGGER.debug("Sent PWM command: {}", command.trim());
            return true;
        } catch (IOException e) {
            LOGGER.error("Error sending PWM command", e);
            return false;
        }
    }

    /**
     * Sends a profile data command to the connected peripheral.
     * 
     * Command format: L<cpuMinTemp>:<gpuMinTemp>:<cpuMaxTemp>:<gpuMaxTemp>\n
     * 
     * @param cpuMinTemp Minimum CPU temperature threshold
     * @param gpuMinTemp Minimum GPU temperature threshold
     * @param cpuMaxTemp Maximum CPU temperature threshold
     * @param gpuMaxTemp Maximum GPU temperature threshold
     * @return true if the data was sent, false otherwise
     */
    public boolean sendProfileData(int cpuMinTemp, int gpuMinTemp, int cpuMaxTemp, int gpuMaxTemp) {
        if (!isConnected || outputStream == null) {
            LOGGER.error("Cannot send profile data. Not connected to any device.");
            return false;
        }
        try {
            String data = String.format("L%d:%d:%d:%d\n", cpuMinTemp, gpuMinTemp, cpuMaxTemp, gpuMaxTemp);
            byte[] bytes = data.getBytes();
            outputStream.write(bytes);
            outputStream.flush();
            LOGGER.debug("Sent profile data: {}", data.trim());
            return true;
        } catch (IOException e) {
            LOGGER.error("Error sending profile data", e);
            return false;
        }
    }
    
    /**
     * Sends a constant command to the connected peripheral.
     * 
     * Command format: C<percentage>
     * 
     * @param percentage The constant value (0-100) to be sent
     * @return true if the command was sent, false otherwise
     */
    public boolean sendConstantCommand(int percentage) {
        if (!isConnected || outputStream == null) {
            LOGGER.error("Cannot send constant command. Not connected to any device.");
            return false;
        }
        try {
            String command = String.format("C%d", percentage);
            byte[] bytes = command.getBytes();
            outputStream.write(bytes);
            outputStream.flush();
            LOGGER.debug("Sent constant command: {}", command.trim());
            return true;
        } catch (IOException e) {
            LOGGER.error("Error sending constant command", e);
            return false;
        }
    }
    
    /**
     * Closes the connection with the current peripheral.
     */
    public void closeConnection() {
        if (isConnected) {
            RemoteDevice deviceBeingDisconnected = connectedDevice; // Store reference before nulling
            int disconnectionStatus = 0; // 0 for clean user disconnect

            // Stop the read thread
            keepReading = false;
            if (readThread != null) {
                readThread.interrupt();
                // Wait briefly for the thread to potentially finish? Optional.
                // try { readThread.join(100); } catch (InterruptedException ignored) {}
                readThread = null;
            }
            
            // Close streams and connection
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
                
                if (streamConnection != null) {
                    streamConnection.close();
                    streamConnection = null;
                }
            } catch (IOException e) {
                LOGGER.error("Error closing connection resources", e);
                disconnectionStatus = -4; // Assign a new error code for resource closing failure
            } finally {
                // Update state regardless of stream closing errors
                isConnected = false;
                connectedDevice = null;
                
                LOGGER.info("Connection closed internally.");
                
                // Notify the listener AFTER internal state is updated
                if (eventListener != null) {
                    eventListener.onDeviceDisconnected(deviceBeingDisconnected, disconnectionStatus);
                }
            }
        } else {
             LOGGER.warn("closeConnection called but already disconnected.");
        }
    }
    
    /**
     * Checks if connected to a device.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Checks if Bluetooth was correctly initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Checks if scanning is in progress.
     * 
     * @return true if scanning, false otherwise
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Shuts down the Bluetooth service, stopping scans and disconnecting.
     */
    public void shutdown() {
        LOGGER.info("Shutting down BluetoothManager...");
        
        if (isScanning) {
            stopDeviceDiscovery();
        }
        
        closeConnection();
        
        // Shutdown executor
        executor.shutdown();
        
        LOGGER.info("BluetoothManager shutdown complete.");
    }
    
    /**
     * Gets the output stream for the current connection.
     * 
     * @return The output stream or null if not connected
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }
    
    /**
     * Gets the input stream for the current connection.
     * 
     * @return The input stream or null if not connected
     */
    public InputStream getInputStream() {
        return inputStream;
    }
    
    // DiscoveryListener implementation
    
    @Override
    public void deviceDiscovered(RemoteDevice device, DeviceClass deviceClass) {
        try {
            String address = device.getBluetoothAddress();
            String name = device.getFriendlyName(false);
            
            // Sometimes name can be null or empty
            if (name == null || name.isEmpty()) {
                name = "[Unknown]";
            }
            
            LOGGER.info("Device discovered: {} ({})", name, address);
            
            // Store the device in our map
            discoveredDevices.put(address, device);
            
            // Notify the listener (use a default RSSI since BlueCove doesn't provide it)
            // The deviceClass.getMajorDeviceClass() could be used for filtering instead of RSSI
            if (eventListener != null) {
                // -50 is a moderate signal strength when we can't get the actual RSSI
                eventListener.onDeviceDiscovered(device, name, address, -50);
            }
        } catch (IOException e) {
            LOGGER.error("Error getting device name", e);
        }
    }
    
    @Override
    public void servicesDiscovered(int transID, ServiceRecord[] records) {
        LOGGER.info("Services discovered: {}", records.length);
        
        for (ServiceRecord record : records) {
            // Get the service URL
            String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            if (url != null && url.startsWith("btspp")) {
                LOGGER.info("Found SPP service: {}", url);
            }
        }
    }
    
    @Override
    public void serviceSearchCompleted(int transID, int respCode) {
        String response;
        
        switch (respCode) {
            case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
                response = "SERVICE_SEARCH_COMPLETED";
                break;
            case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
                response = "SERVICE_SEARCH_DEVICE_NOT_REACHABLE";
                break;
            case DiscoveryListener.SERVICE_SEARCH_ERROR:
                response = "SERVICE_SEARCH_ERROR";
                break;
            case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
                response = "SERVICE_SEARCH_NO_RECORDS";
                break;
            case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
                response = "SERVICE_SEARCH_TERMINATED";
                break;
            default:
                response = "Unknown Response Code: " + respCode;
                break;
        }
        
        LOGGER.info("Service search completed: {}", response);
        
        // Notify waiting threads
        synchronized (this) {
            this.notifyAll();
        }
    }
    
    @Override
    public void inquiryCompleted(int discType) {
        String completionType;
        boolean stoppedByUser = false;
        boolean unexpectedCompletion = false;
        
        switch (discType) {
            case DiscoveryListener.INQUIRY_COMPLETED:
                completionType = "INQUIRY_COMPLETED (Unexpected)";
                isScanning = false;
                unexpectedCompletion = true;
                break;
            case DiscoveryListener.INQUIRY_ERROR:
                completionType = "INQUIRY_ERROR";
                isScanning = false;
                if (eventListener != null) {
                    eventListener.onScanFailed(discType);
                }
                // No need to call onScanStopped here, onScanFailed implies stop
                break;
            case DiscoveryListener.INQUIRY_TERMINATED:
                completionType = "INQUIRY_TERMINATED (Stopped by user)";
                isScanning = false;
                stoppedByUser = true;
                break;
            default:
                completionType = "Unknown Completion Type: " + discType;
                isScanning = false;
                unexpectedCompletion = true;
                break;
        }
        
        LOGGER.info("Discovery process ended: {}", completionType);
        
        // Notify UI that scan has stopped if it was terminated or completed unexpectedly
        if ((stoppedByUser || unexpectedCompletion) && eventListener != null) {
            eventListener.onScanStopped();
        }
    }
}