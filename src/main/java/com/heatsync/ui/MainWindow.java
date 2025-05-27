package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.CsvLogger;
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

    //Log painel and text area
    private JTextArea logTextArea;
    private JScrollPane logScrollPane;
    
    // Sub-panels
    private TemperaturePanel temperaturePanel;
    private BluetoothPanel bluetoothPanel;
    private ProfilePanel profilePanel;
    private JPanel logPanel;

    // private ProfilePanel profilePanel;
    
    // Services
    private final TemperatureMonitor temperatureMonitor;
    private final BluetoothService bluetoothService;
    private final CsvLogger csvLogger = new CsvLogger(this::logMessage);

    
    /**
     * Creates the main window with all required panels.
     * 
     * @param temperatureMonitor The temperature monitoring service
     * @param powerMonitor The power monitoring service
     * @param bluetoothService The Bluetooth service
     */
    public MainWindow(TemperatureMonitor temperatureMonitor, BluetoothService bluetoothService) {
        this.temperatureMonitor = temperatureMonitor;
        this.bluetoothService = bluetoothService;
        
        
        initializeUIElements();
        applyLayout(0);
    }
    
    /**
     * Initialize the main UI components and layout.
     */
    private void initializeUIElements() {
        mainFrame = new JFrame("HeatSync");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setMinimumSize(new Dimension(700, 550));
        mainFrame.setLayout(new BorderLayout(10, 10));
        
        // Create log panel first so logTextArea is initialized
        logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        
        logTextArea = new JTextArea(8, 50);
        logTextArea.setEditable(false);
        logScrollPane = new JScrollPane(logTextArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Create sub-panels
        temperaturePanel = new TemperaturePanel(this, 0, csvLogger);
        bluetoothPanel = new BluetoothPanel(bluetoothService, this::logMessage, temperaturePanel, csvLogger);
        profilePanel = new ProfilePanel(this);

        // Add JVM shutdown hook for system power off
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("System shutdown detected, turning off fan...");
            bluetoothService.sendConstantProfile(0);
            if (bluetoothService != null) {
                bluetoothService.shutdown();
            }
        }));
        
        // Keep the existing window listener
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logMessage("Shutting down application...");
                bluetoothService.sendConstantProfile(0);
                if (bluetoothService != null) {
                    bluetoothService.shutdown();
                }
            }
        });
    }
    
    /**
     * Applies the specified layout to the main frame.
     * 
     * @param layout Layout identifier (0 for main layout, 1 for profile editor)
     */
    public void applyLayout(int layoutMode) {
        // Clear the existing content first
        mainFrame.getContentPane().removeAll();
        
        if (layoutMode == 0) {
            mainLayout(layoutMode);
        } else if (layoutMode == 1) {
            profileEditorLayout(layoutMode);
        }
        
        // These steps are crucial to refresh the UI
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    /**
     * Applies the main application layout.
     */
    public void mainLayout(int layoutMode) {
        temperaturePanel.updateUIForMode(layoutMode);
        // Update title for main layout
        mainFrame.setTitle("HeatSync");
        
        // Create top panel with components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(temperaturePanel, BorderLayout.WEST);
        topPanel.add(bluetoothPanel.getDevicesPanel(), BorderLayout.CENTER);
        topPanel.add(bluetoothPanel.getControlPanel(), BorderLayout.EAST);
        
        // Add panels to the main frame
        mainFrame.add(topPanel, BorderLayout.CENTER);
        mainFrame.add(logPanel, BorderLayout.SOUTH);
        LOGGER.info("Main layout applied.");
    }

    /**
     * Applies the profile editor layout.
     */
    public void profileEditorLayout(int layoutMode) {
        temperaturePanel.updateUIForMode(layoutMode);
        // Update title for profile editor layout
        mainFrame.setTitle("HeatSync - Fan Profile Editor");
        
        // Create a panel for the bottom buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton backButton = new JButton("Back to Main");
        backButton.addActionListener(e -> applyLayout(0));
        bottomPanel.add(logPanel, BorderLayout.NORTH);
        buttonPanel.add(backButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add components to the main frame
        mainFrame.add(temperaturePanel, BorderLayout.NORTH);
        mainFrame.add(profilePanel, BorderLayout.CENTER);
        mainFrame.add(bottomPanel, BorderLayout.SOUTH);
        LOGGER.info("Profile editor layout applied.");
    }

    
    /**
     * Displays the main application window.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
            logMessage("HeatSync started (Temperature Monitor Active)");
            logMessage("Check Bluetooth status and start scanning to find devices.");
            mainFrame.setVisible(true);
        });
    }
    
    /**
     * hides the application window.
     */
    public void hide() {
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(false);
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

    /**
     * Gets the Bluetooth service.
     * 
     * @return The Bluetooth service
     */
    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }
}