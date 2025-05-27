package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.CsvLogger;
import com.heatsync.service.bluetooth.BluetoothEventListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Panel for managing Bluetooth device discovery, connection, and control.
 */
public class BluetoothPanel implements BluetoothEventListener {
    private static final Logger LOGGER = Logger.getLogger(BluetoothPanel.class.getName());
    
    // UI components
    private JPanel devicesPanel;
    private JPanel controlPanel;
    private JList<String> deviceList;
    private DefaultListModel<String> deviceListModel;
    private JLabel connectionStatusLabel;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton scanButton;
    private JToggleButton autoManualToggle;
    private JSlider fanSpeedSlider;
    private JCheckBox dumpCheckBox;
    private JCheckBox isInBenchmarkCheckBox;
    
    // Data
    private Map<String, String> deviceAddressMap = new HashMap<>(); // Maps display string to device address
    private boolean autoMode = true;
    private boolean scanning = false;
    
    // Services
    private final BluetoothService bluetoothService;
    private final Consumer<String> logCallback;
    private final CsvLogger csvLogger;

    // Panel for rpm reading
    private TemperaturePanel temperaturePanel;
    
    /**
     * Creates a new Bluetooth panel with discovery and control components.
     * 
     * @param bluetoothService The Bluetooth service
     * @param logCallback Callback for logging messages
     */
    public BluetoothPanel(BluetoothService bluetoothService, Consumer<String> logCallback, TemperaturePanel temperaturePanel, CsvLogger csvLogger) {
        this.bluetoothService = bluetoothService;
        this.logCallback = logCallback;
        this.temperaturePanel = temperaturePanel;
        this.csvLogger = csvLogger;

        initializeUI();
        registerBluetoothCallbacks();
    }
    
    /**
     * Initialize the UI components and layout.
     */
    private void initializeUI() {
        // Create devices panel
        createDevicesPanel();
        
        // Create control panel
        createControlPanel();
    }
    
    /**
     * Creates the devices panel with the discovered devices list.
     */
    private void createDevicesPanel() {
        devicesPanel = new JPanel(new BorderLayout(5, 5));
        devicesPanel.setBorder(BorderFactory.createTitledBorder("Discovered Devices"));
        
        // Create the device list with scrolling
        deviceListModel = new DefaultListModel<>();
        deviceList = new JList<>(deviceListModel);
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.addListSelectionListener(e -> {
            // Enable connect button only when an item is selected
            connectButton.setEnabled(!deviceList.isSelectionEmpty());
        });
        
        JScrollPane deviceScrollPane = new JScrollPane(deviceList);
        devicesPanel.add(deviceScrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Creates the control panel with Bluetooth and fan controls.
     */
    private void createControlPanel() {
        controlPanel = new JPanel(new GridLayout(7, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        // Connection status
        connectionStatusLabel = new JLabel("Status: Disconnected");
        connectionStatusLabel.setForeground(Color.RED);
        
        // RSSI filter slider
        JPanel rssiPanel = new JPanel(new BorderLayout());
        JLabel rssiLabel = new JLabel("RSSI Filter: -75 dBm");
        JSlider rssiSlider = new JSlider(JSlider.HORIZONTAL, -100, -30, -75);
        rssiSlider.setMajorTickSpacing(10);
        rssiSlider.setPaintTicks(true);
        rssiSlider.addChangeListener(e -> {
            int value = rssiSlider.getValue();
            rssiLabel.setText("RSSI Filter: " + value + " dBm");
            if (!rssiSlider.getValueIsAdjusting()) {
                // Apply filter only when user stops dragging the slider
                if (bluetoothService != null && bluetoothService.isInitialized()) {
                    bluetoothService.setMinimumRssi(value);
                    logCallback.accept("RSSI filter adjusted to " + value + " dBm");
                }
            }
        });
        rssiPanel.add(rssiLabel, BorderLayout.NORTH);
        rssiPanel.add(rssiSlider, BorderLayout.CENTER);
        
        // Scan button
        scanning = bluetoothService.isScanning();
        scanButton = new JButton(scanning ? "Stop Scanning" : "Scan for Devices");
        scanButton.setEnabled(bluetoothService.isInitialized());
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scanning) {
                    // Stop scanning
                    bluetoothService.stopDeviceDiscovery();
                    // The isScanning flag will be set to false in inquiryCompleted
                    // which will be called asynchronously after stopDeviceDiscovery completes.
                    scanButton.setText("Stopping Scan..."); // Indicate intermediate state
                    scanButton.setEnabled(false); // Prevent rapid clicking
                } else {
                    // Clear previous devices before starting a new scan
                    deviceListModel.clear();
                    deviceAddressMap.clear();
                    connectButton.setEnabled(false); // Disable connect until devices are found
                    
                    // Start scanning
                    if (bluetoothService.startDeviceDiscovery()) {
                        scanning = true;
                        scanButton.setText("Stop Scanning");
                        scanButton.setEnabled(true); // Ensure enabled
                        logCallback.accept("Starting continuous Bluetooth scan...");
                        logCallback.accept("Click 'Stop Scanning' to halt discovery.");
                    } else {
                        logCallback.accept("ERROR: Unable to start Bluetooth scan.");
                        scanButton.setText("Scan for Devices"); // Reset button text on failure
                        scanButton.setEnabled(bluetoothService.isInitialized()); // Re-enable if initialized
                        scanning = false;
                    }
                }
            }
        });
        
        // Connect button
        connectButton = new JButton("Connect to Selected Device");
        connectButton.setEnabled(false);
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = deviceList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedDeviceDisplay = deviceListModel.getElementAt(selectedIndex);
                    String deviceAddress = deviceAddressMap.get(selectedDeviceDisplay);
                    
                    if (deviceAddress != null) {
                        logCallback.accept("Attempting to connect to: " + selectedDeviceDisplay);
                        bluetoothService.connectToDevice(deviceAddress);
                    } else {
                        logCallback.accept("ERROR: Could not find the address for the selected device.");
                    }
                }
            }
        });
        
        // Disconnect button
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logCallback.accept("Disconnecting from device...");
                bluetoothService.closeConnection();
            }
        });
        
        dumpCheckBox = new JCheckBox("Dump RPM");
        dumpCheckBox.setSelected(false); // Default state is unchecked
        dumpCheckBox.setEnabled(false); // Enable only if connected
        dumpCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(dumpCheckBox.isSelected()) {
                    isInBenchmarkCheckBox.setEnabled(true); // Enable benchmark checkbox when dumping is enabled
                    csvLogger.start(); // Enable RPM dumping in CsvLogger
                } else {
                    isInBenchmarkCheckBox.setEnabled(false); // Disable benchmark checkbox when dumping is disabled
                    csvLogger.stop(); // Disable RPM dumping in CsvLogger
                }
                LOGGER.info("Dump RPM checkbox selected: " + dumpCheckBox.isSelected());
            }
        });
        isInBenchmarkCheckBox = new JCheckBox("Toggle Benchmark state");
        isInBenchmarkCheckBox.setSelected(false); // Default state is unchecked
        isInBenchmarkCheckBox.setEnabled(false); // Enable only if connected
        isInBenchmarkCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            if (isInBenchmarkCheckBox.isSelected()) {
                dumpCheckBox.setEnabled(false); // Enable dump checkbox
                LOGGER.info("Benchmark enabled");
                // Additional logic for benchmark can be added here
            } else {
                dumpCheckBox.setEnabled(true); // Disable dump checkbox
                LOGGER.info("Benchmark disabled");
                // Additional logic for disabling benchmark can be added here
            }
            }
        });
        // // Auto/Manual mode toggle
        // autoManualToggle = new JToggleButton("Mode: Automatic");
        // autoManualToggle.setSelected(true);
        // autoManualToggle.addItemListener(new ItemListener() {
        //     @Override
        //     public void itemStateChanged(ItemEvent e) {
        //         autoMode = (e.getStateChange() == ItemEvent.SELECTED);
        //         autoManualToggle.setText("Mode: " + (autoMode ? "Automatic" : "Manual"));
        //         fanSpeedSlider.setEnabled(!autoMode);
        //         logCallback.accept("Mode changed to " + (autoMode ? "automatic" : "manual"));
        //     }
        // });
        
        // // Fan speed slider
        // fanSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        // fanSpeedSlider.setMajorTickSpacing(20);
        // fanSpeedSlider.setMinorTickSpacing(5);
        // fanSpeedSlider.setPaintTicks(true);
        // fanSpeedSlider.setPaintLabels(true);
        // fanSpeedSlider.setEnabled(false);
        // fanSpeedSlider.addChangeListener(new ChangeListener() {
        //     @Override
        //     public void stateChanged(ChangeEvent e) {
        //         if (!fanSpeedSlider.getValueIsAdjusting() && !autoMode && bluetoothService.isConnected()) {
        //             int value = fanSpeedSlider.getValue();
        //             logCallback.accept("Setting fan speed to " + value + "%");
        //             bluetoothService.sendPwmCommand(value);
        //         }
        //     }
        // });
        
        // Add components to control panel
        controlPanel.add(connectionStatusLabel);
        controlPanel.add(rssiPanel);
        controlPanel.add(scanButton);
        controlPanel.add(connectButton);
        controlPanel.add(disconnectButton);
        controlPanel.add(dumpCheckBox);
        controlPanel.add(isInBenchmarkCheckBox);
        // controlPanel.add(autoManualToggle);
        // controlPanel.add(fanSpeedSlider);
    }
    
    /**
     * Registers this panel as a listener for Bluetooth events.
     */
    private void registerBluetoothCallbacks() {
        // Alterado de setBluetoothEventListener para setEventListener
        bluetoothService.setEventListener(this);
        
        if (bluetoothService.isInitialized()) {
            logCallback.accept("Bluetooth initialized successfully!");
            scanButton.setEnabled(true);
        } else {
            logCallback.accept("WARNING: BluetoothService was not initialized correctly. Check if your Bluetooth hardware is available.");
            scanButton.setEnabled(false);
        }
    }

    // BluetoothEventListener implementation
    @Override
    public void onDeviceDiscovered(Object deviceObj, String name, String address, int rssi) {
        String deviceInfo = name + " (" + address + ") RSSI: " + rssi + " dBm";
        
        // Check if device already exists in the visual list
        boolean deviceExists = false;
        for (int i = 0; i < deviceListModel.size(); i++) {
            String item = deviceListModel.getElementAt(i);
            if (item.contains(address)) {
                // If item contains the address, update it
                deviceListModel.setElementAt(deviceInfo, i);
                deviceExists = true;
                
                // Update the map too
                deviceAddressMap.put(deviceInfo, address);
                break;
            }
        }
        
        // If it doesn't exist, add it
        if (!deviceExists) {
            deviceListModel.addElement(deviceInfo);
            deviceAddressMap.put(deviceInfo, address);
        }
        
        // Enable connect button if there's at least one device
        if (deviceListModel.size() > 0 && !connectButton.isEnabled()) {
            connectButton.setEnabled(true);
        }
    }

    @Override
    public void onDeviceConnected(Object deviceObj) {
        SwingUtilities.invokeLater(() -> {
            // Try to extract name and address from the device object,
            // but in most cases we'll only log a success message
            logCallback.accept("Connected to device successfully");
            
            // Update interface to reflect connected state
            connectionStatusLabel.setText("Status: Connected");
            connectionStatusLabel.setForeground(Color.GREEN);
            disconnectButton.setEnabled(true);
            dumpCheckBox.setEnabled(true); 
            scanButton.setEnabled(false);
            connectButton.setEnabled(false);
        });
    }

    @Override
    public void onDeviceDisconnected(Object deviceObj, int status) {
        SwingUtilities.invokeLater(() -> {
            logCallback.accept("Disconnected from device");
            
            // Update interface to reflect disconnected state
            connectionStatusLabel.setText("Status: Disconnected");
            connectionStatusLabel.setForeground(Color.RED);
            disconnectButton.setEnabled(false);
            if(csvLogger.isFileOpen()) {
                csvLogger.stop(); // Stop logging if a test was running
            }
            dumpCheckBox.setSelected(false);
            dumpCheckBox.setEnabled(false); 
            isInBenchmarkCheckBox.setSelected(false);
            isInBenchmarkCheckBox.setEnabled(false);

            // Re-enable scan button if not already scanning
            if (!scanning) {
                scanButton.setText("Scan for Devices");
                scanButton.setEnabled(bluetoothService.isInitialized());
            }
        });
    }

    @Override
    public void onScanFailed(int errorCode) {
        SwingUtilities.invokeLater(() -> {
            logCallback.accept("ERROR: Bluetooth scan failed. Code: " + errorCode);
            scanning = false;
            scanButton.setText("Scan for Devices");
            scanButton.setEnabled(bluetoothService.isInitialized());
        });
    }

    /**
     * Gets the devices panel.
     * 
     * @return The devices panel
     */
    public JPanel getDevicesPanel() {
        return devicesPanel;
    }

    /**
     * Gets the control panel.
     * 
     * @return The control panel
     */
    public JPanel getControlPanel() {
        return controlPanel;
    }

    /**
     * Gets the auto mode status.
     * 
     * @return True if in auto mode, false if in manual mode
     */
    public boolean isAutoMode() {
        return autoMode;
    }

    /**
     * Gets the fan speed value from slider (0-100).
     * 
     * @return The fan speed value
     */
    public int getFanSpeed() {
        return fanSpeedSlider.getValue();
    }

    @Override
    public void onFanRpmReceived(int rpm) {
        SwingUtilities.invokeLater(() -> {
            // Aqui você pode atualizar um label ou outro componente da UI com o valor do RPM
            LOGGER.info("Fan RPM received: " + rpm);
            temperaturePanel.updateFanRpm(rpm); // Atualiza o painel de temperatura com o RPM recebido

            // Log the RPM to the console or a log area
            if (dumpCheckBox.isSelected()) {
                csvLogger.logRpmData(rpm, isInBenchmarkCheckBox.isSelected());
            }
        });
    }

    // Add a listener method for scan completion/termination
    public void onScanStopped() {
        SwingUtilities.invokeLater(() -> {
            scanning = false;
            scanButton.setText("Scan for Devices");
            scanButton.setEnabled(bluetoothService.isInitialized());
            logCallback.accept("Bluetooth scan stopped.");
        });
    }

    
}