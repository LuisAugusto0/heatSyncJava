package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.bluetooth.BluetoothEventListener;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    
    // Data
    private Map<String, BluetoothPeripheral> discoveredDevicesMap = new HashMap<>();
    private boolean autoMode = true;
    private boolean scanning = false;
    
    // Services
    private final BluetoothService bluetoothService;
    private final Consumer<String> logCallback;
    
    /**
     * Creates a new Bluetooth panel with discovery and control components.
     * 
     * @param bluetoothService The Bluetooth service
     * @param logCallback Callback for logging messages
     */
    public BluetoothPanel(BluetoothService bluetoothService, Consumer<String> logCallback) {
        this.bluetoothService = bluetoothService;
        this.logCallback = logCallback;
        
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
        scanButton = new JButton("Scan for Devices");
        scanButton.setEnabled(bluetoothService.isInitialized());
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scanning) {
                    // Stop scanning
                    bluetoothService.stopDeviceDiscovery();
                    scanning = false;
                    scanButton.setText("Scan for Devices");
                    logCallback.accept("BLE scan stopped.");
                } else {
                    // Clear previous devices
                    deviceListModel.clear();
                    discoveredDevicesMap.clear();
                    connectButton.setEnabled(false);
                    
                    // Start scanning
                    if (bluetoothService.startDeviceDiscovery()) {
                        scanning = true;
                        scanButton.setText("Stop Scanning");
                        logCallback.accept("Starting scan for BLE devices...");
                        logCallback.accept("Found devices will appear in the list on the right.");
                    } else {
                        logCallback.accept("ERROR: Unable to start BLE scan.");
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
                    String selectedDevice = deviceListModel.getElementAt(selectedIndex);
                    BluetoothPeripheral peripheral = discoveredDevicesMap.get(selectedDevice);
                    
                    if (peripheral != null) {
                        logCallback.accept("Attempting to connect to: " + selectedDevice);
                        bluetoothService.connectToDevice(peripheral.getAddress());
                    } else {
                        logCallback.accept("ERROR: Could not find the selected device.");
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
        
        // Auto/Manual mode toggle
        autoManualToggle = new JToggleButton("Mode: Automatic");
        autoManualToggle.setSelected(true);
        autoManualToggle.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                autoMode = (e.getStateChange() == ItemEvent.SELECTED);
                autoManualToggle.setText("Mode: " + (autoMode ? "Automatic" : "Manual"));
                fanSpeedSlider.setEnabled(!autoMode);
                logCallback.accept("Mode changed to " + (autoMode ? "automatic" : "manual"));
            }
        });
        
        // Fan speed slider
        fanSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        fanSpeedSlider.setMajorTickSpacing(20);
        fanSpeedSlider.setMinorTickSpacing(5);
        fanSpeedSlider.setPaintTicks(true);
        fanSpeedSlider.setPaintLabels(true);
        fanSpeedSlider.setEnabled(false);
        fanSpeedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!fanSpeedSlider.getValueIsAdjusting() && !autoMode && bluetoothService.isConnected()) {
                    int value = fanSpeedSlider.getValue();
                    logCallback.accept("Setting fan speed to " + value + "%");
                    bluetoothService.sendPwmCommand(value);
                }
            }
        });
        
        // Add components to control panel
        controlPanel.add(connectionStatusLabel);
        controlPanel.add(rssiPanel);
        controlPanel.add(scanButton);
        controlPanel.add(connectButton);
        controlPanel.add(disconnectButton);
        controlPanel.add(autoManualToggle);
        controlPanel.add(fanSpeedSlider);
    }
    
    /**
     * Registers this panel as a listener for Bluetooth events.
     */
    private void registerBluetoothCallbacks() {
        bluetoothService.setBluetoothEventListener(this);
        
        if (bluetoothService.isInitialized()) {
            logCallback.accept("Bluetooth initialized successfully!");
            scanButton.setEnabled(true);
        } else {
            logCallback.accept("WARNING: BluetoothService was not initialized correctly. Check if your Bluetooth hardware is available and BlueZ is installed.");
            scanButton.setEnabled(false);
        }
    }

    // BluetoothEventListener implementation
    @Override
    public void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi) {
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
                discoveredDevicesMap.put(deviceInfo, peripheral);
                break;
            }
        }
        
        // If it doesn't exist, add it
        if (!deviceExists) {
            deviceListModel.addElement(deviceInfo);
            discoveredDevicesMap.put(deviceInfo, peripheral);
        }
        
        // Enable connect button if there's at least one device
        if (deviceListModel.size() > 0 && !connectButton.isEnabled()) {
            connectButton.setEnabled(true);
        }
    }

    @Override
    public void onDeviceConnected(BluetoothPeripheral peripheral) {
        SwingUtilities.invokeLater(() -> {
            logCallback.accept("Connected to " + peripheral.getName() + " (" + peripheral.getAddress() + ")");
            
            // Update interface to reflect connected state
            connectionStatusLabel.setText("Status: Connected");
            connectionStatusLabel.setForeground(Color.GREEN);
            disconnectButton.setEnabled(true);
            scanButton.setEnabled(false);
            connectButton.setEnabled(false);
        });
    }

    @Override
    public void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        SwingUtilities.invokeLater(() -> {
            logCallback.accept("Disconnected from " + peripheral.getName() + " (" + peripheral.getAddress() + ")");
            
            // Update interface to reflect disconnected state
            connectionStatusLabel.setText("Status: Disconnected");
            connectionStatusLabel.setForeground(Color.RED);
            disconnectButton.setEnabled(false);
            scanButton.setEnabled(true);
            connectButton.setEnabled(!deviceList.isSelectionEmpty());
        });
    }

    @Override
    public void onScanFailed(int errorCode) {
        SwingUtilities.invokeLater(() -> {
            logCallback.accept("ERROR: Scan failed with error code " + errorCode);
            scanning = false;
            scanButton.setText("Scan for Devices");
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
} 