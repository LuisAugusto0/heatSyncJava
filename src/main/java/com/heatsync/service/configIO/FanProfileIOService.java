package com.heatsync.service.configIO;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.imageio.IIOException;

import com.heatsync.service.configIO.FanProfileConfigIO.Operators;
import com.heatsync.service.configIO.FanProfileConfigIO.Response;
import com.heatsync.util.PairedList;


final class Defaults {
    static final String[] operatorDefaults = new String[Operators.MAX_LIST_SIZE];
    
    static {
        operatorDefaults[Operators.MaxCpu.getCode()] = "85";
        operatorDefaults[Operators.MinCpu.getCode()] = "30";
        operatorDefaults[Operators.MaxGpu.getCode()] = "65";
        operatorDefaults[Operators.MinGpu.getCode()] = "30";
        operatorDefaults[Operators.MaxSpeed.getCode()] = "100";
        operatorDefaults[Operators.MinSpeed.getCode()] = "0";
        operatorDefaults[Operators.CurveGrowthConstant.getCode()] = "1";
        operatorDefaults[Operators.MacAddress.getCode()] = null;
    }


    final static PairedList<String, String> pairList = PairedList.asListPair(
        Arrays.asList(
            Operators.MaxCpu.getLabel(), 
            Operators.MinCpu.getLabel(), 
            Operators.MaxCpu.getLabel(), 
            Operators.MinGpu.getLabel(), 
            Operators.MaxSpeed.getLabel(), 
            Operators.MinSpeed.getLabel(), 
            Operators.CurveGrowthConstant.getLabel(), 
            Operators.MacAddress.getLabel()
        ), 
        Arrays.asList(
           getMaxCpu() , getMinCpu(), getMaxGpu(), getMinGpu(), getMaxSpeed(), 
           getMinSpeed(), getCurveGrowthConstant(), getMacAddress()
        )
    );

    // Hide constructor
    private Defaults() {}

    
    public static String getMaxCpu() { return operatorDefaults[Operators.MaxCpu.getCode()]; }
    public static String getMinCpu() { return operatorDefaults[Operators.MinCpu.getCode()]; }
    public static String getMaxGpu() { return operatorDefaults[Operators.MaxGpu.getCode()]; }
    public static String getMinGpu() { return operatorDefaults[Operators.MinGpu.getCode()]; }
    public static String getMaxSpeed() { return operatorDefaults[Operators.MaxSpeed.getCode()]; }
    public static String getMinSpeed() { return operatorDefaults[Operators.MinSpeed.getCode()]; }
    public static String getCurveGrowthConstant() { return operatorDefaults[Operators.CurveGrowthConstant.getCode()]; }
    public static String getMacAddress() { return operatorDefaults[Operators.MacAddress.getCode()]; }

    static void setNullValuesAsDefault(Response res) {
        if (res == null) return;

        if (res.maxCpu == null) res.maxCpu = Integer.parseInt(Defaults.getMaxCpu());
        if (res.minCpu == null) res.minCpu = Integer.parseInt(Defaults.getMinCpu());
        if (res.maxGpu == null) res.maxGpu = Integer.parseInt(Defaults.getMaxGpu());
        if (res.minGpu == null) res.minGpu = Integer.parseInt(Defaults.getMinGpu());
        if (res.maxSpeed == null) res.maxSpeed = Integer.parseInt(Defaults.getMaxSpeed());
        if (res.minSpeed == null) res.minSpeed = Integer.parseInt(Defaults.getMinSpeed());

        if (res.curveGrowthConstant == null) {
            res.curveGrowthConstant = Double.parseDouble(Defaults.getCurveGrowthConstant());
        }

        if (res.macAddress == null) res.macAddress = Defaults.getMacAddress();
    }

    static Response getDefaultResponse() {
        Response res = new Response();
        setNullValuesAsDefault(res);
        return res;
    }
}





final public class FanProfileIOService {

    
    final static String workingDir = Paths.get("").toAbsolutePath().toString();
    final static Path configPath = Paths.get(workingDir, ".config");
    final static String configFileName = "profiles.txt";
    static boolean keepStateFlag = false;
    

    static Response response = Defaults.getDefaultResponse();
    

    // Hide constructor
    private FanProfileIOService() {}


    
    public static void logValues() {
        if (response == null) {
            System.out.println("No Fan Profile Config Stored");
            return;
        }

        System.out.println(response.toString());
    }



    public static void writeDefaultConfig(File file) throws IOException, ConfigIOException {
        
        FanProfileConfigIO.writeConfig(file, Defaults.pairList);
    }

    
    public static void initiate(boolean keepStateFlag) throws ConfigIOException {
        FanProfileIOService.keepStateFlag = keepStateFlag;
        
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath);
                System.out.println("Created: " + configPath);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + configPath);
                e.printStackTrace();
            }
        }
        
        Path filename = configPath.resolve(configFileName);
        File file = filename.toFile();
        System.out.println(filename);

        try {
            if (!file.exists()) {
                System.out.println("Writing defaults");
                
                writeDefaultConfig(file);
            }
    
        } catch (Exception e) {
            System.err.println("Failed to create config file");
            e.printStackTrace();


            
        }

        
        try {
            
            FileReader reader = new FileReader(file);
            response = FanProfileConfigIO.readSettingsFile(reader);
            reader.close();

        } catch (IOException e) {
            System.err.println("Failed reading from config file. Using defaults");
            e.printStackTrace();

            response = Defaults.getDefaultResponse();
            
        } catch (ConfigIOException e) {
            System.err.println("Invalid config file");

            if (keepStateFlag == true) {
                if (response.existsNullValue()) 
                    throw new ConfigIOException("Error when reading config file: Empty value detected");
            } else {
                Defaults.setNullValuesAsDefault(response);
    
                try {
                    //UPGRADE REUSE LOGIC HERE!!
                    updateFile();
                } catch (IOException io) {
                    System.err.println("Failed to update new values on the writer");
                }
                
            }
        }   



        
        logValues();
    }



    public static int getMaxCpu() { return response.maxCpu; }
    public static int getMinCpu() { return response.minCpu; }
    public static int getMaxGpu() { return response.maxGpu; }
    public static int getMinGpu() { return response.minGpu; }
    public static int getMaxSpeed() { return response.maxSpeed; }
    public static int getMinSpeed() { return response.minSpeed; }
    public static double getCurveGrowthConstant() { return response.curveGrowthConstant; }
    public static String getMacAddress() { return response.macAddress; }


    public static void setMacAddress(String macAddress) {
        response.setMacAddress(macAddress);
        System.out.println(macAddress);
        try {
            updateFile();
        } catch (IOException e) {
            System.err.println("Warning: Can not write macAddres to config file");
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
            updateFile();
        } catch (IOException e) {
            System.err.println("Warning: Can not write fanProfiles to config file");
            e.printStackTrace();
        }

    }

    static void updateFile() throws IOException {
        FanProfileConfigIO.writeConfig(new File(workingDir + "/profiles.txt"), response);
    }
}
