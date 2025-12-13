package com.fulus.ai.assistant.enums;

public enum BillType {
    ELECTRICITY("Electricity Bill"),
    WATER("Water Bill"),
    AIRTIME("Airtime Top-up"),
    DATA("Data Bundle"),
    CABLE_TV("Cable TV Subscription");

    private final String displayName;

    BillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
