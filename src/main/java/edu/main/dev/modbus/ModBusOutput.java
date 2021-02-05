package edu.main.dev.modbus;

import edu.main.dev.output.OutputWriter;
import edu.main.dev.types.ModBusConfig;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModBusOutput {

    private final OutputWriter outputWriter;
    private final Map<String, ModBusConfig.MemoryArea> deviceConfig;

    public ModBusOutput(final @NotNull OutputWriter output,
                        final @NotNull Map<String, ModBusConfig.MemoryArea> deviceConfig) {
        this.outputWriter = output;
        this.deviceConfig = deviceConfig;
    }

    public Map<String, ModBusConfig.MemoryArea> getDeviceConfig() {
        return deviceConfig;
    }

    public void writeValues(final @NotNull PlcReadResponse readResponse, final long publishTimestamp) {

        final Map<String, Object> event = new LinkedHashMap<>(readResponse.getFieldNames().size());
        readResponse.getFieldNames().forEach(fieldName -> {

            if(readResponse.getResponseCode(fieldName) == PlcResponseCode.OK) {

                final Collection<Object> values = readResponse.getAllObjects(fieldName);
                final Map<String, Object> data = new LinkedHashMap<>();
                data.put("value", values);

                final String unit = getDeviceConfig().get(fieldName).getUnit();
                if (unit != null)
                    data.put("uom", unit);

                event.put(fieldName, data);

            }
            else {
                System.out.println("Error reading field [" + fieldName + "]: " + readResponse.getResponseCode(fieldName).name());
            }

        });

        outputWriter.writeRaw(event, publishTimestamp);

    }
}