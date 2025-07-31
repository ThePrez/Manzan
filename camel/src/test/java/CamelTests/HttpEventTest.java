package CamelTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.routes.event.HttpEvent;

import java.io.IOException;
import java.util.*;

public class HttpEventTest extends CamelTestSupport {

    final String testOutDest = "test_out";

    private void expectBodyToHaveKeys(MockEndpoint mockOut, List<String> keys){
        mockOut.expectedMessagesMatches(exchange -> {
            try {
                String body = exchange.getIn().getBody(String.class);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(body, Map.class);

                Set<String> expectedKeys = new HashSet<>(keys);

                return json.keySet().containsAll(expectedKeys);
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void expectBodyToStartWith(MockEndpoint mockOut, String prefix){
        mockOut.expectedMessagesMatches(exchange -> {
            try {
                String body = exchange.getIn().getBody(String.class);


                return body.startsWith(prefix);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public String isMockEndpoints() {
        String[] mockEndpoints = new String[]{"direct:" + testOutDest};
        return String.join(",", mockEndpoints);
    }

    @Test
    public void testHttpEndpoint() throws Exception {
        String prefix = "{seed=";
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds
        mockOut.expectedMessageCount(1);
        expectBodyToStartWith(mockOut, prefix);
        mockOut.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final String httpEvent = "httpEvent";
        final String url =  "https://randomuser.me/api/?results=1";

        final String format = "$info$";
        final Map<String, String> headerParams = new LinkedHashMap();
        headerParams.put("authorization", "bearer eyt");

        LinkedList<String> destinations = new LinkedList<>();
        Map<String, String> dataMapInjections = new HashMap<>();
        dataMapInjections.put("foo", "bar");
        Map<String, String> componentOptions = new HashMap<>();

        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new HttpEvent(httpEvent, url, format, destinations, null, pollInterval, headerParams, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

