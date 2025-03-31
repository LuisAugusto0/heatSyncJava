package com.heatsync.service;

import com.profesorfalken.jsensors.JSensors;
import com.profesorfalken.jsensors.model.components.Cpu;
import com.profesorfalken.jsensors.model.components.Disk;
import com.profesorfalken.jsensors.model.components.Gpu;
import com.profesorfalken.jsensors.model.sensors.Temperature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço responsável por monitorar as temperaturas dos componentes do sistema.
 * Utiliza a biblioteca JSensors para obter as informações dos sensores.
 */
public class TemperatureMonitor {
    private static final Logger LOGGER = Logger.getLogger(TemperatureMonitor.class.getName());
    
    // Valores de limite para normalização de temperatura
    private static final double MILLIGRADE_THRESHOLD = 1000.0;
    private static final double DECIGRADE_THRESHOLD = 200.0;
    private static final double FAHRENHEIT_THRESHOLD = 100.0;
    
    // Fatores de conversão
    private static final double MILLIGRADE_FACTOR = 100.0;
    private static final double DECIGRADE_FACTOR = 10.0;
    
    private List<Cpu> cpus;
    private List<Gpu> gpus;
    private List<Disk> disks;

    /**
     * Construtor padrão, inicializa os componentes e seus sensores.
     */
    public TemperatureMonitor() {
        initializeComponents();
    }

    /**
     * Inicializa e atualiza os componentes e seus sensores.
     * Habilita o modo de depuração no JSensors e registra informações detalhadas.
     */
    private void initializeComponents() {
        // Habilita o modo de depuração no JSensors
        Map<String, String> config = new HashMap<>();
        config.put("debugMode", "true");
        
        cpus = JSensors.get.config(config).components().cpus;
        gpus = JSensors.get.config(config).components().gpus;
        disks = JSensors.get.config(config).components().disks;
        
        // Registra informações detalhadas dos sensores
        logSensorDetails();
    }
    
    /**
     * Registra informações detalhadas sobre todos os sensores encontrados.
     */
    private void logSensorDetails() {
        LOGGER.info("===== DETAILED SENSOR INFORMATION =====");
        
        logComponentDetails("CPU", cpus);
        logComponentDetails("GPU", gpus);
        logComponentDetails("DISK", disks);
        
        LOGGER.info("=======================================");
    }
    
    /**
     * Registra informações detalhadas para uma categoria específica de componentes.
     * 
     * @param componentType O tipo de componente (CPU, GPU, DISK)
     * @param components A lista de componentes a ser registrada
     */
    private <T> void logComponentDetails(String componentType, List<T> components) {
        if (components == null || components.isEmpty()) {
            LOGGER.info(componentType + " SENSORS: None found");
            return;
        }
        
        LOGGER.info(componentType + " SENSORS:");
        
        components.forEach(component -> {
            String name = "";
            List<Temperature> temperatures = null;
            
            if (component instanceof Cpu) {
                Cpu cpu = (Cpu) component;
                name = cpu.name;
                temperatures = cpu.sensors.temperatures;
            } else if (component instanceof Gpu) {
                Gpu gpu = (Gpu) component;
                name = gpu.name;
                temperatures = gpu.sensors.temperatures;
            } else if (component instanceof Disk) {
                Disk disk = (Disk) component;
                name = disk.name;
                temperatures = disk.sensors.temperatures;
            }
            
            LOGGER.info(componentType + " Name: " + name);
            
            if (temperatures != null) {
                temperatures.forEach(temp -> 
                    LOGGER.info("  Temperature: " + temp.name + " = " + temp.value + 
                               " (Normalized: " + normalizeTemperature(temp.value) + "°C)"));
            }
        });
    }

    /**
     * Obtém a temperatura média da CPU.
     * 
     * @return A temperatura média da CPU em graus Celsius
     */
    public double getCpuTemperature() {
        return cpus.stream()
                .flatMap(cpu -> cpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Obtém a temperatura média da GPU.
     * 
     * @return A temperatura média da GPU em graus Celsius
     */
    public double getGpuTemperature() {
        return gpus.stream()
                .flatMap(gpu -> gpu.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Obtém a temperatura média do disco.
     * 
     * @return A temperatura média do disco em graus Celsius
     */
    public double getDiskTemperature() {
        return disks.stream()
                .flatMap(disk -> disk.sensors.temperatures.stream())
                .filter(temp -> temp != null && temp.value > 0)
                .mapToDouble(temp -> normalizeTemperature(temp.value))
                .average()
                .orElse(0.0);
    }

    /**
     * Normaliza as temperaturas para a escala Celsius adequada com base no valor reportado.
     * 
     * @param value O valor bruto da temperatura
     * @return O valor normalizado em Celsius
     */
    private double normalizeTemperature(double value) {
        LOGGER.log(Level.FINE, "Normalizando temperatura: {0}", value);
        
        if (value > MILLIGRADE_THRESHOLD) {
            // Se o valor estiver na casa de milhares, provavelmente é miligraus (mC)
            // Exemplo: 3385 mC = 33.85 C
            return value / MILLIGRADE_FACTOR;
        } else if (value > DECIGRADE_THRESHOLD) {
            // Se o valor estiver entre 200 e 1000, provavelmente é decigraus (dC)
            // Exemplo: 538 dC = 53.8 C
            return value / DECIGRADE_FACTOR;
        } else if (value > FAHRENHEIT_THRESHOLD) {
            // Alguns sensores podem reportar em Fahrenheit
            // Converter para Celsius: (F - 32) * 5/9
            return (value - 32.0) * 5.0 / 9.0;
        } else {
            // Valores abaixo de 100 provavelmente já estão em Celsius
            return value;
        }
    }

    /**
     * Atualiza as informações de temperatura de todos os componentes.
     */
    public void updateTemperatures() {
        // Atualiza os dados dos componentes
        initializeComponents();
    }
} 