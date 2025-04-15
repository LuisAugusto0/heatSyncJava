package com.heatsync.util;

import java.util.ArrayList;
import java.util.List;

public class PairedList<A, B> {
    public final List<A> first;
    public final List<B> second;

    public PairedList() {
        this.first = new ArrayList<>();
        this.second = new ArrayList<>();
    }

    public PairedList(List<A> first, List<B> second) {
        this.first = first;
        this.second = second;
    }

    public static <A,B> PairedList<A,B> asListPair(List<A> first, List<B> second) {
        return new PairedList<A, B>(first, second);
    }


    // Although clearer, those options are less optimized as they involve class creations
    // For more optimized code, use access first and second directly
    public Pair<A, B> get(int i) throws ArrayIndexOutOfBoundsException {
        return new Pair<A, B>(first.get(i), second.get(i));
    }

    public void add(Pair<A, B> val) {
        first.add(val.first);
        second.add(val.second);
    }

    public void add(A first, B second) {
        this.first.add(first);
        this.second.add(second);
    }
}


