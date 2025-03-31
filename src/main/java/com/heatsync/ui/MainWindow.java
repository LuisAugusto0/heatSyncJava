package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.PowerMonitor;
import com.heatsync.service.TemperatureMonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * The main application window for HeatSync.
 * Contains the primary layout and manages sub-panels.
 */
public class MainWindow {
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
    
    private JFrame mainFrame;
    private JTextArea logTextArea;
    
    // Sub-panels
    private TemperaturePanel temperaturePanel;
    private BluetoothPanel bluetoothPanel;
    
    // Services
    private final TemperatureMonitor temperatureMonitor;
    private final PowerMonitor powerMonitor;
    private final BluetoothService bluetoothService;
    
    /**
     * Creates the main window with all required panels.
     * 
     * @param temperatureMonitor The temperature monitoring service
     * @param powerMonitor The power monitoring service
     * @param bluetoothService The Bluetooth service
     */
    public MainWindow(TemperatureMonitor temperatureMonitor, PowerMonitor powerMonitor, BluetoothService bluetoothService) {
        this.temperatureMonitor = temperatureMonitor;
        this.powerMonitor = powerMonitor;
        this.bluetoothService = bluetoothService;
        
        initializeUI();
    }
    
    /**
     * Initialize the main UI components and layout.
     */
    private void initializeUI() {
        mainFrame = new JFrame("HeatSync");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(700, 500);
        mainFrame.setLayout(new BorderLayout(10, 10));
        
        // Create log panel first so logTextArea is initialized
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        
        logTextArea = new JTextArea(8, 50);
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Create sub-panels
        temperaturePanel = new TemperaturePanel();
        bluetoothPanel = new BluetoothPanel(bluetoothService, this::logMessage);
        
        // Combine panels in the main layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(temperaturePanel, BorderLayout.WEST);
        topPanel.add(bluetoothPanel.getDevicesPanel(), BorderLayout.CENTER);
        topPanel.add(bluetoothPanel.getControlPanel(), BorderLayout.EAST);
        
        mainFrame.add(topPanel, BorderLayout.CENTER);
        mainFrame.add(logPanel, BorderLayout.SOUTH);
        
        // Window closing event handler
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logMessage("Encerrando aplicação...");
                
                if (bluetoothService != null) {
                    bluetoothService.shutdown();
                }
            }
        });
    }
    
    /**
     * Displays the main application window.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            logMessage("HeatSync iniciado (Monitor de Temperatura Ativo)");
            logMessage("Verifique o status do Bluetooth e inicie a varredura para encontrar dispositivos.");
            mainFrame.setVisible(true);
        });
    }
    
    /**
     * Logs a message to the application log area.
     * 
     * @param message The message to log
     */
    public void logMessage(String message) {
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
    
    /**
     * Gets the temperature panel.
     * 
     * @return The temperature panel
     */
    public TemperaturePanel getTemperaturePanel() {
        return temperaturePanel;
    }
    
    /**
     * Gets the Bluetooth panel.
     * 
     * @return The Bluetooth panel
     */
    public BluetoothPanel getBluetoothPanel() {
        return bluetoothPanel;
    }
} 