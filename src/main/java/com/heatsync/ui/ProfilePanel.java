package com.heatsync.ui;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.FanProfileIOService;
import com.heatsync.service.configIO.FanProfileConfigIO;
import com.profesorfalken.jsensors.model.sensors.Fan;

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
    private JSpinner kSpinner;

    private int defaultCpuMinSpinner;
    private int defaultCpuMaxSpinner;
    private int defaultGpuMinSpinner;
    private int defaultGpuMaxSpinner;
    private int defaultMinSpeedSpinner;
    private int defaultMaxSpeedSpinner;
    private double defaultKSpinner;

    
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
        
        defaultCpuMaxSpinner = FanProfileIOService.getMaxCpu();
        defaultCpuMinSpinner = FanProfileIOService.getMinCpu();
        defaultGpuMaxSpinner = FanProfileIOService.getMaxGpu();
        defaultGpuMinSpinner = FanProfileIOService.getMinGpu();
        defaultMinSpeedSpinner = FanProfileIOService.getMinSpeed();
        defaultMaxSpeedSpinner = FanProfileIOService.getMaxSpeed();
        defaultKSpinner = FanProfileIOService.getCurveGrowthConstant();

        // Top panel with profile type selection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Profile Type:"));

        JRadioButton constantButton = new JRadioButton("Constant", true);
        JRadioButton simplifiedButton = new JRadioButton("Simplified temperature control (linear)");
        JRadioButton completeButton = new JRadioButton("Custom temperature control");

        // Group the buttons for single selection
        ButtonGroup group = new ButtonGroup();
        group.add(constantButton);
        group.add(simplifiedButton);
        group.add(completeButton);

        // Add ActionListener to update the current profile
        ActionListener profileListener = e -> {
            if (constantButton.isSelected()) {
                currentProfileType = 0;
            } else if (simplifiedButton.isSelected()) {
                currentProfileType = 1;
            } else if (completeButton.isSelected()) {
                currentProfileType = 2;
            }
            updateUIForProfileType();
        };

        constantButton.addActionListener(profileListener);
        simplifiedButton.addActionListener(profileListener);
        completeButton.addActionListener(profileListener);

        topPanel.add(constantButton);
        topPanel.add(simplifiedButton);
        topPanel.add(completeButton);

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
                    boolean sent = bluetoothService.sendConstantProfile(constantValue);
                    if (sent) {
                        logMessage("Constant profile sent: " + constantValue);
                    } else {
                        logMessage("Failed to send constant profile");
                    }
                } else if(currentProfileType == 1) { // Simplified profile
                    int cpuMin = (Integer) cpuMinSpinner.getValue();
                    int cpuMax = (Integer) cpuMaxSpinner.getValue();
                    int gpuMin = (Integer) gpuMinSpinner.getValue();
                    int gpuMax = (Integer) gpuMaxSpinner.getValue();
                    if (cpuMin >= cpuMax || gpuMin >= gpuMax) {
                        logMessage("Validation error: Ensure that minimum values are less than maximum values.");
                        return;
                    }
                    boolean sent = bluetoothService.sendSimplifiedProfileData(cpuMin, gpuMin, cpuMax, gpuMax);
                    if (sent) {
                        logMessage("Simplified" + " profile sent: CPU (" + cpuMin + "-" + cpuMax + "), GPU (" + gpuMin + "-" + gpuMax + ")");
                    } else {
                        logMessage("Failed to send profile data");
                    }
                } else { // Custom profile
                    int cpuMin = (Integer) cpuMinSpinner.getValue();
                    int cpuMax = (Integer) cpuMaxSpinner.getValue();
                    int gpuMin = (Integer) gpuMinSpinner.getValue();
                    int gpuMax = (Integer) gpuMaxSpinner.getValue();
                    int minSpeed = (Integer) minSpeedSpinner.getValue();
                    int maxSpeed = (Integer) maxSpeedSpinner.getValue();
                    Double k = (Double) kSpinner.getValue();
                    if (cpuMin >= cpuMax || gpuMin >= gpuMax || minSpeed >= maxSpeed) {
                        logMessage("Validation error: Ensure that minimum values are less than maximum values.");
                        return;
                    } 
                    boolean sent = bluetoothService.sendProfileData(cpuMin, gpuMin, cpuMax, gpuMax, minSpeed, maxSpeed, k);
                    if (sent) {
                        logMessage("Custom profile sent: CPU (" + cpuMin + "-" + cpuMax + "), GPU (" + gpuMin + "-" + gpuMax + "), Fan Speed (" + minSpeed + "-" + maxSpeed + ") with k = " + k);
                    } else {
                        logMessage("Failed to send custom profile data");
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
            configPanel = new JPanel(new GridLayout(4, 2, 10, 10));
            configPanel.setBorder(BorderFactory.createTitledBorder("Constant Profile Configuration"));
            
            // Fan Speed (with help icon)
            JPanel fanSpeedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel fanSpeedHelp = new JLabel("?");
            fanSpeedHelp.setForeground(Color.BLUE);
            fanSpeedHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String fanSpeedTooltip = "<html>Sets the constant fan speed percentage.<br>"
                    + "Choose a value between 0 and 100</html>";
            fanSpeedHelp.setToolTipText(fanSpeedTooltip);
            fanSpeedHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, fanSpeedTooltip, "Fan Speed Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            fanSpeedPanel.add(fanSpeedHelp);
            fanSpeedPanel.add(new JLabel("Fan Speed (%):"));
            configPanel.add(fanSpeedPanel);
            
            constantValueSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 1));
            configPanel.add(constantValueSpinner);
            
            for (int i = 0; i < 2; i++) {
                configPanel.add(new JLabel("")); // Placeholder labels for layout
                configPanel.add(new JLabel(""));
            }
            
        } else if(currentProfileType == 1) { // Simplified profile: full set of fields with range [30,100]
            configPanel = new JPanel(new GridLayout(2, 4, 10, 10));
            String header = "Simplified Profile Configuration";
            configPanel.setBorder(BorderFactory.createTitledBorder(header));
            
            // CPU Min Temp
            JPanel cpuMinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel cpuMinHelp = new JLabel("?");
            cpuMinHelp.setForeground(Color.BLUE);
            cpuMinHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String cpuMinTooltip = "<html>Sets the minimum temperature of the CPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            cpuMinHelp.setToolTipText(cpuMinTooltip);
            cpuMinHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, cpuMinTooltip, "CPU Min Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            cpuMinPanel.add(cpuMinHelp);
            cpuMinPanel.add(new JLabel("CPU Min Temp (°C):"));
            configPanel.add(cpuMinPanel);
            cpuMinSpinner = new JSpinner(new SpinnerNumberModel(defaultCpuMinSpinner, 30, 100, 1));
            configPanel.add(cpuMinSpinner);
            
            // CPU Max Temp
            JPanel cpuMaxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel cpuMaxHelp = new JLabel("?");
            cpuMaxHelp.setForeground(Color.BLUE);
            cpuMaxHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String cpuMaxTooltip = "<html>Sets the maximum temperature of the CPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            cpuMaxHelp.setToolTipText(cpuMaxTooltip);
            cpuMaxHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, cpuMaxTooltip, "CPU Max Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            cpuMaxPanel.add(cpuMaxHelp);
            cpuMaxPanel.add(new JLabel("CPU Max Temp (°C):"));
            configPanel.add(cpuMaxPanel);
            cpuMaxSpinner = new JSpinner(new SpinnerNumberModel(defaultCpuMaxSpinner, 30, 100, 1));
            configPanel.add(cpuMaxSpinner);
            
            // GPU Min Temp
            JPanel gpuMinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel gpuMinHelp = new JLabel("?");
            gpuMinHelp.setForeground(Color.BLUE);
            gpuMinHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String gpuMinTooltip = "<html>Sets the minimum temperature of the GPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            gpuMinHelp.setToolTipText(gpuMinTooltip);
            gpuMinHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, gpuMinTooltip, "GPU Min Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            gpuMinPanel.add(gpuMinHelp);
            gpuMinPanel.add(new JLabel("GPU Min Temp (°C):"));
            configPanel.add(gpuMinPanel);
            gpuMinSpinner = new JSpinner(new SpinnerNumberModel(defaultGpuMinSpinner, 30, 100, 1));
            configPanel.add(gpuMinSpinner);
            
            // GPU Max Temp
            JPanel gpuMaxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel gpuMaxHelp = new JLabel("?");
            gpuMaxHelp.setForeground(Color.BLUE);
            gpuMaxHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String gpuMaxTooltip = "<html>Sets the maximum temperature of the GPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            gpuMaxHelp.setToolTipText(gpuMaxTooltip);
            gpuMaxHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, gpuMaxTooltip, "GPU Max Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            gpuMaxPanel.add(gpuMaxHelp);
            gpuMaxPanel.add(new JLabel("GPU Max Temp (°C):"));
            configPanel.add(gpuMaxPanel);
            gpuMaxSpinner = new JSpinner(new SpinnerNumberModel(65, 30, 100, 1));
            configPanel.add(gpuMaxSpinner);
            
        } else { // Custom profile: full set of fields with range [30,100] for temps and [0,100] for fan speed and an exponent k parameter
            configPanel = new JPanel(new GridLayout(4, 4, 10, 10));
            String header = "Custom Profile Configuration";
            configPanel.setBorder(BorderFactory.createTitledBorder(header));
            
            // CPU Min Temp
            JPanel cpuMinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel cpuMinHelp = new JLabel("?");
            cpuMinHelp.setForeground(Color.BLUE);
            cpuMinHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String cpuMinTooltip = "<html>Sets the minimum temperature of the CPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            cpuMinHelp.setToolTipText(cpuMinTooltip);
            cpuMinHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, cpuMinTooltip, "CPU Min Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            cpuMinPanel.add(cpuMinHelp);
            cpuMinPanel.add(new JLabel("CPU Min Temp (°C):"));
            configPanel.add(cpuMinPanel);
            cpuMinSpinner = new JSpinner(new SpinnerNumberModel(defaultCpuMinSpinner, 30, 100, 1));
            configPanel.add(cpuMinSpinner);
            
            // CPU Max Temp
            JPanel cpuMaxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel cpuMaxHelp = new JLabel("?");
            cpuMaxHelp.setForeground(Color.BLUE);
            cpuMaxHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String cpuMaxTooltip = "<html>Sets the maximum temperature of the CPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            cpuMaxHelp.setToolTipText(cpuMaxTooltip);
            cpuMaxHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, cpuMaxTooltip, "CPU Max Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            cpuMaxPanel.add(cpuMaxHelp);
            cpuMaxPanel.add(new JLabel("CPU Max Temp (°C):"));
            configPanel.add(cpuMaxPanel);
            cpuMaxSpinner = new JSpinner(new SpinnerNumberModel(defaultCpuMaxSpinner, 30, 100, 1));
            configPanel.add(cpuMaxSpinner);
            
            // GPU Min Temp
            JPanel gpuMinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel gpuMinHelp = new JLabel("?");
            gpuMinHelp.setForeground(Color.BLUE);
            gpuMinHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String gpuMinTooltip = "<html>Sets the minimum temperature of the GPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            gpuMinHelp.setToolTipText(gpuMinTooltip);
            gpuMinHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, gpuMinTooltip, "GPU Min Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            gpuMinPanel.add(gpuMinHelp);
            gpuMinPanel.add(new JLabel("GPU Min Temp (°C):"));
            configPanel.add(gpuMinPanel);
            gpuMinSpinner = new JSpinner(new SpinnerNumberModel(defaultGpuMinSpinner, 30, 100, 1));
            configPanel.add(gpuMinSpinner);
            
            // GPU Max Temp
            JPanel gpuMaxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel gpuMaxHelp = new JLabel("?");
            gpuMaxHelp.setForeground(Color.BLUE);
            gpuMaxHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String gpuMaxTooltip = "<html>Sets the maximum temperature of the GPU's temperature range to be used for calculating the fan speed.<br>"
                    + "Choose a value between 30 and 100</html>";
            gpuMaxHelp.setToolTipText(gpuMaxTooltip);
            gpuMaxHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, gpuMaxTooltip, "GPU Max Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            gpuMaxPanel.add(gpuMaxHelp);
            gpuMaxPanel.add(new JLabel("GPU Max Temp (°C):"));
            configPanel.add(gpuMaxPanel);
            gpuMaxSpinner = new JSpinner(new SpinnerNumberModel(defaultGpuMaxSpinner, 30, 100, 1));
            configPanel.add(gpuMaxSpinner);
            
            // Fan Speed Min
            JPanel fanSpeedMinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel fanSpeedMinHelp = new JLabel("?");
            fanSpeedMinHelp.setForeground(Color.BLUE);
            fanSpeedMinHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String fanSpeedMinTooltip = "<html>Sets the minimum fan speed percentage.<br>"
                    + "Choose a value between 0 and 100</html>";
            fanSpeedMinHelp.setToolTipText(fanSpeedMinTooltip);
            fanSpeedMinHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, fanSpeedMinTooltip, "Fan Speed Min Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            fanSpeedMinPanel.add(fanSpeedMinHelp);
            fanSpeedMinPanel.add(new JLabel("Fan Speed Min (%):"));
            configPanel.add(fanSpeedMinPanel);
            minSpeedSpinner = new JSpinner(new SpinnerNumberModel(defaultCpuMinSpinner, 0, 100, 1));
            configPanel.add(minSpeedSpinner);
            
            // Fan Speed Max
            JPanel fanSpeedMaxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel fanSpeedMaxHelp = new JLabel("?");
            fanSpeedMaxHelp.setForeground(Color.BLUE);
            fanSpeedMaxHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String fanSpeedMaxTooltip = "<html>Sets the maximum fan speed percentage.<br>"
                    + "Choose a value between 0 and 100</html>";
            fanSpeedMaxHelp.setToolTipText(fanSpeedMaxTooltip);
            fanSpeedMaxHelp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, fanSpeedMaxTooltip, "Fan Speed Max Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            fanSpeedMaxPanel.add(fanSpeedMaxHelp);
            fanSpeedMaxPanel.add(new JLabel("Fan Speed Max (%):"));
            configPanel.add(fanSpeedMaxPanel);
            maxSpeedSpinner = new JSpinner(new SpinnerNumberModel(defaultMaxSpeedSpinner, 0, 100, 1));
            configPanel.add(maxSpeedSpinner);
            
            // Parameter k
            JPanel kPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel kHelpLabel = new JLabel("?");
            kHelpLabel.setForeground(Color.BLUE);
            kHelpLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            String kTooltip = "<html>If K = 1, the growth is linear.<br>"
                    + "If K &gt; 1, slower response at first, with a more aggressive increase near the limit — ideal if you want quiet operation until temperature reaches critical.<br>"
                    + "If K &lt; 1, quicker response at first, with a slower increase near the limit — ideal if you want the cooler to always run at high speed.<br><br>"
                    + "Choose a value between 0.25 and 2.0</html>";
            kHelpLabel.setToolTipText(kTooltip);
            kHelpLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, kTooltip, "K Parameter Description", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            kPanel.add(kHelpLabel);
            kPanel.add(new JLabel("Exponent K value:"));
            configPanel.add(kPanel);
            kSpinner = new JSpinner(new SpinnerNumberModel(defaultKSpinner, 0.25d, 2.0d, 0.1d));
            configPanel.add(kSpinner);
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
        mainWindow.logMessage(message);
    }
}