package CamelTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;

import java.beans.PropertyVetoException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class CamelTestHelper extends CamelTestSupport {
    protected void expectBodyToHaveKeys(MockEndpoint mockOut, List<String> keys) {
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

    protected void expectBodyToStartWith(MockEndpoint mockOut, String prefix) {
        mockOut.expectedMessagesMatches(exchange -> {
            try {
                String body = exchange.getIn().getBody(String.class);


                return body.startsWith(prefix);
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected boolean attemptInvalidLogin(String hostname, String username, String password) throws PropertyVetoException {
        AS400 system = new AS400(hostname, username, password);
        system.setGuiAvailable(false); // ✅ Disable GUI prompt

        try {
            system.connectService(AS400.COMMAND); // Try connecting to the COMMAND service
            System.out.println("Login unexpectedly succeeded.");
            return false;
        } catch (AS400SecurityException e) {
            System.out.println("Login failed: Invalid credentials.");
        } catch (Exception e) {
            System.out.println("Login failed: IO or connection error - " + e.getMessage());
        } finally {
            if (system.isConnected()) {
                system.disconnectAllServices();
            }
        }
        return true;
    }
}
