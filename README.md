# HeatSync

Uma aplicação desktop Java para monitorar temperaturas do sistema e controlar dispositivos de resfriamento externos via Bluetooth.

## Recursos

- Monitoramento de temperatura em tempo real para:
  - CPU
  - GPU
  - Disco
- Monitoramento do consumo de energia:
  - Energia da CPU
  - Energia da GPU
  - Energia total do sistema
- Conectividade Bluetooth Low Energy (BLE):
  - Descoberta de dispositivos com filtragem RSSI
  - Gerenciamento de conexão de dispositivos
  - Controle de ventilador baseado nas temperaturas do sistema
- Modos de controle automático e manual do ventilador
- Interface gráfica Swing simples e intuitiva

## Requisitos

- Java 17 ou superior
- Maven 3.6 ou superior
- Sistema operacional Linux (para monitoramento de temperatura do hardware)
- Adaptador Bluetooth com suporte a BLE
- BlueZ (para funcionalidade Bluetooth no Linux)

## Compilando o Projeto

1. Clone o repositório:
```bash
git clone https://github.com/yourusername/heatSyncJava.git
cd heatSyncJava
```

2. Compile o projeto usando Maven:
```bash
mvn clean install
```

3. Execute a aplicação:
```bash
mvn exec:java -Dexec.mainClass="com.heatsync.HeatSyncApp"
```

## Estrutura do Projeto

```
heatSyncJava/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── heatsync/
│   │   │           ├── HeatSyncApp.java
│   │   │           ├── controller/
│   │   │           │   └── MonitoringController.java
│   │   │           ├── ui/
│   │   │           │   ├── BluetoothPanel.java
│   │   │           │   ├── MainWindow.java
│   │   │           │   └── TemperaturePanel.java
│   │   │           └── service/
│   │   │               ├── BluetoothService.java
│   │   │               ├── PowerMonitor.java
│   │   │               ├── TemperatureMonitor.java
│   │   │               └── bluetooth/
│   │   │                   ├── BluetoothConnectionHandler.java
│   │   │                   ├── BluetoothDataHandler.java
│   │   │                   ├── BluetoothDeviceScanner.java
│   │   │                   ├── BluetoothEventListener.java
│   │   │                   └── BluetoothManager.java
│   │   └── resources/
│   └── test/
│       └── java/
│           └── com/
│               └── heatsync/
└── pom.xml
```

## Arquitetura

A aplicação segue uma arquitetura modular:

1. **Camada de Apresentação** (`ui` package):
   - MainWindow - Contêiner principal da aplicação
   - TemperaturePanel - Exibe dados de temperatura e potência
   - BluetoothPanel - Interface para descoberta e controle de dispositivos Bluetooth

2. **Camada de Controle** (`controller` package):
   - MonitoringController - Coordena monitoramento e atualizações

3. **Camada de Serviço** (`service` package):
   - TemperatureMonitor - Leitura de temperatura do hardware
   - PowerMonitor - Estimativa de consumo de energia
   - BluetoothService - Facade para todas as operações Bluetooth

4. **Módulo Bluetooth** (`service.bluetooth` package):
   - BluetoothManager - Coordena componentes Bluetooth
   - BluetoothDeviceScanner - Descoberta de dispositivos Bluetooth
   - BluetoothConnectionHandler - Gerenciamento de conexão
   - BluetoothDataHandler - Troca de dados com dispositivos
   - BluetoothEventListener - Interface de notificação de eventos

## Dependências

- **JSensors (2.0.0)** - Biblioteca de monitoramento de hardware para Linux
- **Blessed-Bluez** - Biblioteca Bluetooth Low Energy para Linux
  - Baseada no BlueZ e D-Bus
- **SLF4J/Logback** - Estrutura de registro
- **JUnit (4.13.2)** - Estrutura de teste
- **Swing** - Componentes GUI (JDK integrado)

## Licença

Este projeto é licenciado sob a Licença MIT - veja o arquivo LICENSE para detalhes.
