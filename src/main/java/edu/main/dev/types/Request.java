package edu.main.dev.types;

public class Request {

    private final String object;
    private final int query_period;
    private final int revisionWaitingTime;
    private final String uid_log;

    public Request(String object, int query_period, String uid_log, int revisionWaitingTime) {
        this.object = object;
        this.query_period = query_period;
        this.uid_log = uid_log;
        this.revisionWaitingTime = revisionWaitingTime;
    }

    public Request(String object, int query_period, String uid_log) {
        this.object = object;
        this.query_period = query_period;
        this.uid_log = uid_log;
        this.revisionWaitingTime = 0;
    }

    public Request(String object, int query_period) {
        this.object = object;
        this.query_period = query_period;
        this.uid_log = null;
        this.revisionWaitingTime = 0;
    }

    public String object() { return object; }

    public int queryPeriod() {
        return query_period;
    }

    public int revisionWaitingTime() { return revisionWaitingTime; }

    public String uidLog() {
        return uid_log;
    }
}
