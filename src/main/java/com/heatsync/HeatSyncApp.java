package com.heatsync;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.BluetoothService.BluetoothEventListener;
import com.heatsync.service.TemperatureMonitor;
import com.welie.blessed.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HeatSyncApp implements BluetoothEventListener {
    private static final Logger LOGGER = Logger.getLogger(HeatSyncApp.class.getName());
    
    private JFrame mainFrame;
    private JLabel cpuTempLabel;
    private JLabel gpuTempLabel;
    private JLabel diskTempLabel;
    private JLabel connectionStatusLabel;
    private JTextArea logTextArea;
    private JButton connectButton;
    private JToggleButton autoManualToggle;
    private JSlider fanSpeedSlider;
    private JButton scanButton;
    
    // Componentes para a lista de dispositivos descobertos
    private JList<String> deviceList;
    private DefaultListModel<String> deviceListModel;
    private JButton disconnectButton;
    
    // Mapa para associar strings exibidas na lista aos objetos BluetoothPeripheral
    private Map<String, BluetoothPeripheral> discoveredDevicesMap = new HashMap<>();
    
    private TemperatureMonitor temperatureMonitor;
    private BluetoothService bluetoothService;
    private Timer updateTimer;
    private boolean autoMode = true;
    private boolean scanning = false; // Flag para controlar o estado da varredura

    public HeatSyncApp() {
        temperatureMonitor = new TemperatureMonitor();
        bluetoothService = new BluetoothService();
        initializeGUI();
        startTemperatureUpdates();
        
        // Registrar este objeto como o listener de eventos Bluetooth
        registerBluetoothCallbacks();
    }
    
    /**
     * Registra callbacks para eventos Bluetooth
     */
    private void registerBluetoothCallbacks() {
        // Registra este objeto como listener de eventos Bluetooth
        bluetoothService.setBluetoothEventListener(this);
        
        // Verificar se o Bluetooth foi inicializado corretamente
        if (bluetoothService.isInitialized()) {
            logMessage("Bluetooth inicializado com sucesso!");
            scanButton.setEnabled(true);
        } else {
            logMessage("AVISO: BluetoothService não foi inicializado corretamente. Verifique se o hardware Bluetooth está disponível e se o BlueZ está instalado.");
            scanButton.setEnabled(false);
        }
    }

    // Implementação dos métodos da interface BluetoothEventListener
    @Override
    public void onDeviceDiscovered(BluetoothPeripheral peripheral, String name, String address, int rssi) {
        String deviceInfo = name + " (" + address + ") RSSI: " + rssi + " dBm";
        
        // Verificar se o dispositivo já existe na lista visual
        boolean deviceExists = false;
        String deviceKey = address; // Usar o endereço como chave única
        
        for (int i = 0; i < deviceListModel.size(); i++) {
            String item = deviceListModel.getElementAt(i);
            // Se o item contém o endereço, atualize-o
            if (item.contains(address)) {
                deviceListModel.setElementAt(deviceInfo, i);
                deviceExists = true;
                // Atualizar o mapa também
                discoveredDevicesMap.put(deviceInfo, peripheral);
                break;
            }
        }
        
        // Se não existir, adicione-o
        if (!deviceExists) {
            deviceListModel.addElement(deviceInfo);
            discoveredDevicesMap.put(deviceInfo, peripheral);
            
            // Habilitar botão conectar se houver pelo menos um dispositivo
            if (!connectButton.isEnabled() && deviceListModel.size() > 0) {
                connectButton.setEnabled(true);
            }
        }
        
        logMessage("Dispositivo encontrado: " + deviceInfo);
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
            logMessage("Conectado ao dispositivo: " + name + " (" + peripheral.getAddress() + ")");
            
            // Atualizar interface para refletir estado conectado
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
            logMessage("Desconectado do dispositivo: " + peripheral.getName() + " (" + peripheral.getAddress() + ") Status: " + status);
            
            // Atualizar interface para refletir estado desconectado
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
            logMessage("Falha na varredura BLE. Código de erro: " + errorCode);
        });
    }

    private void initializeGUI() {
        mainFrame = new JFrame("HeatSync");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(700, 500); // Aumentei o tamanho para acomodar a lista de dispositivos
        mainFrame.setLayout(new BorderLayout(10, 10));

        // Painel de temperaturas
        JPanel tempPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        tempPanel.setBorder(BorderFactory.createTitledBorder("Temperaturas"));
        
        cpuTempLabel = new JLabel("CPU Temperature: --°C");
        gpuTempLabel = new JLabel("GPU Temperature: --°C");
        diskTempLabel = new JLabel("Disk Temperature: --°C");
        
        tempPanel.add(cpuTempLabel);
        tempPanel.add(gpuTempLabel);
        tempPanel.add(diskTempLabel);
        
        // Painel de dispositivos descobertos (NOVO)
        JPanel devicesPanel = new JPanel(new BorderLayout(5, 5));
        devicesPanel.setBorder(BorderFactory.createTitledBorder("Dispositivos Descobertos"));
        
        // Criar a lista de dispositivos com scroll
        deviceListModel = new DefaultListModel<>();
        deviceList = new JList<>(deviceListModel);
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.addListSelectionListener(e -> {
            // Habilitar botão de conexão apenas quando um item é selecionado
            connectButton.setEnabled(!deviceList.isSelectionEmpty());
        });
        
        JScrollPane deviceScrollPane = new JScrollPane(deviceList);
        devicesPanel.add(deviceScrollPane, BorderLayout.CENTER);
        
        // Painel de controle do Bluetooth e Fan
        JPanel controlPanel = new JPanel(new GridLayout(7, 1, 5, 5)); // Aumentei para 7 para acomodar o slider RSSI
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));
        
        // Status da conexão
        connectionStatusLabel = new JLabel("Status: Desconectado");
        connectionStatusLabel.setForeground(Color.RED);
        
        // Adicionar slider para filtro de RSSI
        JPanel rssiPanel = new JPanel(new BorderLayout());
        JLabel rssiLabel = new JLabel("Filtro RSSI: -75 dBm");
        JSlider rssiSlider = new JSlider(JSlider.HORIZONTAL, -100, -30, -75);
        rssiSlider.setMajorTickSpacing(10);
        rssiSlider.setPaintTicks(true);
        rssiSlider.addChangeListener(e -> {
            int value = rssiSlider.getValue();
            rssiLabel.setText("Filtro RSSI: " + value + " dBm");
            if (!rssiSlider.getValueIsAdjusting()) {
                // Aplicar o filtro apenas quando o usuário parar de arrastar o slider
                if (bluetoothService != null && bluetoothService.isInitialized()) {
                    bluetoothService.setMinimumRssi(value);
                    logMessage("Filtro RSSI ajustado para " + value + " dBm");
                }
            }
        });
        rssiPanel.add(rssiLabel, BorderLayout.NORTH);
        rssiPanel.add(rssiSlider, BorderLayout.CENTER);
        
        // Botão para iniciar/parar varredura
        scanButton = new JButton("Escanear Dispositivos");
        scanButton.setEnabled(bluetoothService.isInitialized()); // Habilita apenas se o BT estiver inicializado
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scanning) {
                    // Parar a varredura
                    bluetoothService.stopDeviceDiscovery();
                    scanning = false;
                    scanButton.setText("Escanear Dispositivos");
                    logMessage("Varredura BLE interrompida.");
                } else {
                    // Limpar lista de dispositivos anteriores
                    deviceListModel.clear();
                    discoveredDevicesMap.clear();
                    connectButton.setEnabled(false);
                    
                    // Iniciar a varredura
                    if (bluetoothService.startDeviceDiscovery()) {
                        scanning = true;
                        scanButton.setText("Parar Varredura");
                        logMessage("Iniciando varredura por dispositivos BLE...");
                        logMessage("Dispositivos encontrados aparecerão na lista à direita.");
                    } else {
                        logMessage("ERRO: Não foi possível iniciar a varredura BLE.");
                    }
                }
            }
        });
        
        // Botão de conexão
        connectButton = new JButton("Conectar ao Dispositivo Selecionado");
        connectButton.setEnabled(false); // Inicialmente desabilitado até encontrar dispositivos
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = deviceList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedDevice = deviceListModel.getElementAt(selectedIndex);
                    BluetoothPeripheral peripheral = discoveredDevicesMap.get(selectedDevice);
                    
                    if (peripheral != null) {
                        logMessage("Tentando conectar a: " + selectedDevice);
                        bluetoothService.connectToDevice(peripheral.getAddress());
                    } else {
                        logMessage("ERRO: Não foi possível encontrar o dispositivo selecionado.");
                    }
                }
            }
        });
        
        // Botão de desconexão
        disconnectButton = new JButton("Desconectar");
        disconnectButton.setEnabled(false); // Inicialmente desabilitado até ter conexão
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logMessage("Desconectando do dispositivo...");
                bluetoothService.closeConnection();
            }
        });
        
        // Toggle para modo automático/manual
        autoManualToggle = new JToggleButton("Modo: Automático");
        autoManualToggle.setSelected(true); // Modo automático por padrão
        autoManualToggle.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                autoMode = (e.getStateChange() == ItemEvent.SELECTED);
                autoManualToggle.setText("Modo: " + (autoMode ? "Automático" : "Manual"));
                fanSpeedSlider.setEnabled(!autoMode); // Habilita o slider apenas no modo manual
                logMessage("Modo alterado para " + (autoMode ? "automático" : "manual"));
            }
        });
        
        // Slider para controle manual de velocidade do fan
        fanSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        fanSpeedSlider.setMajorTickSpacing(20);
        fanSpeedSlider.setMinorTickSpacing(5);
        fanSpeedSlider.setPaintTicks(true);
        fanSpeedSlider.setPaintLabels(true);
        fanSpeedSlider.setEnabled(false); // Inicialmente desabilitado (modo automático)
        fanSpeedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!fanSpeedSlider.getValueIsAdjusting() && !autoMode && bluetoothService.isConnected()) {
                    int value = fanSpeedSlider.getValue();
                    logMessage("Configurando velocidade do ventilador para " + value + "%");
                    bluetoothService.sendPwmCommand(value);
                }
            }
        });
        
        controlPanel.add(connectionStatusLabel);
        controlPanel.add(rssiPanel);  // Adicionando o novo painel do RSSI
        controlPanel.add(scanButton);
        controlPanel.add(connectButton);
        controlPanel.add(disconnectButton);
        controlPanel.add(autoManualToggle);
        controlPanel.add(fanSpeedSlider);
        
        // Painel de log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        
        logTextArea = new JTextArea(8, 50);
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Adicionar componentes à janela principal
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(tempPanel, BorderLayout.WEST);
        topPanel.add(devicesPanel, BorderLayout.CENTER);
        topPanel.add(controlPanel, BorderLayout.EAST);
        
        mainFrame.add(topPanel, BorderLayout.CENTER);
        mainFrame.add(logPanel, BorderLayout.SOUTH);
        
        // Configurar evento de fechamento de janela
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logMessage("Encerrando aplicação...");
                if (updateTimer != null) {
                    updateTimer.cancel();
                }
                if (bluetoothService != null) {
                    bluetoothService.shutdown();
                }
            }
        });
        
        mainFrame.setVisible(true);
    }

    private void startTemperatureUpdates() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTemperatures();
            }
        }, 0, 2000); // Update every 2 seconds
    }

    private void updateTemperatures() {
        temperatureMonitor.updateTemperatures();
        
        double cpuTemp = temperatureMonitor.getCpuTemperature();
        double gpuTemp = temperatureMonitor.getGpuTemperature();
        double diskTemp = temperatureMonitor.getDiskTemperature();
        
        SwingUtilities.invokeLater(() -> {
            cpuTempLabel.setText(String.format("CPU Temperature: %.2f°C", cpuTemp));
            gpuTempLabel.setText(String.format("GPU Temperature: %.2f°C", gpuTemp));
            diskTempLabel.setText(String.format("Disk Temperature: %.2f°C", diskTemp));
        });
    }
    
    private void logMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
             logTextArea.append(message + "\n");
             logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                 logTextArea.append(message + "\n");
                 logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            });
        }
        LOGGER.info(message);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            logMessage("HeatSync iniciado (Monitor de Temperatura Ativo)");
            logMessage("Verifique o status do Bluetooth e inicie a varredura para encontrar dispositivos.");
            mainFrame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.warning("Não foi possível definir o Look and Feel do sistema.");
        }

        SwingUtilities.invokeLater(() -> {
            HeatSyncApp app = new HeatSyncApp();
            app.show();
        });
    }
} 