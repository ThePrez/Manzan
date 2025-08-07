package CamelTests;

import com.github.theprez.manzan.routes.dest.StreamDestination;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.routes.event.HttpEvent;

import java.io.IOException;
import java.util.*;

public class HttpEventTest extends CamelTestHelper {
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

        dataMapInjections.put("foo", "bar");
        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new HttpEvent(httpEvent, url, format, destinations, null, pollInterval, headerParams, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

