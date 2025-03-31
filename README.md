# HeatSync

A Java desktop application for monitoring system temperatures and controlling external cooling devices via Bluetooth.

## Features

- Real-time temperature monitoring for:
  - CPU
  - GPU
  - Disk
- Power consumption monitoring:
  - CPU power
  - GPU power
  - Total system power
- Bluetooth Low Energy (BLE) connectivity:
  - Device discovery with RSSI filtering
  - Device connection management
  - Fan control based on system temperatures
- Automatic and manual fan control modes
- Simple and intuitive Swing GUI interface

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Linux operating system (for hardware temperature monitoring)
- Bluetooth adapter with BLE support
- BlueZ (for Linux Bluetooth functionality)

## Building the Project

1. Clone the repository:
```bash
git clone https://github.com/yourusername/heatSyncJava.git
cd heatSyncJava
```

2. Build the project using Maven:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.heatsync.HeatSyncApp"
```

## Project Structure

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

## Architecture

The application follows a modular architecture:

1. **Presentation Layer** (`ui` package):
   - MainWindow - Main application container
   - TemperaturePanel - Displays temperature and power data
   - BluetoothPanel - Interface for Bluetooth device discovery and control

2. **Controller Layer** (`controller` package):
   - MonitoringController - Coordinates temperature monitoring and updates

3. **Service Layer** (`service` package):
   - TemperatureMonitor - Hardware temperature reading
   - PowerMonitor - Power consumption estimation
   - BluetoothService - Facade for all Bluetooth operations

4. **Bluetooth Module** (`service.bluetooth` package):
   - BluetoothManager - Coordinates Bluetooth components
   - BluetoothDeviceScanner - Bluetooth device discovery
   - BluetoothConnectionHandler - Connection management
   - BluetoothDataHandler - Data exchange with devices
   - BluetoothEventListener - Event notification interface

## Dependencies

- **JSensors (2.0.0)** - Hardware monitoring library for Linux
- **Blessed-Bluez** - Bluetooth Low Energy library for Linux
  - Based on BlueZ and D-Bus
- **SLF4J/Logback** - Logging framework
- **JUnit (4.13.2)** - Testing framework
- **Swing** - GUI components (JDK built-in)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
