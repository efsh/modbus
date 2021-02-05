package edu.main.dev.output;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public interface OutputWriter {

    void writeRaw(@NotNull Map<String, Object> data, long captureStartTimestamp);

}
