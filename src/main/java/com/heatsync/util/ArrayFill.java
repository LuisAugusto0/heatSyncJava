package com.heatsync.util;
import java.util.Arrays;

public class ArrayFill {
    public static boolean[] createFilledArray(int size, boolean value) {
        boolean[] arr = new boolean[size];
        Arrays.fill(arr, value);
        return arr;
    }
}


