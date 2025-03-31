# HeatSync

A Java desktop application for monitoring system temperatures and controlling external cooling devices.

## Features

- Real-time temperature monitoring for:
  - CPU
  - GPU
  - Disk
- Automatic temperature updates every 3 seconds
- Simple and intuitive GUI interface

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Linux operating system (for hardware temperature monitoring)

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
│   │   │           └── service/
│   │   │               └── TemperatureMonitor.java
│   │   └── resources/
│   └── test/
│       └── java/
│           └── com/
│               └── heatsync/
└── pom.xml
```

## Dependencies

- Jsensors (2.0.0) - For hardware temperature monitoring
- JUnit (4.13.2) - For testing

## License

This project is licensed under the MIT License - see the LICENSE file for details.
