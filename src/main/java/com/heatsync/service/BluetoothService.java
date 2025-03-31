package com.heatsync.service;

// Removido TinyB e outras importações não utilizadas
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço que ANTES gerenciava comunicação Bluetooth.
 * Agora está desativado, mantendo a estrutura para a UI.
 */
public class BluetoothService {
    private static final Logger LOGGER = Logger.getLogger(BluetoothService.class.getName());
    private boolean connected = false; // Mantém estado falso

    /**
     * Construtor padrão.
     */
    public BluetoothService() {
        LOGGER.info("BluetoothService instanciado (Funcionalidade Bluetooth DESATIVADA)");
        // Nenhuma inicialização necessária
    }

    /**
     * Simula a descoberta de dispositivos (não faz nada).
     * @return Sempre false.
     */
    public boolean startDeviceDiscovery() {
        LOGGER.warning("startDeviceDiscovery chamado, mas Bluetooth está desativado.");
        return false;
    }

    /**
     * Retorna uma lista vazia de dispositivos.
     * @return Lista vazia.
     */
    public List<?> getDiscoveredDevices() { // Usando wildcard genérico
        return Collections.emptyList();
    }

    /**
     * Simula a tentativa de conexão (não faz nada).
     * @return Sempre false.
     */
    public boolean connectToESP32() {
        LOGGER.warning("connectToESP32 chamado, mas Bluetooth está desativado.");
        connected = false;
        return false;
    }

    /**
     * Simula o envio de dados (não faz nada).
     * @return Sempre false.
     */
    public boolean sendTemperatureData(double cpuTemp, double gpuTemp, double diskTemp) {
        if (!connected) {
            LOGGER.warning("Tentativa de enviar dados sem conexão (Bluetooth desativado).");
        }
        // Não envia nada
        return false;
    }

    /**
     * Simula o recebimento de dados (não faz nada).
     * @return Sempre null.
     */
    public String receiveData() {
        // Não recebe nada
        return null;
    }

    /**
     * Simula o fechamento da conexão.
     */
    public void closeConnection() {
        LOGGER.info("closeConnection chamado (Bluetooth desativado).");
        connected = false;
        // Nenhuma conexão real para fechar
    }

    /**
     * Verifica se está conectado.
     * @return Sempre false.
     */
    public boolean isConnected() {
        return connected; // Sempre false
    }

    /**
     * Simula o desligamento do serviço.
     */
    public void shutdown() {
        LOGGER.info("shutdown chamado para BluetoothService (desativado).");
        // Nenhuma limpeza necessária
    }
}
