

import com.github.theprez.manzan.routes.dest.StreamDestination;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.routes.event.FileEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class CamelTests extends CamelTestSupport {

    final String filePathString = "/Users/zakjonat/test.txt";
    final String testOutDest = "test_out";

    @Override
    protected void doPreSetup() throws Exception {
        // Ensure the file exists and is empty
        Path filePath = Paths.get(filePathString);
        Files.write(filePath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public String isMockEndpoints() {
        String[] mockEndpoints = new String[]{"direct:" + testOutDest};
        return String.join(",", mockEndpoints);
    }

    @Test
    public void testMock() throws Exception {
        final String textContent = "Hello World";
        Path path = Paths.get(filePathString);
        String fileName = path.getFileName().toString();
        String expectedTextContent = "{\n" +
                "  \"FILE_NAME\" : \"" + fileName + "\",\n" +
                "  \"FILE_PATH\" : \"" + filePathString + "\",\n" +
                "  \"FILE_DATA\" : \"" + textContent + "\"\n" +
                "}";

        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePathString))) {
            writer.write(textContent);
        }
        mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds

        mockOut.expectedMessageCount(1);
        mockOut.expectedBodiesReceived(expectedTextContent);
        mockOut.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final String fileEventRouteName = "fileRoute";
        LinkedList<String> destinations = new LinkedList<>();
        Map<String, String> dataMapInjections = new HashMap<>();
        Map<String, String> componentOptions = new HashMap<>();

        destinations.add(testOutDest);
        return new RoutesBuilder[]{
                new FileEvent(fileEventRouteName, filePathString, null, destinations, null, pollInterval, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }

}

