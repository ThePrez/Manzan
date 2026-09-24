import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.configuration.DestinationConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit tests for DestinationConfig multi-instance support.
 * Verifies that each InstanceContext produces an independent DestinationConfig
 * reading from the correct instance config directory.
 */
public class DestinationConfigTest {

    private static final String INSTANCE_A = "dest-config-test-a";
    private static final String INSTANCE_B = "dest-config-test-b";

    @Before
    public void setUp() {
        cleanupTestDirectories();
    }

    @After
    public void tearDown() {
        cleanupTestDirectories();
    }

    private void cleanupTestDirectories() {
        for (String name : new String[]{INSTANCE_A, INSTANCE_B, "default"}) {
            File dir = new File("/QOpenSys/etc/manzan-" + name);
            if (dir.exists()) {
                deleteDirectory(dir);
            }
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) deleteDirectory(f);
            }
        }
        dir.delete();
    }

    @Test
    public void testGetWithContextCreatesConfigFile() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});

        DestinationConfig config = DestinationConfig.get(ctx);

        assertNotNull("DestinationConfig should not be null", config);
        File expectedFile = new File(ctx.getConfigDirectory(), "dests.ini");
        assertTrue("dests.ini should be created in the instance config directory", expectedFile.exists());
    }

    @Test
    public void testTwoInstancesReadFromSeparateDirectories() throws Exception {
        InstanceContext ctxA = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        InstanceContext ctxB = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_B});

        DestinationConfig configA = DestinationConfig.get(ctxA);
        DestinationConfig configB = DestinationConfig.get(ctxB);

        assertNotNull("Config A should not be null", configA);
        assertNotNull("Config B should not be null", configB);
        assertNotSame("Two instances must produce distinct DestinationConfig objects", configA, configB);

        File fileA = new File(ctxA.getConfigDirectory(), "dests.ini");
        File fileB = new File(ctxB.getConfigDirectory(), "dests.ini");
        assertNotEquals("dests.ini must be in separate directories",
                fileA.getAbsolutePath(), fileB.getAbsolutePath());
    }

    @Test
    public void testDeprecatedGetDelegatesToDefaultInstance() throws Exception {
        @SuppressWarnings("deprecation")
        DestinationConfig config = DestinationConfig.get();

        assertNotNull("Deprecated get() should still return a valid config", config);
    }
}
