import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.WatchStarter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit tests for WatchStarter multi-instance support.
 *
 * These tests verify construction and session ID generation only — actual
 * STRWCH/ENDWCH execution requires a live IBM i connection and is covered by
 * integration tests.
 */
public class WatchStarterTest {

    private static final String INSTANCE_A = "alpha-instance";
    private static final String INSTANCE_B = "bravo-instance";

    // Minimal valid STRWCH command string (no SSNID or WCHPGM — those are injected)
    private static final String STRWCH_CMD =
            "STRWCH WCHMSGQ((QSYSOPR *FIRST)) WCHMSGS((*SEVER 00 *ESCAPE))";

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
    public void testConstructionWithContextGeneratesSessionId() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});

        // Construction succeeds and produces a WatchStarter — session ID is generated
        // internally from the context; no IBM i connection is made at construction time.
        WatchStarter ws = new WatchStarter(ctx, "mysession", STRWCH_CMD);
        assertNotNull("WatchStarter should be constructed successfully", ws);
    }

    @Test
    public void testTwoInstancesProduceDifferentSessionIds() throws Exception {
        InstanceContext ctxA = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});
        InstanceContext ctxB = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_B});

        // Session IDs are generated inside WatchStarter, so we verify the prefix logic
        // via InstanceContext directly (WatchStarter does not expose the session ID).
        String prefixA = ctxA.getSessionIdPrefix();
        String prefixB = ctxB.getSessionIdPrefix();

        assertNotEquals("Different instances must produce different session ID prefixes",
                prefixA, prefixB);
    }

    @Test
    public void testSessionIdIsExactlyTenCharacters() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});

        String sessionId = ctx.generateSessionId("mysession");
        assertEquals("Session ID must be exactly 10 characters for IBM i SSNID", 10, sessionId.length());
    }

    @Test
    public void testSessionIdStartsWithInstancePrefix() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[]{"--instance=" + INSTANCE_A});

        String sessionId = ctx.generateSessionId("mysession");
        assertTrue("Session ID must start with the instance prefix",
                sessionId.startsWith(ctx.getSessionIdPrefix()));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedConstructorDelegatesToDefaultInstance() throws Exception {
        // The deprecated two-arg constructor must still be accepted without throwing.
        // It delegates to InstanceContext.getDefault() internally.
        // We only test construction — not strwch() — to avoid needing a live connection.
        WatchStarter ws = new WatchStarter("TESTSID", STRWCH_CMD);
        assertNotNull("Deprecated WatchStarter constructor should succeed", ws);
    }
}
