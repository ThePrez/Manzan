import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.LockFile;
import com.github.theprez.manzan.ManzanInstanceManager;
import com.github.theprez.manzan.ManzanInstanceManager.InstanceInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for ManzanInstanceManager.
 *
 * Tests are self-contained: they create real directories under
 * /QOpenSys/etc/manzan-mgr-* and real lock files under /var/run/manzan/,
 * and clean them up in tearDown. No IBM i connection or database is required.
 */
public class ManzanInstanceManagerTest {

    // All instance names used in this suite share this prefix so cleanup is
    // unambiguous and does not interfere with other test classes.
    private static final String PREFIX = "mgr-test-";

    // Names used across individual tests
    private static final String INST_A     = PREFIX + "a";
    private static final String INST_B     = PREFIX + "b";
    private static final String INST_C     = PREFIX + "c";
    private static final String INST_ALPHA = PREFIX + "alpha";
    private static final String INST_BETA  = PREFIX + "beta";
    private static final String INST_ZOO   = PREFIX + "zoo";

    @Before
    public void setUp() {
        cleanupAll();
    }

    @After
    public void tearDown() {
        cleanupAll();
    }

    // -------------------------------------------------------------------------
    // createInstance tests
    // -------------------------------------------------------------------------

    @Test
    public void testCreateInstanceCreatesConfigDir() throws IOException {
        InstanceContext ctx = ManzanInstanceManager.createInstance(INST_A);

        assertNotNull("createInstance must return a non-null InstanceContext", ctx);
        assertEquals("Instance name must match", INST_A, ctx.getInstanceName());

        File configDir = new File(ctx.getConfigDirectory());
        assertTrue("Config directory must exist after createInstance", configDir.isDirectory());
    }

    @Test
    public void testCreateInstanceSeedsAllThreeIniFiles() throws IOException {
        InstanceContext ctx = ManzanInstanceManager.createInstance(INST_A);
        File configDir = new File(ctx.getConfigDirectory());

        for (String name : new String[]{"app.ini", "data.ini", "dests.ini"}) {
            assertTrue(name + " must be created by createInstance",
                    new File(configDir, name).isFile());
        }
    }

    @Test
    public void testCreateInstanceSetsIniFilePermissions600() throws IOException {
        InstanceContext ctx = ManzanInstanceManager.createInstance(INST_A);
        File configDir = new File(ctx.getConfigDirectory());

        try {
            for (String name : new String[]{"app.ini", "data.ini", "dests.ini"}) {
                File f = new File(configDir, name);
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(f.toPath());
                assertTrue(name + " must be owner-readable",   perms.contains(PosixFilePermission.OWNER_READ));
                assertTrue(name + " must be owner-writable",   perms.contains(PosixFilePermission.OWNER_WRITE));
                assertFalse(name + " must not be group-readable", perms.contains(PosixFilePermission.GROUP_READ));
                assertFalse(name + " must not be world-readable", perms.contains(PosixFilePermission.OTHERS_READ));
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX dev environment (e.g. Windows) — skip permission assertions
            System.out.println("Skipping permission assertions on non-POSIX system");
        }
    }

    @Test
    public void testCreateInstanceCopiesTemplateFromDefaultInstance() throws IOException {
        // Arrange: create the default instance directory with a recognisable app.ini
        File defaultDir = new File("/QOpenSys/etc/manzan-default");
        defaultDir.mkdirs();
        File defaultAppIni = new File(defaultDir, "app.ini");
        defaultAppIni.createNewFile();
        Files.write(defaultAppIni.toPath(), "[install]\nlibrary=templatelib\n".getBytes());

        // Act: create a new instance — it should copy the default template
        InstanceContext ctx = ManzanInstanceManager.createInstance(INST_A);
        File newAppIni = new File(ctx.getConfigDirectory(), "app.ini");

        // Assert
        assertTrue("app.ini should exist in new instance directory", newAppIni.isFile());
        String content = new String(Files.readAllBytes(newAppIni.toPath()));
        assertTrue("app.ini content should be copied from the default instance template",
                content.contains("templatelib"));
    }

    @Test
    public void testCreateInstanceCreatesEmptyStubWhenNoDefault() throws IOException {
        // No default instance directory present — stubs should be created without throwing
        InstanceContext ctx = ManzanInstanceManager.createInstance(INST_A);

        for (String name : new String[]{"app.ini", "data.ini", "dests.ini"}) {
            File f = new File(ctx.getConfigDirectory(), name);
            assertTrue(name + " stub must exist", f.isFile());
            // The stub may be empty
            assertTrue(name + " stub size must be >= 0", f.length() >= 0);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateInstanceThrowsIfAlreadyExists() throws IOException {
        ManzanInstanceManager.createInstance(INST_A);
        // Second call must throw
        ManzanInstanceManager.createInstance(INST_A);
    }

    @Test(expected = SecurityException.class)
    public void testCreateInstanceRejectsUppercaseName() throws IOException {
        ManzanInstanceManager.createInstance("UPPERCASE");
    }

    @Test(expected = SecurityException.class)
    public void testCreateInstanceRejectsNameWithSpaces() throws IOException {
        ManzanInstanceManager.createInstance("bad name");
    }

    @Test(expected = SecurityException.class)
    public void testCreateInstanceRejectsReservedName() throws IOException {
        ManzanInstanceManager.createInstance("root");
    }

    // -------------------------------------------------------------------------
    // destroyInstance tests
    // -------------------------------------------------------------------------

    @Test
    public void testDestroyInstanceRemovesConfigDir() throws IOException {
        ManzanInstanceManager.createInstance(INST_B);
        File configDir = new File("/QOpenSys/etc/manzan-" + INST_B);
        assertTrue("Config dir must exist before destroy", configDir.isDirectory());

        ManzanInstanceManager.destroyInstance(INST_B, false);

        assertFalse("Config dir must not exist after destroy", configDir.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDestroyInstanceThrowsIfNotExists() throws IOException {
        // Never created — must throw immediately
        ManzanInstanceManager.destroyInstance("mgr-test-never-created", false);
    }

    @Test
    public void testDestroyInstanceThrowsIfRunning() throws IOException {
        ManzanInstanceManager.createInstance(INST_B);
        File configDir = new File("/QOpenSys/etc/manzan-" + INST_B);

        LockFile lock = LockFile.acquire(INST_B);
        assertNotNull("Lock must be acquired to simulate a running instance", lock);

        try {
            ManzanInstanceManager.destroyInstance(INST_B, false);
            fail("destroyInstance must throw IllegalStateException for a running instance");
        } catch (IllegalStateException e) {
            // expected — message should mention the instance name
            assertTrue("Exception message must name the instance",
                    e.getMessage().contains(INST_B));
        } finally {
            lock.release();
        }

        // Config dir must be untouched
        assertTrue("Config dir must remain intact when destroy is blocked", configDir.isDirectory());
    }

    @Test
    public void testDestroyInstanceRemovesStaleLockFile() throws IOException {
        ManzanInstanceManager.createInstance(INST_B);

        // Manually plant a stale lock file (PID 0 is never a live process)
        File lockDir = new File("/var/run/manzan");
        lockDir.mkdirs();
        File stale = new File(lockDir, "manzan-" + INST_B + ".lock");
        Files.write(stale.toPath(), "0".getBytes());
        assertTrue("Stale lock file must exist before destroy", stale.exists());

        ManzanInstanceManager.destroyInstance(INST_B, false);

        assertFalse("Stale lock file must be removed by destroy", stale.exists());
    }

    // -------------------------------------------------------------------------
    // listInstances tests
    // -------------------------------------------------------------------------

    @Test
    public void testListInstancesContainsCreatedInstance() throws IOException {
        ManzanInstanceManager.createInstance(INST_C);

        List<InstanceInfo> list = ManzanInstanceManager.listInstances();
        InstanceInfo found = findByName(list, INST_C);

        assertNotNull("listInstances must include the newly created instance", found);
        assertEquals("Config directory must match", "/QOpenSys/etc/manzan-" + INST_C,
                found.getConfigDirectory());
        assertFalse("Instance must not appear as running when no lock file exists",
                found.isRunning());
        assertEquals("PID must be -1 when not running", -1L, found.getPid());
    }

    @Test
    public void testListInstancesShowsRunningStatus() throws IOException {
        ManzanInstanceManager.createInstance(INST_C);
        LockFile lock = LockFile.acquire(INST_C);
        assertNotNull("Lock must be acquired", lock);

        try {
            List<InstanceInfo> list = ManzanInstanceManager.listInstances();
            InstanceInfo found = findByName(list, INST_C);

            assertNotNull("Running instance must appear in listInstances", found);
            assertTrue("Instance must be reported as running", found.isRunning());
            assertEquals("Reported PID must match lock-file PID", lock.getPid(), found.getPid());
        } finally {
            lock.release();
        }
    }

    @Test
    public void testListInstancesSortedByName() throws IOException {
        // Create three instances whose names sort in a non-insertion order
        ManzanInstanceManager.createInstance(INST_ZOO);
        ManzanInstanceManager.createInstance(INST_ALPHA);
        ManzanInstanceManager.createInstance(INST_BETA);

        List<InstanceInfo> list = ManzanInstanceManager.listInstances();

        // Find positions of our three instances
        int posAlpha = indexByName(list, INST_ALPHA);
        int posBeta  = indexByName(list, INST_BETA);
        int posZoo   = indexByName(list, INST_ZOO);

        assertTrue("alpha must appear before beta", posAlpha < posBeta);
        assertTrue("beta must appear before zoo",   posBeta  < posZoo);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListInstancesIsUnmodifiable() throws IOException {
        ManzanInstanceManager.createInstance(INST_C);
        List<InstanceInfo> list = ManzanInstanceManager.listInstances();
        // Must throw — list is unmodifiable
        list.add(null);
    }

    @Test
    public void testListInstancesDetectsOrphanedLock() throws IOException {
        // No config directory — only a stale-but-live-looking lock file.
        // We simulate an "orphaned" entry by writing a real PID for the current
        // process so /proc/<pid> exists and LockFile.listActiveLocks() reports it.
        File lockDir = new File("/var/run/manzan");
        lockDir.mkdirs();
        String orphanName = PREFIX + "orphan";
        File lockFile = new File(lockDir, "manzan-" + orphanName + ".lock");

        // Write the current JVM's PID so isProcessRunning() returns true
        String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        long currentPid = Long.parseLong(jvmName.split("@")[0]);
        Files.write(lockFile.toPath(), String.valueOf(currentPid).getBytes());

        try {
            List<InstanceInfo> list = ManzanInstanceManager.listInstances();
            InstanceInfo orphan = findByName(list, orphanName);

            assertNotNull("Orphaned lock must appear in listInstances", orphan);
            assertTrue("Orphaned lock must be reported as running", orphan.isRunning());
            // Config directory path is synthesised even though the directory does not exist
            assertTrue("Config directory path must contain instance name",
                    orphan.getConfigDirectory().contains(orphanName));
        } finally {
            lockFile.delete();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static InstanceInfo findByName(List<InstanceInfo> list, String name) {
        for (InstanceInfo info : list) {
            if (name.equals(info.getInstanceName())) return info;
        }
        return null;
    }

    private static int indexByName(List<InstanceInfo> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).getInstanceName())) return i;
        }
        return -1;
    }

    private void cleanupAll() {
        // Remove all config directories created by this suite
        String[] names = {
            INST_A, INST_B, INST_C, INST_ALPHA, INST_BETA, INST_ZOO,
            PREFIX + "orphan", "default"
        };
        for (String name : names) {
            deleteDirectory(new File("/QOpenSys/etc/manzan-" + name));
        }

        // Remove all lock files created by this suite
        File lockDir = new File("/var/run/manzan");
        if (lockDir.isDirectory()) {
            for (String name : names) {
                new File(lockDir, "manzan-" + name + ".lock").delete();
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
}
