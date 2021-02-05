package edu.dev;

import edu.main.dev.modbus.ModBusOutput;
import edu.main.dev.output.OutputWriter;
import edu.main.dev.types.ModBusConfig;
import edu.main.dev.types.Request;
import edu.main.dev.types.Source;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.mock.connection.MockConnection;
import org.apache.plc4x.java.mock.connection.MockDevice;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class ModBusClientTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private MockDevice mockDeviceOne;
    @Mock
    private OutputWriter outputWriter;
    @Mock
    private PlcReadResponse readResponse;
    @Mock
    private ScheduledExecutorService executor;

    private MockConnection connection;
    private PlcDriverManager driverManager;
    private static Source source;
    private static Map<String, Map<String, ModBusConfig.MemoryArea>> modBusConfig;
    private static final String DEVICE_NAME = "deviceOne";
    private static final long INITIAL_DELAY = 100;

    @BeforeClass
    public static void setUp() throws IOException {

        // get an available port
        final ServerSocket socket = new ServerSocket(0);
        final String endpoint = "mock:my-mock-connection:" + socket.getLocalPort();

        Request requestConfig = new Request(DEVICE_NAME, 500, null);
        ArrayList<Request> allRequestConfig = new ArrayList<>();
        allRequestConfig.add(requestConfig);

        source = new Source(endpoint, allRequestConfig);

        modBusConfig = new HashMap<>();
        ModBusConfig.MemoryArea item1 = new ModBusConfig.MemoryArea(1, 2, "cm");
        ModBusConfig.MemoryArea item2 = new ModBusConfig.MemoryArea(0, 3, null);
        Map<String, ModBusConfig.MemoryArea> configDevice = new HashMap<>();
        configDevice.put("holding-register", item1);
        configDevice.put("coil", item2);

        modBusConfig.put(DEVICE_NAME, configDevice);

    }

    @Before
    public void connection() throws PlcConnectionException {

        // Setup
        driverManager = new PlcDriverManager();
        connection = (MockConnection) driverManager.getConnection(source.endpoint());
        connection.setDevice(mockDeviceOne);
    }

    @After
    public void tearDown() {
        connection.close();
    }

    @SuppressWarnings("EmptyMethod")
    @Test
    public void testSimpleDevice() {

//        when(mockDeviceOne.read("holding-register")).thenReturn(new ResponseItem<>(PlcResponseCode.OK, new PlcFloat(0.5F)));
        //when(mockDeviceOne.read("coil")).thenReturn(new ResponseItem<>(PlcResponseCode.OK, new PlcBoolean(true)));

//        ModBusOutput output = new ModBusOutput(outputWriter, modBusConfig.get(DEVICE_NAME));
//        ModBusClient client = new ModBusClient(source, null, output, executor);
//        client.doConnect(INITIAL_DELAY).whenComplete((o, e) -> {
//            assertEquals(true, o);
//            if (o != null)
//                client.doRequest(INITIAL_DELAY, 100);
//        });
    }

    @Test
    public void testWhenValidResponseWriteValues() {
        ModBusOutput output = new ModBusOutput(outputWriter, modBusConfig.get(DEVICE_NAME));

        List<String> fields = Stream.of("holding-register", "coil").collect(Collectors.toList());

        when(readResponse.getFieldNames()).thenReturn(fields);
        when(readResponse.getResponseCode(anyString())).thenReturn(PlcResponseCode.OK);
        when(readResponse.getAllObjects("holding-register")).thenReturn(Stream.of(0.5).collect(Collectors.toList()));
        when(readResponse.getAllObjects("coil")).thenReturn(Stream.of(true).collect(Collectors.toList()));
        output.writeValues(readResponse, 10000L);
        verify(outputWriter, times(2)).writeRaw(anyMap(), anyLong());

    }

    @Test
    public void testWhenInvalidResponseDontWriteAnyValue() {
        ModBusOutput output = new ModBusOutput(outputWriter, modBusConfig.get(DEVICE_NAME));

        List<String> fields = Stream.of("holding-register", "coil").collect(Collectors.toList());

        when(readResponse.getFieldNames()).thenReturn(fields);
        when(readResponse.getResponseCode(anyString())).thenReturn(PlcResponseCode.NOT_FOUND);
        output.writeValues(readResponse, 10000L);
        verify(outputWriter, never()).writeRaw(anyMap(), anyLong());
    }

}