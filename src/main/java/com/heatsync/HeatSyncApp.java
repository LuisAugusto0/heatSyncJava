package com.heatsync;

import com.heatsync.service.BluetoothService;
import com.heatsync.service.TemperatureMonitor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HeatSyncApp {
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
    
    private TemperatureMonitor temperatureMonitor;
    private BluetoothService bluetoothService;
    private Timer updateTimer;
    private boolean autoMode = true;

    public HeatSyncApp() {
        temperatureMonitor = new TemperatureMonitor();
        bluetoothService = new BluetoothService();
        initializeGUI();
        startTemperatureUpdates();
    }

    private void initializeGUI() {
        mainFrame = new JFrame("HeatSync");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(500, 400);
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
        
        // Painel de controle do Bluetooth e Fan
        JPanel controlPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles (Bluetooth Desativado)"));
        
        // Status da conexão
        connectionStatusLabel = new JLabel("Status: Desconectado");
        connectionStatusLabel.setForeground(Color.RED);
        
        // Botão de conexão
        connectButton = new JButton("Conectar ESP32 (Desativado)");
        connectButton.setEnabled(false);
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logMessage("Botão Conectar clicado, mas funcionalidade Bluetooth está desativada.");
                JOptionPane.showMessageDialog(mainFrame, "A funcionalidade de conexão Bluetooth está desativada nesta versão.", "Bluetooth Desativado", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        // Toggle para modo automático/manual
        autoManualToggle = new JToggleButton("Modo: Automático");
        autoManualToggle.setSelected(true);
        autoManualToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoMode = autoManualToggle.isSelected();
                if (autoMode) {
                    autoManualToggle.setText("Modo: Automático");
                    fanSpeedSlider.setEnabled(false);
                    logMessage("Modo automático ativado");
                } else {
                    autoManualToggle.setText("Modo: Manual");
                    fanSpeedSlider.setEnabled(true);
                    logMessage("Modo manual ativado (envio de comando desativado)");
                }
            }
        });
        
        // Slider para controle manual da fan
        fanSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        fanSpeedSlider.setMajorTickSpacing(25);
        fanSpeedSlider.setMinorTickSpacing(5);
        fanSpeedSlider.setPaintTicks(true);
        fanSpeedSlider.setPaintLabels(true);
        fanSpeedSlider.setEnabled(false);
        fanSpeedSlider.addChangeListener(e -> {
            if (!autoMode && !fanSpeedSlider.getValueIsAdjusting()) {
                int value = fanSpeedSlider.getValue();
                logMessage("Fan speed manual ajustado para: " + value + "% (envio desativado)");
            }
        });
        
        controlPanel.add(connectionStatusLabel);
        controlPanel.add(connectButton);
        controlPanel.add(autoManualToggle);
        controlPanel.add(fanSpeedSlider);
        
        // Painel superior combinando temperatura e controles
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.add(tempPanel);
        topPanel.add(controlPanel);
        
        // Área de log
        logTextArea = new JTextArea(10, 40);
        logTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        
        // Adiciona componentes ao frame
        mainFrame.add(topPanel, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        
        // Fecha conexões e serviço Bluetooth quando o aplicativo for fechado
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (updateTimer != null) {
                    updateTimer.cancel();
                }
                System.exit(0);
            }
        });
        
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
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
            logMessage("Funcionalidade Bluetooth está DESATIVADA nesta versão.");
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