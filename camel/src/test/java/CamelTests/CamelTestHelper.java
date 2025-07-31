package CamelTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.ibm.as400.access.AS400SecurityException;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;

import java.beans.PropertyVetoException;
import java.util.*;

public abstract class CamelTestHelper extends CamelTestSupport {
    protected LinkedList<String> destinations = new LinkedList<>();
    protected Map<String, String> dataMapInjections = new HashMap<>();
    protected Map<String, String> componentOptions = new HashMap<>();

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

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        final AS400 as400 = ApplicationConfig.get().getRemoteConnection();
        as400.setGuiAvailable(false);
        as400.validateSignon();

        final AS400JDBCDataSource dataSource = new AS400JDBCDataSource(as400);
        dataSource.setTransactionIsolation("none");

        // Register the dataSource with the correct Camel registry
        context.getRegistry().bind("jt400", dataSource);

        return context;
    }
}
