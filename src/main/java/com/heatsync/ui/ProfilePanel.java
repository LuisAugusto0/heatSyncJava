package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Panel for editing fan profile configuration.
 */
public class ProfilePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ProfilePanel.class.getName());
    private Consumer<String> logCallback = message -> LOGGER.info(message);
    private MainWindow mainWindow;
    private BluetoothService bluetoothService;
    
    // Components for profile type selection
    private int currentProfileType; // 0 = Constant, 1 = Linear, 2 = Exponential
    
    // Components for Constant profile
    private JSpinner constantValueSpinner;
    
    // Components for Linear/Exponential profile
    private JSpinner cpuMinSpinner;
    private JSpinner cpuMaxSpinner;
    private JSpinner gpuMinSpinner;
    private JSpinner gpuMaxSpinner;
    private JSpinner minSpeedSpinner;
    private JSpinner maxSpeedSpinner;
    
    /**
     * Creates a new ProfilePanel.
     */
    public ProfilePanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.bluetoothService = mainWindow.getBluetoothService();
        currentProfileType = 0; // Default to Constant
        initializeUI();
    }
    
    /**
     * Initialize the UI components and layout.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Profile Editor"));
        
        // Top panel with profile type selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Profile Type:"));

        JRadioButton constantButton = new JRadioButton("Constant", true);
        JRadioButton linearButton = new JRadioButton("Linear");
        JRadioButton exponentialButton = new JRadioButton("Exponential");

        // Group the buttons for single selection
        ButtonGroup group = new ButtonGroup();
        group.add(constantButton);
        group.add(linearButton);
        group.add(exponentialButton);

        // Add ActionListener to update the current profile
        ActionListener profileListener = e -> {
            if (constantButton.isSelected()) {
                currentProfileType = 0;
            } else if (linearButton.isSelected()) {
                currentProfileType = 1;
            } else if (exponentialButton.isSelected()) {
                currentProfileType = 2;
            }
            updateUIForProfileType();
        };

        constantButton.addActionListener(profileListener);
        linearButton.addActionListener(profileListener);
        exponentialButton.addActionListener(profileListener);

        topPanel.add(constantButton);
        topPanel.add(linearButton);
        topPanel.add(exponentialButton);

        add(topPanel, BorderLayout.NORTH);
        
        // Center panel with inputs based on current profile type
        updateUIForProfileType();
        
        // Bottom panel with send button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton sendProfileButton = new JButton("Send Profile to Device");
        sendProfileButton.addActionListener(e -> {
            if (bluetoothService != null && bluetoothService.isConnected()) {
                if (currentProfileType == 0) { // Constant profile
                    int constantValue = (Integer) constantValueSpinner.getValue();
                    boolean sent = bluetoothService.sendConstantCommand(constantValue);
                    if (sent) {
                        logMessage("Constant profile sent: " + constantValue);
                    } else {
                        logMessage("Failed to send constant profile");
                    }
                } else { // Linear or Exponential profile
                    int cpuMin = (Integer) cpuMinSpinner.getValue();
                    int cpuMax = (Integer) cpuMaxSpinner.getValue();
                    int gpuMin = (Integer) gpuMinSpinner.getValue();
                    int gpuMax = (Integer) gpuMaxSpinner.getValue();
                    if (cpuMin >= cpuMax || gpuMin >= gpuMax) {
                        logMessage("Validation error: Ensure that minimum values are less than maximum values.");
                        return;
                    }
                    boolean sent = bluetoothService.sendProfileData(cpuMin, gpuMin, cpuMax, gpuMax);
                    if (sent) {
                        logMessage((currentProfileType == 1 ? "Linear" : "Exponential")
                            + " profile sent: CPU (" + cpuMin + "-" + cpuMax + "), GPU (" + gpuMin + "-" + gpuMax + ")");
                    } else {
                        logMessage("Failed to send profile data");
                    }
                }
            } else {
                logMessage("Cannot send profile: No device connected");
            }
        });
        buttonPanel.add(sendProfileButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Updates the center configuration panel according to the selected profile type.
     * For "Constant" it displays a single input; for "Linear"/"Exponential", it displays the full set of fields.
     */
    private void updateUIForProfileType() {
        // Remove any existing center component
        Component currentCenter = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (currentCenter != null) {
            remove(currentCenter);
        }
        
        JPanel configPanel;
        if (currentProfileType == 0) { // Constant profile: single spinner for constant value (0-100)
            configPanel = new JPanel(new GridLayout(6, 2, 10, 10));
            configPanel.setBorder(BorderFactory.createTitledBorder("Constant Profile Configuration"));
            
            configPanel.add(new JLabel("Fan Speed (%):"));
            constantValueSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));
            configPanel.add(constantValueSpinner);
            for (int i = 0; i < 4; i++) {
                configPanel.add(new JLabel("")); // Placeholder labels for layout
                configPanel.add(new JLabel());
            }
        } else { // Linear or Exponential profiles: full set of fields with range [30,100]
            configPanel = new JPanel(new GridLayout(6, 2, 10, 10));
            String header = currentProfileType == 1 ? "Linear Profile Configuration" : "Exponential Profile Configuration";
            configPanel.setBorder(BorderFactory.createTitledBorder(header));
            
            configPanel.add(new JLabel("CPU Min Temp (°C):"));
            cpuMinSpinner = new JSpinner(new SpinnerNumberModel(30, 30, 100, 1));
            configPanel.add(cpuMinSpinner);
            
            configPanel.add(new JLabel("CPU Max Temp (°C):"));
            cpuMaxSpinner = new JSpinner(new SpinnerNumberModel(70, 30, 100, 1));
            configPanel.add(cpuMaxSpinner);
            
            configPanel.add(new JLabel("GPU Min Temp (°C):"));
            gpuMinSpinner = new JSpinner(new SpinnerNumberModel(30, 30, 100, 1));
            configPanel.add(gpuMinSpinner);
            
            configPanel.add(new JLabel("GPU Max Temp (°C):"));
            gpuMaxSpinner = new JSpinner(new SpinnerNumberModel(70, 30, 100, 1));
            configPanel.add(gpuMaxSpinner);
            
            configPanel.add(new JLabel("Min Fan Speed (%):"));
            minSpeedSpinner = new JSpinner(new SpinnerNumberModel(30, 30, 100, 1));
            configPanel.add(minSpeedSpinner);
            
            configPanel.add(new JLabel("Max Fan Speed (%):"));
            maxSpeedSpinner = new JSpinner(new SpinnerNumberModel(100, 30, 100, 1));
            configPanel.add(maxSpeedSpinner);
        }
        
        add(configPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    /**
     * Logs a message.
     *
     * @param message The message to log.
     */
    private void logMessage(String message) {
        LOGGER.info(message);
    }
}