package edu.main.dev.modbus;

import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

public class ModBusClient {

    private String endpoint;
    private PlcDriverManager driverManager;

    public ModBusClient(@NotNull PlcDriverManager driverManager, @NotNull final String endpoint) {
        this.driverManager = driverManager;
        this.endpoint = endpoint;
    }

    /**
     * Creates a new PLC device connection operation.
     *
     */
    public CompletableFuture<PlcConnection> newClient(@Nullable final String unitId, final int requestTimeout)
            throws URISyntaxException {

        CompletableFuture<PlcConnection> connectionFuture = new CompletableFuture<>();
        PlcConnection plcConnection;

        try {

            if (unitId != null)
                appendParamenter(this.endpoint, "unit-identifier", unitId);

            if (requestTimeout != 0)
                appendParamenter(this.endpoint, "request-timeout", String.valueOf(requestTimeout));

            plcConnection = driverManager.getConnection(this.endpoint);

            // Check if this connection support reading of data.
            if (!plcConnection.getMetadata().canRead())
                connectionFuture.completeExceptionally(new Throwable("This PLC device doesn't support reading request"));

            connectionFuture.complete(plcConnection);

        } catch (PlcConnectionException e) {
            connectionFuture.completeExceptionally(new Throwable("This connection not performed", e));
        }

        return connectionFuture;
    }

    private void appendParamenter(final String uri, final String key, final String value) throws URISyntaxException {

        String newParameter = key + "=" + value;

        URI newUri = new URI(uri);
        String queryParameter = newUri.getQuery() == null ? newParameter : newUri.getQuery() + "&" + newParameter;

        this.endpoint = new URI(newUri.getScheme(), newUri.getAuthority(), newUri.getPath(), queryParameter,
                newUri.getFragment()).toString();
    }

}