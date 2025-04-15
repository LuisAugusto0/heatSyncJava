package com.heatsync.service.configIO;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.heatsync.util.ArrayFill;
import com.heatsync.util.PairedList;


public class FanProfileConfigIO extends ConfigFileIO {

    final static public String maxCpuString = Operators.MaxCpu.label;
    final static public String minCpuString = Operators.MinCpu.label;
    final static public String maxGpuString = Operators.MaxGpu.label;
    final static public String minGpuString = Operators.MinGpu.label;
    final static public String maxSpeedString = Operators.MaxSpeed.label;
    final static public String minSpeedString = Operators.MinSpeed.label;
    final static public String curveGrowthConstantString = Operators.CurveGrowthConstant.label;

    final static PairedList<String, String> defaults = PairedList.asListPair(
        Arrays.asList(
            maxCpuString, 
            minCpuString, 
            maxGpuString, 
            minGpuString,
            maxSpeedString,
            minSpeedString,
            curveGrowthConstantString
        ), 
        Arrays.asList(
            "10", "1", "50", "2", "10", "100", "4.4"
        )
    );

    

    public enum Operators {
        // "enum mapped to pair of values"
        MaxCpu(0, "maxCpu"),
        MinCpu(1, "minCpu"),
        MaxGpu(2, "maxGpu"),
        MinGpu(3, "minGpu"),
        MaxSpeed(4, "maxSpeed"),
        MinSpeed(5, "minSpeed"),
        CurveGrowthConstant(6, "curveGrowthConstant");
    
        private final int code;
        private final String label;

        static final Map<String, Operators> map = new HashMap<>(Map.ofEntries(
            Map.entry(MaxCpu.label, Operators.MaxCpu),
            Map.entry(MinCpu.label, Operators.MinCpu),
            Map.entry(MaxGpu.label, Operators.MaxGpu),
            Map.entry(MinGpu.label, Operators.MinGpu),
            Map.entry(MaxSpeed.label, Operators.MaxSpeed),
            Map.entry(MinSpeed.label, Operators.MinSpeed),
            Map.entry(CurveGrowthConstant.label, Operators.CurveGrowthConstant)
        ));
    
        Operators(int code, String label) {
            this.code = code;
            this.label = label;
        }
    
        public int getCode() {
            return code;
        }
    
        public String getLabel() {
            return label;
        }
    
        public static Operators getOperator(String s) {
            return map.get(s);
        }
    
        @Override
        public String toString() {
            return label;
        }
    }
    

    public static class Response {
        public int maxCpu = Integer.MIN_VALUE, minCpu =  Integer.MIN_VALUE, maxGpu = Integer.MIN_VALUE, 
        minGpu = Integer.MIN_VALUE, minSpeed = Integer.MIN_VALUE, maxSpeed = Integer.MIN_VALUE;

        public double curveGrowthConstant = Double.MIN_VALUE;
        protected boolean[] valuesNull = ArrayFill.createFilledArray(Operators.values().length, true);

        public boolean isValueNull(Operators code) { return valuesNull[code.getCode()]; }
        public boolean isMaxCpuNull() { return valuesNull[Operators.MaxCpu.getCode()]; }
        public boolean isMinCpuNull() { return valuesNull[Operators.MinCpu.getCode()]; }
        public boolean isMaxGpuNull() { return valuesNull[Operators.MaxGpu.getCode()]; }
        public boolean isMinGpuNull() { return valuesNull[Operators.MinGpu.getCode()]; }
        public boolean isMaxSpeedNull() { return valuesNull[Operators.MaxSpeed.getCode()]; }
        public boolean isMinSpeedNull() { return valuesNull[Operators.MinSpeed.getCode()]; }
        public boolean isCurveGrowthCOnstantNull() { return valuesNull[Operators.CurveGrowthConstant.getCode()]; }

        public boolean noNullValues() {
            boolean res = true;
            if (isMaxCpuNull()) res = false;
            else if (isMinCpuNull()) res = false;
            else if (isMaxGpuNull()) res = false;
            else if (isMinGpuNull()) res = false;
            else if (isMaxSpeedNull()) res = false;
            else if (isMinSpeedNull()) res = false;
            else if (isCurveGrowthCOnstantNull()) res = false;
            
            return res;
        }

        public void setMaxCpu(int i) { 
            valuesNull[Operators.MaxCpu.getCode()] = false;
            maxCpu = i;
        }

        public void setMinCpu(int i) { 
            valuesNull[Operators.MinCpu.getCode()] = false;
            minCpu = i;
        }

        public void setMaxGpu(int i) { 
            valuesNull[Operators.MaxGpu.getCode()] = false;
            maxGpu = i;
        }

        public void setMinGpu(int i) { 
            valuesNull[Operators.MinGpu.getCode()] = false;
            minGpu = i;
        }

        public void setMaxSpeed(int i) { 
            valuesNull[Operators.MaxSpeed.getCode()] = false;
            maxSpeed = i;
        }

        public void setMinSpeed(int i) { 
            valuesNull[Operators.MinSpeed.getCode()] = false;
            minSpeed = i;
        }

        public void setSpeedGrowthConstant(double d) { 
            valuesNull[Operators.CurveGrowthConstant.getCode()] = false;
            curveGrowthConstant = d;
        }

        void addIntegerOperandResponse(Operators op, int val) throws ConfigIOException {
            switch (op) {
                case MaxCpu:
                    setMaxCpu(val);
                    break;
                case MinCpu:
                    setMinCpu(val);
                    break;
                case MaxGpu:
                    setMaxGpu(val);
                    break;
                case MinGpu:
                    setMinGpu(val);
                    break;
                case MaxSpeed:
                    setMaxSpeed(val);
                    break;
                case MinSpeed:
                    setMinSpeed(val);
                    break;
                default:
                    throw new ConfigIOException("Not a integer operator for: " + op.label + ". Attempt to set integer values to it");
            }
        }   

    

        public String getValueAsString(Operators op) {
            String s;
            switch(op) {
                case MaxCpu:
                    s = Integer.toString(maxCpu);
                    break;

                case MinCpu:
                    s = Integer.toString(minCpu);
                    break;

                case MaxGpu:
                    s = Integer.toString(maxGpu);
                    break;

                case MinGpu:
                    s = Integer.toString(minGpu);
                    break;

                case MaxSpeed:
                    s = Integer.toString(maxSpeed);
                    break;

                case MinSpeed:
                    s = Integer.toString(minSpeed);
                    break;

                case CurveGrowthConstant:
                    s = Double.toString(curveGrowthConstant);
                    break;

                default:
                    s = "ILLEGAL CAST EXECUTED";
                    break;
            }

            return s;
        }

        public PairedList<String, String> toPairs() {
            List<String> opList = new ArrayList<>();
            List<String> valList = new ArrayList<>();

            for (Operators op : Operators.values()) {
                opList.add(op.toString());
                valList.add(getValueAsString(op));
            }

            return new PairedList<>(opList, valList);
        }

    }
    



    public static Response readSettingsFile(FileReader fileReader) 
    throws IOException, ConfigIOException {
        PairedList<String,String> lists = readKeyValueAssociations(fileReader);
        List<String> operators = lists.first;
        List<String> values = lists.second;

        StringBuilder errorTrace = new StringBuilder();
        Response response = new Response();

        for (int i = 0 ; i < operators.size(); i++) {
            Operators op = Operators.getOperator(operators.get(i));
            if (op == null) { 
                errorTrace.append(
                    "Operator does not match any of the expected operators: {" + operators.get(i) + "} for file line " 
                    + i+1 + System.lineSeparator()
                );
                continue;
            }

            if (!response.isValueNull(op)) {
                errorTrace.append(
                    "Repeated operand {" + op.toString() + "} found on config value on line " + i+1 + System.lineSeparator()
                );
                continue;
            }  

            switch(op) {
                // Integer operands
                case MaxCpu:
                case MinCpu:
                case MaxGpu:
                case MinGpu:
                case MaxSpeed:
                case MinSpeed:
                    try {
                        int val = Integer.parseInt(values.get(i));
                        response.addIntegerOperandResponse(op, val);
                        
                    } catch (NumberFormatException e) {
                        errorTrace.append(
                            "Integer operator {" + op.label + "} has non integer argument {" + values.get(i) + "} for file line " +
                            i+1 + System.lineSeparator()
                        );
                    }
                    break;
                
                case CurveGrowthConstant:
                    try {
                        double val = Double.parseDouble(values.get(i));
                        response.setSpeedGrowthConstant(val);
                        
                    } catch (NumberFormatException e) {
                        errorTrace.append(
                            "Double operator {" + op.label + "} has non double argument {" + values.get(i) + "} for file line " +
                            i+1 + System.lineSeparator()
                        );
                    }
                    break;
            }
        }

        String errorString = errorTrace.toString();
        if (!errorString.isEmpty()) throw new ConfigIOException(errorString);
        
        return response;
    }

    public static void writeDefaultConfig(File file) throws IOException, ConfigIOException {
        writeSettingsFile(file, defaults);
    }

}




