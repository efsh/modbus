package edu.main.dev;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class SimpleError {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static Memory memoryArea;

    public static void main(String[] args) {

        configureLogger();

//        try {
//            generateException();
//        } catch (IllegalArgumentException e) {
//            LOGGER.error("Um erro bonito: {}:{}", "teste", e);
//        }

        memoryArea = new Memory();
        String plcFieldName = "coil";

        // Acceptable values:
        // coil:5
        // coil:5[3]
        boolean hasCount = memoryArea.getCount() != null && memoryArea.getCount() > 0;
        String fieldQuery = String.format("%s:%s", plcFieldName, memoryArea.getStart_address());
        if (hasCount)
            fieldQuery = String.format("%s[%s]", fieldQuery, memoryArea.getCount());


        System.out.println(fieldQuery);

    }

    private static void generateException() {

        throw new IllegalArgumentException("Erro do argumento");
    }

    private static void configureLogger() {
        Logger root = Logger.getRootLogger();
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("[%t] %d{dd MMM yyyy HH:mm:ss,SSS} %r %-1p %c{1}: %m%n"));
        appender.setWriter(new OutputStreamWriter(System.err, Charset.defaultCharset()));
        root.removeAllAppenders();
        root.addAppender(appender);
        root.setLevel(Level.INFO);
    }

    private static class Memory {
        public Integer getCount() {
            return null;
        }

        public int getStart_address() {
            return 5;
        }
    }
}
