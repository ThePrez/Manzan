import com.github.theprez.manzan.LockFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Unit tests for LockFile class
 * Tests PID-based locking, stale lock detection, and concurrent access prevention
 */
public class LockFileTest {
    
    private static final String TEST_INSTANCE = "test-lock-instance";
    private File lockDir;
    
    @Before
    public void setUp() {
        // Clean up any existing lock files
        cleanupLockFiles();
    }
    
    @After
    public void tearDown() {
        // Clean up lock files after each test
        cleanupLockFiles();
    }
    
    private void cleanupLockFiles() {
        // IBM i only: lock directory is always /var/run/manzan
        lockDir = new File("/var/run/manzan");

        if (lockDir.exists() && lockDir.isDirectory()) {
            File[] lockFiles = lockDir.listFiles((dir, name) -> name.startsWith("manzan-") && name.endsWith(".lock"));
            if (lockFiles != null) {
                for (File lockFile : lockFiles) {
                    lockFile.delete();
                }
            }
        }
    }
    
    @Test
    public void testAcquireLock() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        
        assertNotNull("Lock should be acquired", lock);
        assertTrue("Lock should be valid", lock.isValid());
        assertEquals("Instance name should match", TEST_INSTANCE, lock.getInstanceName());
        assertNotNull("Lock file should exist", lock.getLockFile());
        assertTrue("Lock file should exist on disk", lock.getLockFile().exists());
        assertTrue("PID should be positive", lock.getPid() > 0);
        
        lock.release();
    }
    
    @Test
    public void testDuplicateLockPrevention() throws IOException {
        LockFile lock1 = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("First lock should be acquired", lock1);
        
        // Try to acquire the same lock again
        LockFile lock2 = LockFile.acquire(TEST_INSTANCE);
        assertNull("Second lock should not be acquired", lock2);
        
        lock1.release();
    }
    
    @Test
    public void testLockRelease() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);
        
        File lockFile = lock.getLockFile();
        assertTrue("Lock file should exist before release", lockFile.exists());
        
        lock.release();
        
        assertFalse("Lock file should not exist after release", lockFile.exists());
        assertFalse("Lock should not be valid after release", lock.isValid());
    }
    
    @Test
    public void testLockReacquisitionAfterRelease() throws IOException {
        // Acquire and release lock
        LockFile lock1 = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("First lock should be acquired", lock1);
        lock1.release();
        
        // Should be able to acquire again after release
        LockFile lock2 = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be reacquirable after release", lock2);
        assertTrue("Second lock should be valid", lock2.isValid());
        
        lock2.release();
    }
    
    @Test
    public void testMultipleInstancesWithDifferentNames() throws IOException {
        LockFile lock1 = LockFile.acquire("instance1");
        LockFile lock2 = LockFile.acquire("instance2");
        
        assertNotNull("First lock should be acquired", lock1);
        assertNotNull("Second lock should be acquired", lock2);
        assertTrue("First lock should be valid", lock1.isValid());
        assertTrue("Second lock should be valid", lock2.isValid());
        
        assertNotEquals("Lock files should be different", 
                       lock1.getLockFile(), lock2.getLockFile());
        
        lock1.release();
        lock2.release();
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithSlash() throws IOException {
        LockFile.acquire("invalid/name");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithBackslash() throws IOException {
        LockFile.acquire("invalid\\name");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithPathTraversal() throws IOException {
        LockFile.acquire("../etc/passwd");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameEmpty() throws IOException {
        LockFile.acquire("");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameNull() throws IOException {
        LockFile.acquire(null);
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithSpecialChars() throws IOException {
        LockFile.acquire("invalid@name");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameUppercase() throws IOException {
        // LockFile enforces lowercase-only, same as InstanceContext
        LockFile.acquire("UPPERCASE");
    }

    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameTooLong() throws IOException {
        LockFile.acquire("this-is-a-very-long-instance-name-that-exceeds-the-maximum");
    }
    
    @Test
    public void testValidInstanceNames() throws IOException {
        String[] validNames = {
            "watson-monitor",
            "nav-01",
            "test_instance",
            "prod-app-123",
            "a",
            "lowercase",
            "mixed-case-123"
        };
        
        for (String name : validNames) {
            LockFile lock = LockFile.acquire(name);
            assertNotNull("Should acquire lock for valid name: " + name, lock);
            assertTrue("Lock should be valid for: " + name, lock.isValid());
            lock.release();
        }
    }
    
    @Test
    public void testLockFileContainsPID() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);
        
        File lockFile = lock.getLockFile();
        assertTrue("Lock file should exist", lockFile.exists());
        
        // Read lock file content
        String content = new String(Files.readAllBytes(lockFile.toPath())).trim();
        assertFalse("Lock file should not be empty", content.isEmpty());
        
        // Verify content is a valid PID (number)
        try {
            long pid = Long.parseLong(content);
            assertTrue("PID should be positive", pid > 0);
            assertEquals("PID in file should match lock PID", lock.getPid(), pid);
        } catch (NumberFormatException e) {
            fail("Lock file should contain valid PID: " + content);
        }
        
        lock.release();
    }
    
    @Test
    public void testLockFileLocation() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);

        File lockFile = lock.getLockFile();
        String lockPath = lockFile.getAbsolutePath();

        // IBM i only: lock directory is always /var/run/manzan
        assertTrue("Lock file should be in /var/run/manzan", lockPath.startsWith("/var/run/manzan"));

        // Verify lock file name format
        String fileName = lockFile.getName();
        assertTrue("Lock file should start with 'manzan-'", fileName.startsWith("manzan-"));
        assertTrue("Lock file should end with '.lock'", fileName.endsWith(".lock"));
        assertTrue("Lock file should contain instance name", fileName.contains(TEST_INSTANCE));

        lock.release();
    }
    
    @Test
    public void testToString() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);
        
        String str = lock.toString();
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain instance name", str.contains(TEST_INSTANCE));
        assertTrue("toString should contain PID", str.contains(String.valueOf(lock.getPid())));
        assertTrue("toString should contain valid status", str.contains("valid=true"));
        
        lock.release();
    }
    
    @Test
    public void testLockValidityAfterRelease() throws IOException {
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);
        assertTrue("Lock should be valid initially", lock.isValid());
        
        lock.release();
        
        assertFalse("Lock should not be valid after release", lock.isValid());
    }
    
    @Test
    public void testStaleLockCleanup() throws IOException {
        // Create a stale lock file manually
        File lockFile = new File(lockDir, "manzan-stale-test.lock");
        lockDir.mkdirs();
        
        // Write an invalid PID (0 or negative)
        Files.write(lockFile.toPath(), "0".getBytes());
        assertTrue("Stale lock file should exist", lockFile.exists());
        
        // Try to acquire lock - should clean up stale lock and succeed
        LockFile lock = LockFile.acquire("stale-test");
        assertNotNull("Should acquire lock after cleaning stale lock", lock);
        assertTrue("Lock should be valid", lock.isValid());
        
        lock.release();
    }
    
    @Test
    public void testConcurrentAccessFromSameJVM() throws IOException, InterruptedException {
        final LockFile[] lock1 = new LockFile[1];
        final LockFile[] lock2 = new LockFile[1];
        final Exception[] exception = new Exception[1];
        
        // Thread 1: Acquire lock
        Thread thread1 = new Thread(() -> {
            try {
                lock1[0] = LockFile.acquire(TEST_INSTANCE);
                Thread.sleep(100); // Hold lock briefly
            } catch (Exception e) {
                exception[0] = e;
            }
        });
        
        // Thread 2: Try to acquire same lock
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(50); // Let thread1 acquire first
                lock2[0] = LockFile.acquire(TEST_INSTANCE);
            } catch (Exception e) {
                exception[0] = e;
            }
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        assertNull("No exception should be thrown", exception[0]);
        assertNotNull("Thread 1 should acquire lock", lock1[0]);
        assertNull("Thread 2 should not acquire lock", lock2[0]);
        
        if (lock1[0] != null) {
            lock1[0].release();
        }
    }
    
    @Test
    public void testLockDirectoryCreation() throws IOException {
        // Clean up lock directory if it exists
        if (lockDir.exists()) {
            File[] files = lockDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            lockDir.delete();
        }
        
        assertFalse("Lock directory should not exist initially", lockDir.exists());
        
        // Acquire lock should create directory
        LockFile lock = LockFile.acquire(TEST_INSTANCE);
        assertNotNull("Lock should be acquired", lock);
        assertTrue("Lock directory should be created", lockDir.exists());
        assertTrue("Lock directory should be a directory", lockDir.isDirectory());
        
        lock.release();
    }
    
    @Test
    public void testMultipleLockAcquisitionsSequentially() throws IOException {
        // Acquire and release lock multiple times
        for (int i = 0; i < 10; i++) {
            LockFile lock = LockFile.acquire(TEST_INSTANCE);
            assertNotNull("Lock should be acquired on iteration " + i, lock);
            assertTrue("Lock should be valid on iteration " + i, lock.isValid());
            lock.release();
        }
    }
}
