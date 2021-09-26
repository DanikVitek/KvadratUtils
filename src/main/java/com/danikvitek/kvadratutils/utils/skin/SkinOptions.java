package com.danikvitek.kvadratutils.utils.skin;

public class SkinOptions {

    private static final String URL_FORMAT = "name=%s&model=%s&visibility=%s";

    private final String name;
    private final Variant variant;
    private final Visibility visibility;

    public SkinOptions(String name, Variant variant, Visibility visibility) {
        this.name = name;
        this.variant = variant;
        this.visibility = visibility;
    }

    @Deprecated
    public String toUrlParam() {
        return String.format(URL_FORMAT, this.name, this.variant.getName(), this.visibility.getCode());
    }
}