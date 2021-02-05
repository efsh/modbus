package edu.main.dev.types;

import java.util.Map;

public class ModBusConfig {

    private final Map<String, Map<String, MemoryArea>> controllers;

    public ModBusConfig(String comments, Map<String, Map<String, MemoryArea>> controllers) {
        this.controllers = controllers;
    }

    public ModBusConfig(Map<String, Map<String, MemoryArea>> controllers) {
        this(null, controllers);
    }

    public Map<String, Map<String, MemoryArea>> getControllers() {
        return controllers;
    }

    public static class MemoryArea {

        final private int address;
        final private int count;
        final private String unit;

        public MemoryArea(int address, int count, String unit) {
            this.address = address;
            this.count = count;
            this.unit = unit;
        }

        public MemoryArea(int address, int count) {
            this(address, count, null);
        }

        public MemoryArea(int address) {
            this(address, 0, null);
        }

        public String getUnit() {
            return unit;
        }

        public int getStart_address() {
            return address;
        }

        public Integer getCount() {
            return count;
        }
    }
}
