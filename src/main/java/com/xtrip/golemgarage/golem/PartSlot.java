package com.xtrip.golemgarage.golem;

public enum PartSlot {
    HEAD("head"),
    TORSO("torso"),
    ARMS("arms"),
    LEGS("legs"),
    CORE("core");

    private final String id;

    PartSlot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
