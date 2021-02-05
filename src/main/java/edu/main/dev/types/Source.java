package edu.main.dev.types;

import java.util.List;

public class Source {

    private final String name;
    private final String endpoint;

    public Source(String endpoint, List<Request> requests) {
        this.name= "Modbus TCP";
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }

    public String name() { return name; }
}
