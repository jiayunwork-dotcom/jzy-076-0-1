package com.processeng.distill.core;

/**
 * 阶梯当前所处的塔段。
 */
public enum Section {

    RECTIFYING("精馏段"),
    STRIPPING("提馏段");

    private final String label;

    Section(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
