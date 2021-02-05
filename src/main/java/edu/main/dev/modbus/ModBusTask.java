package edu.main.dev.modbus;

import com.google.common.annotations.VisibleForTesting;
import com.sun.org.apache.bcel.internal.generic.LOOKUPSWITCH;
import edu.main.dev.output.OutputWriter;
import edu.main.dev.types.ModBusConfig;
import edu.main.dev.types.Request;
import edu.main.dev.types.Source;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ModBusTask implements Runnable, AutoCloseable {

    private final Source source;
    private final Request request;
    private final ModBusConfig modBusConfig;
    private final OutputWriter outputWriter;
    private final ScheduledExecutorService executor;

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<PlcConnection> clientReference = new AtomicReference<>();
    private final AtomicReference<Future<?>> futureReference = new AtomicReference<>();
    private long attemptToRetries = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ModBusTask.class);

    public ModBusTask(Source source, Request request, @Nullable ModBusConfig modBusConfig, OutputWriter outputWriter,
                      ScheduledExecutorService executor) {
        this.source = source;
        this.request = request;
        this.modBusConfig = modBusConfig;
        this.outputWriter = outputWriter;
        this.executor = executor;
    }

    private Map<String, ModBusConfig.MemoryArea> getPlcConfig(@Nullable  ModBusConfig modBusConfig) {

        if (modBusConfig == null)
            return Collections.emptyMap();

        if (modBusConfig.getControllers() == null)
            return Collections.emptyMap();

        return modBusConfig.getControllers().get(request.object());
    }

    @Override
    public void close() {
        closed.set(true);
        closeReferences();
    }

    private void closeReferences() {
        // Thread-safe
        PlcConnection client = clientReference.getAndSet(null);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOGGER.warn("Cannot close the connection", e);
            }
        }

        // Thread-safe
        Future<?> future = futureReference.getAndSet(null);
        if (future != null)
            future.cancel(false);
    }

    @Override
    public void run() {

        LOGGER.info("Initialising ModBus client...");

        try {
            final PlcDriverManager driverManager = new PlcDriverManager();
            final ModBusClient modBusClient = new ModBusClient(driverManager, source.endpoint());

            modBusClient
                    .newClient(request.uidLog(), request.revisionWaitingTime())
                    .whenComplete((client, error) -> {
                        if (client != null) {

                            LOGGER.info("Successfully established connection at {}", source.endpoint());
                            clientReference.set(client);

                            if (closed.get())
                                try {
                                    client.close();
                                } catch (Exception e) {
                                    LOGGER.warn("Cannot close the connection", e);
                                }
                        }
                    })
                    .thenAccept(this::schedule)
                    .whenComplete((o, e) -> {
                        if (e != null) {

                            long exponentialBackOffTimer = ((long) Math.pow(2.0, ++attemptToRetries));

                            LOGGER.warn("Could not perform request for source {} at {}. A new request will be attempted" +
                                    " in {} seconds", source.name(), source.endpoint(), exponentialBackOffTimer, e);

                            Future<?> future = executor.schedule(this, exponentialBackOffTimer, TimeUnit.SECONDS);
                            futureReference.set(future);

                            if (closed.get())
                                future.cancel(false);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn("Could not perform request for source {} at {}", source.name(), source.endpoint());
            LOGGER.info("Stacktrace", e);
        }
    }

    private synchronized void schedule(PlcConnection plcConnection) {

        if (closed.get() || !plcConnection.isConnected())
            return;

        long initialDelay = mod(request.hashCode(), request.queryPeriod());
        assert initialDelay >= 0;

        ScheduledFuture<?> future = schedule(plcConnection, initialDelay, request.queryPeriod());
        futureReference.set(future);

        if (closed.get())
            future.cancel(false);

    }

    private ScheduledFuture<?> schedule(PlcConnection plcConnection, long delaySeconds, int periodSeconds) {

        return executor.scheduleAtFixedRate(() -> {
            try {
                performRequest(plcConnection);
            } catch (Throwable e) {
                LOGGER.warn("Fail to execute request ", e);
            }
        }, delaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    private void performRequest(PlcConnection plcConnection) {

        final ModBusOutput modBusOutput = new ModBusOutput(outputWriter, getPlcConfig(modBusConfig));

        final Map<String, ModBusConfig.MemoryArea> plcConfig = getPlcConfig(modBusConfig);
        final PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();

        plcConfig.forEach((plcFieldName, memoryArea) -> {

            // Acceptable values:
            // coil:5
            // coil:5[3]
            boolean hasCount = memoryArea.getCount() != null && memoryArea.getCount() > 0;
            String fieldQuery = String.format("%s:%s", plcFieldName, memoryArea.getStart_address());
            if (hasCount)
                fieldQuery = String.format("%s[%s]", fieldQuery, memoryArea.getCount());

            System.out.println("fieldQuery=>" + fieldQuery);

            builder.addItem(plcFieldName, fieldQuery);

        });

        try {

            // The api should cancel this future, after the time was set by query parameter "request-timeout", but
            // this does not happen. Because of this the timeout is forced here.
            long timeout = (request.revisionWaitingTime() == 0) ? 1_000 : request.revisionWaitingTime();
            PlcReadResponse response = builder.build().execute().get(timeout, TimeUnit.MILLISECONDS);

            if (response != null) {
                long publishTimestamp = System.currentTimeMillis();
                modBusOutput.writeValues(response, publishTimestamp);
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            closeReferences();
            LOGGER.warn("Fail to execute request. A new request will be attempted in 3 minutes...", e);
            executor.schedule(this, 30, TimeUnit.SECONDS);
        }

    }

    @VisibleForTesting
    static long mod(long x, long y) {
        assert y > 0;
        return ((x % y) + y) % y;
    }

}