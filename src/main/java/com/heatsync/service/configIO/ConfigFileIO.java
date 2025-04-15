package com.heatsync.service.configIO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.heatsync.util.PairedList;

public class ConfigFileIO {
   /* Simple write of all pairs on file 
     *
     * @exceptions:
     * If the write was not allowed for some reason, fails with IOException 
     * 
     * @warning: 
     * No safeguarding for incorrect {op : value} pair - Invalid configs can be written
     */
    // 
    public static void writeSettingsFile(File file, PairedList<String, String> pairs) throws IOException, ConfigIOException {
        List<String> opList = pairs.first;
        List<String> valList = pairs.second;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < opList.size(); i++) {
                writer.write(opList.get(i) + ": " + valList.get(i) + System.lineSeparator());
                System.out.println(opList.get(i) + ": " + valList.get(i) + System.lineSeparator());
            }
        }
    }


    /* Read values as pairs of {op : val}
     * Ignore empty lines
     * Ignore '#' char and everything to the right of it
     * 
     * 
     * Each operator is associated with a expected type. The argument operatorTypes defines which one
     * Garantees both lists returned are of same size
     * 
     * @exceptions:
     * If the read was not allowed for some reason, fails with IOException
     * If a non empty line, excluding comments and whitespaces, as defined by String.trim(), has
     * any existing stream of characters with no ':' separation, fails with SettingsSaverException
     */
    protected static PairedList<String,String> readKeyValueAssociations(FileReader fileReader) 
    throws IOException, ConfigIOException {
        String line;
        BufferedReader reader = new BufferedReader(fileReader);
        
        PairedList<String, String> map = new PairedList<>();

        int lineIndex = 1;
        while ((line = reader.readLine()) != null) {


            // Remove rightmost comments
            int commentIndex = line.indexOf("#");
            if (commentIndex != -1 ) line = line.substring(0, commentIndex);
            line = line.trim();
            if (line.equals("")) continue;
            
            // Separate {op : arg}
            int index = line.indexOf(":");
            if (index == -1) {
                throw new ConfigIOException(
                    "No delimiter found for file line " + lineIndex + " of settings config file: " 
                    + System.lineSeparator() + line
                );
            }


            String op = line.substring(0,index);

            index++; // Skip spaces after the delimiter
            while (line.charAt(index) == ' ') index++;
            String arg = line.substring(index);



            map.add(op, arg);
            lineIndex++;
        }
        return map;
    }


    protected static boolean isString(String str) {
        if (str.charAt(0) != '"') return false;

        boolean isValidString = true;
        for (int i = 1; i < str.length()-1; i++) {
            if (str.charAt(i) == '/') i++; //special delimeter; skip
            else if (str.charAt(i) == '"') {
                isValidString = false;
                i = str.length();
            }
        }
        return isValidString;
    }
}
