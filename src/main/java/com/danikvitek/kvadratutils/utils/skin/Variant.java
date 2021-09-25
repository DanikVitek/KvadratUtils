package com.danikvitek.kvadratutils.utils.skin;

public enum Variant {
    AUTO(""),
    CLASSIC("classic"),
    SLIM("slim");

    private final String name;

    Variant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
