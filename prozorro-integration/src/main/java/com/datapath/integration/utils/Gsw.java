package com.datapath.integration.utils;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public enum Gsw {

    GOODS(asList("03", "09", "14", "15", "16", "18", "19", "22", "24", "30", "31", "32", "34", "35", "37", "38", "39", "41", "42", "43", "44")),
    WORKS(singletonList("45")),
    SERVICES(emptyList());

    private List<String> codes;

    Gsw(List<String> codes) {
        this.codes = codes;
    }

    public List<String> getCodes() {
        return codes;
    }

    public static Gsw find(String code) {
        if (WORKS.getCodes().contains(code.substring(0, 2))) {
            return WORKS;
        } else if (GOODS.getCodes().contains(code.substring(0, 2))) {
            return GOODS;
        } else {
            return SERVICES;
        }
    }
}
