package CamelTests;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.WatchMsgEventSockets;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class WatchMsgEventSocketsTest extends CamelTestHelper {

    @EndpointInject("netty:tcp://0.0.0.0:8080?sync=false")
    ProducerTemplate socketProducer;

    @Test
    public void testSocketWatchWithoutInjectionsDoesNotThrow() throws Exception {
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(5000);
        mockOut.expectedMessageCount(1);

        socketProducer.sendBody("{\"SESSION_ID\":\"ALPGMR\",\"MESSAGE_ID\":\"CPF0001\",\"MESSAGE\":\"hello\"}");

        mockOut.assertIsSatisfied();
    }

    @Test
    public void testSocketWatchNormalizesSessionIdBeforeLookup() throws Exception {
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(5000);
        mockOut.expectedMessageCount(1);
        expectBodyToHaveKeys(mockOut, java.util.Arrays.asList("SESSION_ID", "MESSAGE_ID", "MESSAGE", "TEST_ENV"));

        socketProducer.sendBody("{\"SESSION_ID\":\" alpgmr \",\"MESSAGE_ID\":\"CPF0002\",\"MESSAGE\":\"normalized\"}");

        mockOut.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final String socketEvent = "socketWatcher";
        final Map<String, String> formatMap = new HashMap<>();
        final Map<String, String> destMap = new HashMap<>();
        final Map<String, ManzanEventType> eventMap = new HashMap<>();
        final Map<String, Map<String, String>> dataMapInjectionsMap = new HashMap<>();

        destinations = new LinkedList<>();
        destinations.add(testOutDest);

        destMap.put("ALPGMR", "direct:" + testOutDest);
        eventMap.put("ALPGMR", ManzanEventType.WATCH_MSG);

        Map<String, String> injectedValues = new HashMap<>();
        injectedValues.put("TEST_ENV", "JUNIT");
        dataMapInjectionsMap.put("ALPGMR", injectedValues);

        return new RoutesBuilder[]{
                new WatchMsgEventSockets(socketEvent, formatMap, destMap, eventMap, dataMapInjectionsMap),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}