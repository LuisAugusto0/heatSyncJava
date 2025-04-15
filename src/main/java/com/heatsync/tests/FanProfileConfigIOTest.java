package com.heatsync.tests;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.heatsync.service.configIO.ConfigIOException;
import com.heatsync.service.configIO.FanProfileConfigIO;
import com.heatsync.util.PairedList;


public class FanProfileConfigIOTest {

    public static void initiate() {
        String workingDir = Paths.get("").toAbsolutePath().toString();
        Path configPath = Paths.get(workingDir, ".config");


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
            FanProfileConfigIO.Response res = FanProfileConfigIO.readSettingsFile(reader);

            System.out.println(FanProfileConfigIO.maxCpuString + " = {" + res.maxCpu + "}");
            System.out.println(FanProfileConfigIO.minCpuString + " = {" + res.minCpu + "}");
            System.out.println(FanProfileConfigIO.maxGpuString + " = {" + res.maxGpu + "}");
            System.out.println(FanProfileConfigIO.minCpuString + " = {" + res.minGpu + "}");
            System.out.println(FanProfileConfigIO.maxSpeedString + " = {" + res.maxSpeed + "}");
            System.out.println(FanProfileConfigIO.minSpeedString + " = {" + res.minSpeed + "}");
            System.out.println(FanProfileConfigIO.curveGrowthConstantString + " = {" + res.curveGrowthConstant + "}");
            reader.close();
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

