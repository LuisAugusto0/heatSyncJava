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


public class FanProfileConfigIO {



    public enum Operators {
        // The maximum value of code is associated with the size of many arrays
        // It must not be unecessarily a big integer for no reason
        MaxCpu(0, "maxCpu"),
        MinCpu(1, "minCpu"),
        MaxGpu(2, "maxGpu"),
        MinGpu(3, "minGpu"),
        MaxSpeed(4, "maxSpeed"),
        MinSpeed(5, "minSpeed"),
        CurveGrowthConstant(6, "curveGrowthConstant"),
        MacAddress(7, "macAddress");
    
        private final int code;
        private final String label;


        static final Map<String, Operators> map = new HashMap<>(Map.ofEntries(
            Map.entry(MaxCpu.label, MaxCpu),
            Map.entry(MinCpu.label, MinCpu),
            Map.entry(MaxGpu.label, MaxGpu),
            Map.entry(MinGpu.label, MinGpu),
            Map.entry(MaxSpeed.label, MaxSpeed),
            Map.entry(MinSpeed.label, MinSpeed),
            Map.entry(CurveGrowthConstant.label, CurveGrowthConstant),
            Map.entry(MacAddress.label, MacAddress)
        ));

        // Max code value. An array on the size of this variable can fit
        // all elements of the enum safely
        public static final int MAX_LIST_SIZE = computeMaxCode()+1;


        private static int computeMaxCode() {
            int max = -1;
            for (Operators op : values()) {
                if (op.code > max) {
                    max = op.code;
                }
            }
            return max;
        }
    
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
        public Integer maxCpu = null, minCpu = null, maxGpu = null, minGpu = null, minSpeed = null, maxSpeed = null;
        public Double curveGrowthConstant = null;
        public String macAddress = null;


        // Table for checking if a operator was assigned
        protected boolean[] valuesNull = ArrayFill.createFilledArray(Operators.MAX_LIST_SIZE, true);


        // Null constructor
        public Response() {}


        public boolean isValueNull(Operators code) { return valuesNull[code.getCode()]; }

        public boolean existsNullValue() {
            for (Operators op : Operators.values()) {
                if (valuesNull[op.code]) {
                    return true;
                }
            }
            return false;
        }

        public List<String> getNullValuesName() {
            List<String> list = new ArrayList<>();
            for (Operators op : Operators.values()) {
                if (valuesNull[op.code]) {
                    list.add(op.label);
                }
            }
            return list;
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

        public void setMacAddress(String s) { 
            valuesNull[Operators.MacAddress.getCode()] = false;
            macAddress = s;
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

                case MacAddress:
                    s = macAddress;
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Operators.MaxCpu.getLabel() + " = {" + maxCpu + "}").append(System.lineSeparator());
            sb.append(Operators.MinCpu.getLabel() + " = {" + minCpu + "}").append(System.lineSeparator());
            sb.append(Operators.MaxGpu.getLabel() + " = {" + maxGpu + "}").append(System.lineSeparator());
            sb.append(Operators.MinGpu.getLabel() + " = {" + minGpu + "}").append(System.lineSeparator());
            sb.append(Operators.MaxSpeed.getLabel() + " = {" + maxSpeed + "}").append(System.lineSeparator());
            sb.append(Operators.MinSpeed.getLabel() + " = {" + minSpeed + "}").append(System.lineSeparator());
            sb.append(Operators.CurveGrowthConstant.getLabel() + " = {" + curveGrowthConstant + "}").append(System.lineSeparator());
            sb.append(Operators.MacAddress.getLabel() + " = {" + macAddress + "}");
            return sb.toString();
        }

    }
    



    public static Response readSettingsFile(FileReader fileReader) 
    throws IOException, ConfigIOException {
        PairedList<String,String> lists = ConfigFileIO.readKeyValueAssociations(fileReader);
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

                case MacAddress:
                    String val = values.get(i);
                    if (val.isEmpty() && val.length() != 12) {
                        errorTrace.append(
                            "Mac address {" + op.label + "} has non valid length argument {" + val + "} for file line " +
                            i+1 + System.lineSeparator()
                        );
                    } else {
                        response.setMacAddress(val);
                    }
                    break;
            }
        }

        String errorString = errorTrace.toString();
        if (!errorString.isEmpty()) throw new ConfigIOException(errorString);
        
        return response;
    }


    public static void writeConfig(File file, PairedList<String, String> config) throws IOException {
        ConfigFileIO.writeSettingsFile(file, config);
    }

    public static void writeConfig(File file, Response response) throws IOException {
        ConfigFileIO.writeSettingsFile(file, response.toPairs());
    }
}




