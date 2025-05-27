package com.heatsync.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class CsvLogger {
    private File csvFile;
    private FileWriter fileWriter;
    private String lastTimestamp = "";
    private LocalDateTime lastTimestampDateTime = LocalDateTime.now();
    private LocalDateTime firstTimestampDateTime;
    private int lastRpm = -1;
    private boolean lastIsTestRunning = false;
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CsvLogger.class.getName());
    private final Consumer<String> logCallback;

    /**
     * Constructor for CsvLogger.
     * Initializes the CSV file and prepares it for logging RPM data.
     *
     * @param logCallback A callback function to log messages to the UI or console
     */
    public CsvLogger(Consumer<String> logCallback){
        this.logCallback = logCallback;
        try{
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }            

            
        } catch (NullPointerException e) {
            LOGGER.severe("Error creating logs directory: " + e.getMessage());
            e.printStackTrace();
            logCallback.accept("Error creating logs directory");
            
        }
    }

    public void start() {
        LocalDateTime now = LocalDateTime.now();
        firstTimestampDateTime = now;
        String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        // Create filename with current date
        String filePath = "logs/rpm_log_" + date + ".csv";
        csvFile = new File(filePath);
        try {
            // Check if the file already exists
            boolean isNewFile = !csvFile.exists();
            fileWriter = new FileWriter(csvFile, true);
            // Write header if it's a new file
            if (isNewFile) {
                fileWriter.append("Timestamp,relativeTime,RPM,IsTestRunning");
            }
            logCallback.accept("CSV file is ready for logging.");
        } catch (IOException e) {
            LOGGER.severe("Error opening CSV file: " + e.getMessage());
            e.printStackTrace();
            logCallback.accept("Error opening CSV file");
        }
    }

    public void stop() {
        // Close the file writer when the logger is no longer needed
        if (fileWriter != null) {
            try {
                fileWriter.flush(); // Ensure all data is written to the file
                fileWriter.close();
                logCallback.accept("CSV file closed successfully.");
            } catch (IOException e) {
                LOGGER.severe("Error closing CSV file: " + e.getMessage());
                e.printStackTrace();
                logCallback.accept("Error closing CSV file");
            }
        }
    }

    public void logRpmData(int rpm, boolean isTestRunning) {
        //ensure the file is open
        if (fileWriter == null) {
            logCallback.accept("CSV file is not initialized.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        long secondsElapsed = java.time.Duration.between(firstTimestampDateTime.withNano(0), now.withNano(0)).getSeconds();
        String relativeTime = String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60);
        // Check if the data is the same as the last logged data
        if (timestamp.equals(lastTimestamp)) {
            logCallback.accept("Data in the same timestamp, skipping log.");
            // No change, skip logging
            return;
        }
        try {
            // Check if the last Timestamp corresponds to the actual timestamp -1 second, if not, fill the gap in the log adding 1 to the last timestamp until the current timestamp
            if (lastRpm != -1) {
                LocalDateTime lastTime = lastTimestampDateTime;
                while (lastTime.isBefore(now.minusSeconds(1).withNano(0))) {
                    lastTime = lastTime.plusSeconds(1);
                    String filledTimestamp = lastTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    fileWriter.append("\n" + filledTimestamp + "," + relativeTime + "," + lastRpm + "," + lastIsTestRunning);
                    logCallback.accept("Filled gap in log: " + filledTimestamp + "," + relativeTime + "," + lastRpm + "," + lastIsTestRunning);
                }
            } 
            // Write the new data to the CSV file
            fileWriter.append("\n" + timestamp + "," + relativeTime + "," + rpm + "," + isTestRunning);
            lastRpm = rpm;
            lastTimestamp = timestamp;
            lastTimestampDateTime = now;
            lastIsTestRunning = isTestRunning;
            LOGGER.info("Logged data: " + timestamp + "," + relativeTime + "," + rpm + "," + isTestRunning);
        } catch (IOException e) {
            LOGGER.severe("Error writing to CSV: " + e.getMessage());
            e.printStackTrace();
            logCallback.accept("Error writing to CSV file");
        }
    }

    public boolean isFileOpen() {
        return fileWriter != null;
    }
}



                    