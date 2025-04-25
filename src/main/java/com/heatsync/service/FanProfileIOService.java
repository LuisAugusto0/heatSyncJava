package com.heatsync.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.heatsync.service.configIO.ConfigIOException;
import com.heatsync.service.configIO.FanProfileConfigIO;

public class FanProfileIOService {
    static String workingDir = Paths.get("").toAbsolutePath().toString();
    static Path configPath = Paths.get(workingDir, ".config");
    static FanProfileConfigIO.Response response;

    public static void initiate() {
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath);
                System.out.println("Created: " + configPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + configPath);
                e.printStackTrace();
            }
        }
        workingDir += "/.config";
        String filename = workingDir + "/profiles.txt";
        File file = new File(filename);
        System.out.println(filename);

        try {
            if (!file.exists()) {
                System.out.println("Writing defaults");
                FanProfileConfigIO.writeDefaultConfig(file);
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }

        

        try {
            // FileWriter writer = new FileWriter(filename);
            // FanProfileConfigIO.writeSettingsFile(writer, defaults);
            // writer.close();

            FileReader reader = new FileReader(filename);
            response = FanProfileConfigIO.readSettingsFile(reader);

            System.out.println(FanProfileConfigIO.maxCpuString + " = {" + response.maxCpu + "}");
            System.out.println(FanProfileConfigIO.minCpuString + " = {" + response.minCpu + "}");
            System.out.println(FanProfileConfigIO.maxGpuString + " = {" + response.maxGpu + "}");
            System.out.println(FanProfileConfigIO.minCpuString + " = {" + response.minGpu + "}");
            System.out.println(FanProfileConfigIO.maxSpeedString + " = {" + response.maxSpeed + "}");
            System.out.println(FanProfileConfigIO.minSpeedString + " = {" + response.minSpeed + "}");
            System.out.println(FanProfileConfigIO.curveGrowthConstantString + " = {" + response.curveGrowthConstant + "}");
            System.out.println(FanProfileConfigIO.macAddressString + " = {" + response.macAddress + "}");
            reader.close();
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static int getMaxCpu() {
        return response.maxCpu;
    }
    public static int getMinCpu() {
        return response.minCpu;
    }
    public static int getMaxGpu() {
        return response.maxGpu;
    }
    public static int getMinGpu() {
        return response.minGpu;
    }
    public static int getMaxSpeed() {
        return response.maxSpeed;
    }
    public static int getMinSpeed() {
        return response.minSpeed;
    }
    public static double getCurveGrowthConstant() {
        return response.curveGrowthConstant;
    }
    public static String getMacAddress() {
        return response.macAddress;
    }

    public static void setMacAddress(String macAddress) {
        response.setMacAddress(macAddress);
        try {
            FanProfileConfigIO.writeConfig(new File(workingDir + "/profiles.txt"), response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigIOException e) {
            e.printStackTrace();
        }
    }

    public static void updateFanProfile(int maxCpu, int minCpu, int maxGpu, int minGpu, int maxSpeed, int minSpeed, double curveGrowthConstant) {
        response.setMaxCpu(maxCpu);
        response.setMinCpu(minCpu);
        response.setMaxGpu(maxGpu);
        response.setMinGpu(minGpu);
        response.setMaxSpeed(maxSpeed);
        response.setMinSpeed(minSpeed);
        response.setSpeedGrowthConstant(curveGrowthConstant);

        try {
            FanProfileConfigIO.writeConfig(new File(workingDir + "/profiles.txt"), response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigIOException e) {
            e.printStackTrace();
        }
    }
}
