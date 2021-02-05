package edu.main.dev;

import edu.main.dev.modbus.ModBusTask;
import edu.main.dev.output.DefaultOutputWriter;
import edu.main.dev.output.OutputWriter;
import edu.main.dev.types.ModBusConfig;
import edu.main.dev.types.Request;
import edu.main.dev.types.Source;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        configureLogger();

        final Source source = getSource();
        final ModBusConfig modBusConfig = getConfig();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        ScheduledExecutorService mainExecutor = Executors.newScheduledThreadPool(3);

        getRequests().forEach( request -> {
            OutputWriter output = new DefaultOutputWriter();
            Runnable modBusTask = new ModBusTask(source, request, modBusConfig, output, scheduledExecutorService);
            mainExecutor.submit(modBusTask);
        });

        try {
            //mainExecutor.shutdown();
            mainExecutor.awaitTermination(240, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private static List<Request> getRequests() {

        List<Request> requests = new LinkedList<>();
        requests.add(new Request("jacone", 5));
        requests.add(new Request("bacaxa", 6, "2", 10000));
        //requests.add(new Request("itauna", 12, "3"));

        return requests;
    }

    @NotNull
    private static Source getSource() {
        //return new Source("modbus://127.0.0.1:1552", getRequests());
        return new Source("modbus://127.0.0.1:1552", getRequests());
    }

    @NotNull
    private static ModBusConfig getConfig() {

        Map<String, Map<String, ModBusConfig.MemoryArea>> modBusConfig = new HashMap<>();

        ModBusConfig.MemoryArea m1 = new ModBusConfig.MemoryArea(0, 5, "pol");
        Map<String, ModBusConfig.MemoryArea> f1 = new HashMap<>();
        f1.put("holding-register", m1);
        modBusConfig.put("jacone", f1);

        ModBusConfig.MemoryArea m2 = new ModBusConfig.MemoryArea(1);
        Map<String, ModBusConfig.MemoryArea> f2 = new HashMap<>();
        f2.put("coil", m2);
        modBusConfig.put("bacaxa", f2);

        ModBusConfig.MemoryArea m3 = new ModBusConfig.MemoryArea(1, 2, "cm");
        ModBusConfig.MemoryArea m4 = new ModBusConfig.MemoryArea(0, 3);
        Map<String, ModBusConfig.MemoryArea> f3 = new HashMap<>();
        f3.put("holding-register", m3);
        f3.put("coil", m4);
        modBusConfig.put("itauna", f3);

        return new ModBusConfig(modBusConfig);
    }

    private static void configureLogger() {
        Logger root = Logger.getRootLogger();
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%t] %d{dd MMM yyyy HH:mm:ss,SSS} %r %-1p %c{1}: %m%n"));
        appender.setWriter(new OutputStreamWriter(System.err, Charset.defaultCharset()));
        root.removeAllAppenders();
        root.addAppender(appender);
        root.setLevel(Level.INFO);
    }
}