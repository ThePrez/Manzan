

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



    Path baseDir = Paths.get("").toAbsolutePath();  // current working dir
    Path fileDir = baseDir
            .resolve("src")
            .resolve("test")
            .resolve("testFiles");

    final String filePathNoFormatString = fileDir.resolve("test.txt").toString();
    final String filePathWithFormatString = fileDir.resolve("testFormat.txt").toString();
    final String filePathUnmatchedFilter = fileDir.resolve("testUnmatchedFilter.txt").toString();

    final String testOutDest = "test_out";

    @Override
    protected void doPreSetup() throws Exception {
        ensureFileExistsAndIsEmpty();


    }

    private void ensureFileExistsAndIsEmpty() throws Exception{
        final String[] filePaths = new String[]{filePathNoFormatString, filePathWithFormatString, filePathUnmatchedFilter};
        for (String path: filePaths){
            Path filePath = Paths.get(path);
            Files.write(filePath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Override
    public String isMockEndpoints() {
        String[] mockEndpoints = new String[]{"direct:" + testOutDest};
        return String.join(",", mockEndpoints);
    }

    @Test
    public void testFileNoFilter() throws Exception {
        final String textContent = "Hello World";
        Path path = Paths.get(filePathNoFormatString);
        String fileName = path.getFileName().toString();
        String expectedTextContent = "{\n" +
                "  \"FILE_NAME\" : \"" + fileName + "\",\n" +
                "  \"FILE_PATH\" : \"" + filePathNoFormatString + "\",\n" +
                "  \"FILE_DATA\" : \"" + textContent + "\"\n" +
                "}";

        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePathNoFormatString))) {
            writer.write(textContent);
        }
        mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds

        mockOut.expectedMessageCount(1);
        mockOut.expectedBodiesReceived(expectedTextContent);
        mockOut.assertIsSatisfied();
    }

    @Test
    public void testFileWithFilter() throws Exception {
        final String textContent = "Hello World";
        Path path = Paths.get(filePathWithFormatString);
        String fileName = path.getFileName().toString();
        String expectedTextContent = String.format("fname: %s fpath: %s fdata: %s",
                fileName, filePathWithFormatString, textContent);


        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePathWithFormatString))) {
            writer.write(textContent);
        }
        mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds

        mockOut.expectedMessageCount(1);
        mockOut.expectedBodiesReceived(expectedTextContent);
        mockOut.assertIsSatisfied();
    }

    @Test
    public void testFileWithFilterNoMatch() throws Exception {
        final String textContent = "Hello World";

        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);

        mockOut.expectedMessageCount(0);      // Expect zero messages

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePathUnmatchedFilter))) {
            writer.write(textContent);         // Write file that should NOT trigger a message
        }
        mockOut.setAssertPeriod(2000);
        mockOut.assertIsSatisfied();           // Will fail if any messages arrive within 5 seconds
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final String fileRouteNoFormat = "fileRouteNoFormat";
        final String fileRouteWithFormat = "fileRouteWithFormat";
        final String fileRouteFilterNoMatch = "fileRouteFilterNoMatch";

        final String format = "fname: $FILE_NAME$ fpath: $FILE_PATH$ fdata: $FILE_DATA$";
        final String filterMatch = "Hello";
        final String filterNoMatch = "re:^goodbye";

        LinkedList<String> destinations = new LinkedList<>();
        Map<String, String> dataMapInjections = new HashMap<>();
        Map<String, String> componentOptions = new HashMap<>();

        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new FileEvent(fileRouteNoFormat, filePathNoFormatString, null, destinations, filterMatch, pollInterval, dataMapInjections),
                new FileEvent(fileRouteWithFormat, filePathWithFormatString, format, destinations, null, pollInterval, dataMapInjections),
                new FileEvent(fileRouteFilterNoMatch, filePathUnmatchedFilter, format, destinations, filterNoMatch, pollInterval, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }

}

