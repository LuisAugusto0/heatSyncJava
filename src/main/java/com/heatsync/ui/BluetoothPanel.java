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
        devicesPanel.setBorder(BorderFactory.createTitledBorder("Dispositivos Descobertos"));
        
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
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));
        
        // Connection status
        connectionStatusLabel = new JLabel("Status: Desconectado");
        connectionStatusLabel.setForeground(Color.RED);
        
        // RSSI filter slider
        JPanel rssiPanel = new JPanel(new BorderLayout());
        JLabel rssiLabel = new JLabel("Filtro RSSI: -75 dBm");
        JSlider rssiSlider = new JSlider(JSlider.HORIZONTAL, -100, -30, -75);
        rssiSlider.setMajorTickSpacing(10);
        rssiSlider.setPaintTicks(true);
        rssiSlider.addChangeListener(e -> {
            int value = rssiSlider.getValue();
            rssiLabel.setText("Filtro RSSI: " + value + " dBm");
            if (!rssiSlider.getValueIsAdjusting()) {
                // Apply filter only when user stops dragging the slider
                if (bluetoothService != null && bluetoothService.isInitialized()) {
                    bluetoothService.setMinimumRssi(value);
                    logCallback.accept("Filtro RSSI ajustado para " + value + " dBm");
                }
            }
        });
        rssiPanel.add(rssiLabel, BorderLayout.NORTH);
        rssiPanel.add(rssiSlider, BorderLayout.CENTER);
        
        // Scan button
        scanButton = new JButton("Escanear Dispositivos");
        scanButton.setEnabled(bluetoothService.isInitialized());
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scanning) {
                    // Stop scanning
                    bluetoothService.stopDeviceDiscovery();
                    scanning = false;
                    scanButton.setText("Escanear Dispositivos");
                    logCallback.accept("Varredura BLE interrompida.");
                } else {
                    // Clear previous devices
                    deviceListModel.clear();
                    discoveredDevicesMap.clear();
                    connectButton.setEnabled(false);
                    
                    // Start scanning
                    if (bluetoothService.startDeviceDiscovery()) {
                        scanning = true;
                        scanButton.setText("Parar Varredura");
                        logCallback.accept("Iniciando varredura por dispositivos BLE...");
                        logCallback.accept("Dispositivos encontrados aparecerão na lista à direita.");
                    } else {
                        logCallback.accept("ERRO: Não foi possível iniciar a varredura BLE.");
                    }
                }
            }
        });
        
        // Connect button
        connectButton = new JButton("Conectar ao Dispositivo Selecionado");
        connectButton.setEnabled(false);
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = deviceList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedDevice = deviceListModel.getElementAt(selectedIndex);
                    BluetoothPeripheral peripheral = discoveredDevicesMap.get(selectedDevice);
                    
                    if (peripheral != null) {
                        logCallback.accept("Tentando conectar a: " + selectedDevice);
                        bluetoothService.connectToDevice(peripheral.getAddress());
                    } else {
                        logCallback.accept("ERRO: Não foi possível encontrar o dispositivo selecionado.");
                    }
                }
            }
        });
        
        // Disconnect button
        disconnectButton = new JButton("Desconectar");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logCallback.accept("Desconectando do dispositivo...");
                bluetoothService.closeConnection();
            }
        });
        
        // Auto/Manual mode toggle
        autoManualToggle = new JToggleButton("Modo: Automático");
        autoManualToggle.setSelected(true);
        autoManualToggle.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                autoMode = (e.getStateChange() == ItemEvent.SELECTED);
                autoManualToggle.setText("Modo: " + (autoMode ? "Automático" : "Manual"));
                fanSpeedSlider.setEnabled(!autoMode);
                logCallback.accept("Modo alterado para " + (autoMode ? "automático" : "manual"));
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
                    logCallback.accept("Configurando velocidade do ventilador para " + value + "%");
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
            logCallback.accept("Bluetooth inicializado com sucesso!");
            scanButton.setEnabled(true);
        } else {
            logCallback.accept("AVISO: BluetoothService não foi inicializado corretamente. Verifique se o hardware Bluetooth está disponível e se o BlueZ está instalado.");
            scanButton.setEnabled(false);
        }
    }

    // BluetoothEventListener implementation
    @Override
    public void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        String deviceInfo = name + " (" + address + ") RSSI: " + rssi + " dBm";
        
        // Check if device already exists in the visual list
        boolean deviceExists = false;
        String deviceKey = address;
        
        for (int i = 0; i < deviceListModel.size(); i++) {
            String item = deviceListModel.getElementAt(i);
            // If item contains the address, update it
            if (item.contains(address)) {
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
            
            // Enable connect button if there's at least one device
            if (!connectButton.isEnabled() && deviceListModel.size() > 0) {
                connectButton.setEnabled(true);
            }
        }
        
        logCallback.accept("Dispositivo encontrado: " + deviceInfo);
    }

    @Override
    public void onDeviceConnected(BluetoothPeripheral peripheral) {
        SwingUtilities.invokeLater(() -> {
            String name = peripheral.getName();
            if (name == null || name.isEmpty()) {
                name = peripheral.getAddress();
            }
            
            connectionStatusLabel.setText("Status: Conectado a " + name);
            connectionStatusLabel.setForeground(Color.GREEN);
            logCallback.accept("Conectado ao dispositivo: " + name + " (" + peripheral.getAddress() + ")");
            
            // Update interface to reflect connected state
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            scanButton.setEnabled(false);
        });
    }

    @Override
    public void onDeviceDisconnected(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        SwingUtilities.invokeLater(() -> {
            connectionStatusLabel.setText("Status: Desconectado");
            connectionStatusLabel.setForeground(Color.RED);
            logCallback.accept("Desconectado do dispositivo: " + peripheral.getName() + " (" + peripheral.getAddress() + ") Status: " + status);
            
            // Update interface to reflect disconnected state
            connectButton.setEnabled(deviceListModel.size() > 0);
            disconnectButton.setEnabled(false);
            scanButton.setEnabled(true);
        });
    }

    @Override
    public void onScanFailed(int errorCode) {
        SwingUtilities.invokeLater(() -> {
            scanning = false;
            scanButton.setText("Escanear Dispositivos");
            logCallback.accept("Falha na varredura BLE. Código de erro: " + errorCode);
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
     * Gets whether the UI is in auto mode.
     * 
     * @return True if in auto mode, false if in manual mode
     */
    public boolean isAutoMode() {
        return autoMode;
    }
    
    /**
     * Gets the manual fan speed value.
     * 
     * @return The fan speed value from slider (0-100)
     */
    public int getFanSpeed() {
        return fanSpeedSlider.getValue();
    }
} 