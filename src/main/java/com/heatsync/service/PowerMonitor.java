package com.heatsync.service;

import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço responsável por monitorar o consumo de energia da CPU e GPU.
 * Utiliza a biblioteca JPowerMonitor para obter informações de potência.
 */
public class PowerMonitor {
    private static final Logger LOGGER = Logger.getLogger(PowerMonitor.class.getName());
    
    private static final String CONFIG_FILE = "jpowermonitor.yaml";
    private double cpuPowerWatts = 0.0;
    private double gpuPowerWatts = 0.0;
    private boolean initialized = false;
    
    // Valores estimados de consumo para diferentes cargas de CPU
    private static final double CPU_MAX_WATTS = 65.0; // Valor máximo de consumo do CPU em watts (depende do processador)
    private static final double CPU_MIN_WATTS = 10.0; // Valor de consumo do CPU em idle
    
    // Valores estimados de consumo para diferentes cargas de GPU
    private static final double GPU_MAX_WATTS = 75.0; // Valor máximo de consumo da GPU (depende da placa de vídeo)
    private static final double GPU_MIN_WATTS = 5.0;  // Valor de consumo da GPU em idle
    
    private final TemperatureMonitor temperatureMonitor;
    
    /**
     * Construtor que recebe uma instância do TemperatureMonitor para acesso aos dados de carga.
     * 
     * @param temperatureMonitor O monitor de temperatura existente
     */
    public PowerMonitor(TemperatureMonitor temperatureMonitor) {
        this.temperatureMonitor = temperatureMonitor;
        initialize();
    }
    
    /**
     * Inicializa o monitor de energia.
     * Verifica se existe o arquivo de configuração do JPowerMonitor.
     */
    private void initialize() {
        // Verifica se existe o arquivo de configuração
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            LOGGER.info("Arquivo de configuração JPowerMonitor não encontrado. Usando estimativas baseadas em carga.");
            createDefaultConfigFile();
        } else {
            LOGGER.info("Usando arquivo de configuração JPowerMonitor existente: " + configFile.getAbsolutePath());
        }
        
        initialized = true;
    }
    
    /**
     * Cria um arquivo de configuração padrão para o JPowerMonitor.
     */
    private void createDefaultConfigFile() {
        String defaultConfig = 
            "measurement:\n" +
            "  method: est\n" +
            "  est:\n" +
            "    cpuMinWatts: " + CPU_MIN_WATTS + "\n" +
            "    cpuMaxWatts: " + CPU_MAX_WATTS + "\n";
        
        try {
            Files.write(Paths.get(CONFIG_FILE), defaultConfig.getBytes());
            LOGGER.info("Arquivo de configuração padrão do JPowerMonitor criado: " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.warning("Não foi possível criar arquivo de configuração padrão: " + e.getMessage());
        }
    }
    
    /**
     * Atualiza as informações de consumo de energia.
     * Calcula o consumo de energia com base na carga da CPU/GPU.
     */
    public void updatePowerConsumption() {
        if (!initialized) {
            initialize();
        }
        
        // Obter a carga da CPU via temperatureMonitor
        // Esta é uma implementação estimada baseada na temperatura
        // Um algoritmo mais preciso seria baseado na carga real da CPU
        double cpuTemp = temperatureMonitor.getCpuTemperature();
        double gpuTemp = temperatureMonitor.getGpuTemperature();
        
        // Estima a carga com base na temperatura (simplificação)
        // Assume temperatura idle de 30°C e temperatura máxima de 90°C
        double cpuLoadPercentage = estimateLoadFromTemperature(cpuTemp, 30.0, 90.0);
        double gpuLoadPercentage = estimateLoadFromTemperature(gpuTemp, 30.0, 90.0);
        
        // Calcula o consumo de energia estimado
        cpuPowerWatts = CPU_MIN_WATTS + (cpuLoadPercentage/100.0) * (CPU_MAX_WATTS - CPU_MIN_WATTS);
        gpuPowerWatts = GPU_MIN_WATTS + (gpuLoadPercentage/100.0) * (GPU_MAX_WATTS - GPU_MIN_WATTS);
        
        LOGGER.fine(String.format("Consumo estimado: CPU=%.2fW (carga: %.1f%%), GPU=%.2fW (carga: %.1f%%)", 
            cpuPowerWatts, cpuLoadPercentage, gpuPowerWatts, gpuLoadPercentage));
    }
    
    /**
     * Estima a porcentagem de carga com base na temperatura.
     * 
     * @param currentTemp A temperatura atual do componente
     * @param idleTemp A temperatura esperada quando o componente está em idle
     * @param maxTemp A temperatura máxima esperada quando o componente está em carga completa
     * @return A porcentagem de carga estimada (0-100)
     */
    private double estimateLoadFromTemperature(double currentTemp, double idleTemp, double maxTemp) {
        // Limita os valores para evitar porcentagens negativas ou acima de 100%
        if (currentTemp <= idleTemp) return 0.0;
        if (currentTemp >= maxTemp) return 100.0;
        
        // Calcula a porcentagem linearmente
        return ((currentTemp - idleTemp) / (maxTemp - idleTemp)) * 100.0;
    }
    
    /**
     * Obtém o consumo de energia estimado da CPU em watts.
     * 
     * @return O consumo da CPU em watts
     */
    public double getCpuPowerWatts() {
        return cpuPowerWatts;
    }
    
    /**
     * Obtém o consumo de energia estimado da GPU em watts.
     * 
     * @return O consumo da GPU em watts
     */
    public double getGpuPowerWatts() {
        return gpuPowerWatts;
    }
    
    /**
     * Obtém o consumo total estimado (CPU + GPU) em watts.
     * 
     * @return O consumo total em watts
     */
    public double getTotalPowerWatts() {
        return cpuPowerWatts + gpuPowerWatts;
    }
} 