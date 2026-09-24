import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.configuration.ApplicationConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;

/**
 * Unit tests for ApplicationConfig multi-instance support.
 * Verifies that each InstanceContext produces an independent ApplicationConfig
 * with no shared static state between instances.
 */
public class ApplicationConfigTest {

    private static final String INSTANCE_A = "app-config-test-a";
    private static final String INSTANCE_B = "app-config-test-b";

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
        String[] args = {"--instance=" + INSTANCE_A};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);

        ApplicationConfig config = ApplicationConfig.get(ctx);

        assertNotNull("ApplicationConfig should not be null", config);
        File expectedFile = new File(ctx.getConfigDirectory(), "app.ini");
        assertTrue("app.ini should be created in the instance config directory", expectedFile.exists());
    }

    @Test
    public void testTwoInstancesProduceIndependentConfigs() throws Exception {
        InstanceContext ctxA = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        InstanceContext ctxB = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_B});

        ApplicationConfig configA = ApplicationConfig.get(ctxA);
        ApplicationConfig configB = ApplicationConfig.get(ctxB);

        assertNotNull("Config A should not be null", configA);
        assertNotNull("Config B should not be null", configB);
        assertNotSame("Two instances must produce distinct ApplicationConfig objects", configA, configB);

        // Config files must be in separate directories
        File fileA = new File(ctxA.getConfigDirectory(), "app.ini");
        File fileB = new File(ctxB.getConfigDirectory(), "app.ini");
        assertNotEquals("Config files must be in separate directories", fileA.getAbsolutePath(), fileB.getAbsolutePath());
    }

    @Test
    public void testDeprecatedGetDelegatesToDefaultInstance() throws Exception {
        @SuppressWarnings("deprecation")
        ApplicationConfig config = ApplicationConfig.get();

        assertNotNull("Deprecated get() should still return a valid config", config);
    }

    @Test
    public void testGetLibraryDefaultsToManzan() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        ApplicationConfig config = ApplicationConfig.get(ctx);

        assertEquals("Default library name should be 'manzan'", "manzan", config.getLibrary());
    }
}
