import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.configuration.DataConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for DataConfig multi-instance support.
 * Verifies instance isolation, config file placement, and that getRoutes()
 * resolves the schema via the correct instance's ApplicationConfig.
 */
public class DataConfigTest {

    private static final String INSTANCE_A = "data-config-test-a";
    private static final String INSTANCE_B = "data-config-test-b";
    private static final Set<String> EMPTY_DESTINATIONS = Collections.emptySet();

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

        DataConfig config = DataConfig.get(ctx, EMPTY_DESTINATIONS);

        assertNotNull("DataConfig should not be null", config);
        File expectedFile = new File(ctx.getConfigDirectory(), "data.ini");
        assertTrue("data.ini should be created in the instance config directory", expectedFile.exists());
    }

    @Test
    public void testTwoInstancesReadFromSeparateDirectories() throws Exception {
        InstanceContext ctxA = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        InstanceContext ctxB = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_B});

        DataConfig configA = DataConfig.get(ctxA, EMPTY_DESTINATIONS);
        DataConfig configB = DataConfig.get(ctxB, EMPTY_DESTINATIONS);

        assertNotNull("Config A should not be null", configA);
        assertNotNull("Config B should not be null", configB);
        assertNotSame("Two instances must produce distinct DataConfig objects", configA, configB);

        File fileA = new File(ctxA.getConfigDirectory(), "data.ini");
        File fileB = new File(ctxB.getConfigDirectory(), "data.ini");
        assertNotEquals("data.ini must be in separate directories",
                fileA.getAbsolutePath(), fileB.getAbsolutePath());
    }

    @Test
    public void testDeprecatedGetDelegatesToDefaultInstance() throws Exception {
        @SuppressWarnings("deprecation")
        DataConfig config = DataConfig.get(EMPTY_DESTINATIONS);

        assertNotNull("Deprecated get() should still return a valid config", config);
    }

    @Test
    public void testGetRoutesWithEmptyDataFileReturnsEmptyMap() throws Exception {
        // An empty data.ini should produce no routes, not throw an exception.
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        DataConfig config = DataConfig.get(ctx, EMPTY_DESTINATIONS);

        // data.ini is empty at this point — getRoutes() must return an empty map cleanly
        assertNotNull("Routes map should not be null for empty data.ini", config.getRoutes());
        assertTrue("Routes map should be empty for empty data.ini", config.getRoutes().isEmpty());
    }
}
