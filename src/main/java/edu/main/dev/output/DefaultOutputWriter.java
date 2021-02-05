package edu.main.dev.output;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public class DefaultOutputWriter implements OutputWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOutputWriter.class);

    @Override
    public synchronized void writeRaw(@NotNull Map<String, Object> data, long captureStartTimestamp) {

        StringBuilder sb = new StringBuilder();

        data.forEach((k, v) -> {
            sb.append(k + ":");
            if (v instanceof Map)
                ((Map<?, ?>) v).forEach((key, value) -> {
                    if (value instanceof Collection)
                        //noinspection rawtypes
                        ((Collection) value).forEach( vv -> {
                            sb.append(vv.toString());
                        });
                    if (value instanceof String)
                        sb.append(value);
                });

            if (v instanceof String)
                sb.append(v);
        });

        LOGGER.info(sb.toString());

    }
}
