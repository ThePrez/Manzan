import com.github.theprez.manzan.InstanceContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for InstanceContext class
 * Tests instance name validation, config directory management, and security features
 */
public class InstanceContextTest {
    
    private static final String TEST_INSTANCE_NAME = "test-instance";
    private File testConfigDir;
    
    @Before
    public void setUp() {
        // Clean up any existing test directories
        cleanupTestDirectories();
    }
    
    @After
    public void tearDown() {
        // Clean up test directories after each test
        cleanupTestDirectories();
    }
    
    private void cleanupTestDirectories() {
        // IBM i only: config directories live under /QOpenSys/etc/manzan-<instance>.
        // Clean up any directories created by the test instances used in this suite.
        String[] testInstanceNames = {
            "default", "watson-monitor", "test-instance", "watson-app", "navigator-app",
            "test-config-dir", "test-permissions", "test-tostring", "ibmi-test",
            "uniqueness-test", "nav-01", "test_instance", "prod-app-123", "a",
            "instance-with-many-dashes", "instance_with_underscores", "mix-of_both-123",
            "myapp", "test"
        };
        for (String name : testInstanceNames) {
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
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
    
    @Test
    public void testDefaultInstanceCreation() throws IOException {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[0]);
        
        assertNotNull("InstanceContext should not be null", ctx);
        assertEquals("Default instance name should be 'default'", "default", ctx.getInstanceName());
        assertTrue("Should be default instance", ctx.isDefaultInstance());
        assertNotNull("Config directory should not be null", ctx.getConfigDirectory());
        assertNotNull("User profile should not be null", ctx.getUserProfile());
        assertNotNull("Session ID prefix should not be null", ctx.getSessionIdPrefix());
    }
    
    @Test
    public void testInstanceCreationWithCommandLineArg() throws IOException {
        String[] args = {"--instance=watson-monitor"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        assertNotNull("InstanceContext should not be null", ctx);
        assertEquals("Instance name should be 'watson-monitor'", "watson-monitor", ctx.getInstanceName());
        assertFalse("Should not be default instance", ctx.isDefaultInstance());
        assertTrue("Config directory should contain instance name", 
                   ctx.getConfigDirectory().contains("watson-monitor"));
    }
    
    @Test
    public void testInstanceCreationWithEnvironmentVariable() throws IOException {
        // Note: This test would require setting environment variable before JVM starts
        // In practice, this is tested through integration tests
        // Here we test the fallback to default when env var is not set
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[0]);
        assertEquals("Should default to 'default' when no env var", "default", ctx.getInstanceName());
    }
    
    @Test
    public void testInstanceNameNormalization() throws IOException {
        String[] args = {"--instance=WATSON-MONITOR"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        assertEquals("Instance name should be lowercase", "watson-monitor", ctx.getInstanceName());
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithSpaces() throws IOException {
        String[] args = {"--instance=watson monitor"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithPathTraversal() throws IOException {
        String[] args = {"--instance=../etc/passwd"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithSlash() throws IOException {
        String[] args = {"--instance=watson/monitor"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithBackslash() throws IOException {
        String[] args = {"--instance=watson\\monitor"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameWithSpecialChars() throws IOException {
        String[] args = {"--instance=watson@monitor"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameTooLong() throws IOException {
        String[] args = {"--instance=this-is-a-very-long-instance-name-that-exceeds-the-maximum-length"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testInvalidInstanceNameEmpty() throws IOException {
        String[] args = {"--instance="};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testReservedInstanceNameRoot() throws IOException {
        String[] args = {"--instance=root"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test(expected = SecurityException.class)
    public void testReservedInstanceNameAdmin() throws IOException {
        String[] args = {"--instance=admin"};
        InstanceContext.fromEnvironment(args);
    }
    
    @Test
    public void testValidInstanceNames() throws IOException {
        String[] validNames = {
            "watson-monitor",
            "nav-01",
            "test_instance",
            "prod-app-123",
            "a",
            "instance-with-many-dashes",
            "instance_with_underscores",
            "mix-of_both-123"
        };
        
        for (String name : validNames) {
            String[] args = {"--instance=" + name};
            InstanceContext ctx = InstanceContext.fromEnvironment(args);
            assertNotNull("Should create context for valid name: " + name, ctx);
            assertEquals("Instance name should match", name.toLowerCase(), ctx.getInstanceName());
        }
    }
    
    @Test
    public void testSessionIdGeneration() throws IOException {
        String[] args = {"--instance=watson-monitor"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        String sessionId1 = ctx.generateSessionId("test");
        String sessionId2 = ctx.generateSessionId("test");
        
        assertNotNull("Session ID should not be null", sessionId1);
        assertEquals("Session ID should be 10 characters", 10, sessionId1.length());
        assertNotEquals("Session IDs should be unique", sessionId1, sessionId2);
        
        // Check format: 6-char prefix + 4-char counter
        assertTrue("Session ID should start with instance prefix", 
                   sessionId1.startsWith(ctx.getSessionIdPrefix()));
    }
    
    @Test
    public void testSessionIdReadability() throws IOException {
        // Test that session IDs are human-readable
        String[][] testCases = {
            {"watson-monitor", "WATSON"},
            {"nav-01", "NAV010"},
            {"default", "DEFAUL"},
            {"test", "TEST00"},
            {"myapp", "MYAPP0"}
        };
        
        for (String[] testCase : testCases) {
            String instanceName = testCase[0];
            String expectedPrefix = testCase[1];
            
            String[] args = {"--instance=" + instanceName};
            InstanceContext ctx = InstanceContext.fromEnvironment(args);
            
            assertEquals("Session ID prefix should be readable for " + instanceName,
                        expectedPrefix, ctx.getSessionIdPrefix());
            
            String sessionId = ctx.generateSessionId("test");
            assertTrue("Session ID should start with readable prefix",
                      sessionId.startsWith(expectedPrefix));
        }
    }
    
    @Test
    public void testConfigDirectoryCreation() throws IOException {
        String[] args = {"--instance=test-config-dir"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        File configDir = new File(ctx.getConfigDirectory());
        assertTrue("Config directory should exist", configDir.exists());
        assertTrue("Config directory should be a directory", configDir.isDirectory());
    }
    
    @Test
    public void testConfigDirectoryPermissions() throws IOException {
        // Only test on POSIX-compliant systems
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            String[] args = {"--instance=test-permissions"};
            InstanceContext ctx = InstanceContext.fromEnvironment(args);
            
            File configDir = new File(ctx.getConfigDirectory());
            Path path = configDir.toPath();
            
            try {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                
                assertTrue("Owner should have read permission", 
                          perms.contains(PosixFilePermission.OWNER_READ));
                assertTrue("Owner should have write permission", 
                          perms.contains(PosixFilePermission.OWNER_WRITE));
                assertTrue("Owner should have execute permission", 
                          perms.contains(PosixFilePermission.OWNER_EXECUTE));
                
                assertFalse("Group should not have read permission", 
                           perms.contains(PosixFilePermission.GROUP_READ));
                assertFalse("Others should not have read permission", 
                           perms.contains(PosixFilePermission.OTHERS_READ));
            } catch (UnsupportedOperationException e) {
                // Skip test on non-POSIX systems
                System.out.println("Skipping permission test on non-POSIX system");
            }
        }
    }
    
    @Test
    public void testMultipleInstancesWithDifferentNames() throws IOException {
        String[] args1 = {"--instance=watson-app"};
        String[] args2 = {"--instance=navigator-app"};
        
        InstanceContext ctx1 = InstanceContext.fromEnvironment(args1);
        InstanceContext ctx2 = InstanceContext.fromEnvironment(args2);
        
        assertNotEquals("Config directories should be different",
                       ctx1.getConfigDirectory(), ctx2.getConfigDirectory());
        assertNotEquals("Session ID prefixes should be different. ctx1=" + ctx1.getSessionIdPrefix() + ", ctx2=" + ctx2.getSessionIdPrefix(),
                       ctx1.getSessionIdPrefix(), ctx2.getSessionIdPrefix());
    }
    
    @Test
    public void testToString() throws IOException {
        String[] args = {"--instance=test-tostring"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        String str = ctx.toString();
        assertNotNull("toString should not return null", str);
        assertTrue("toString should contain instance name", str.contains("test-tostring"));
        assertTrue("toString should contain config directory", str.contains(ctx.getConfigDirectory()));
        assertTrue("toString should contain user profile", str.contains(ctx.getUserProfile()));
    }
    
    @Test
    public void testUserProfileDetection() throws IOException {
        InstanceContext ctx = InstanceContext.fromEnvironment(new String[0]);
        
        String userProfile = ctx.getUserProfile();
        assertNotNull("User profile should not be null", userProfile);
        assertFalse("User profile should not be empty", userProfile.isEmpty());
        
        // User profile should be uppercase (IBM i convention)
        assertEquals("User profile should be uppercase", 
                    userProfile.toUpperCase(), userProfile);
    }
    
    @Test
    public void testConfigDirectoryResolutionOnIBMi() {
        // This test verifies the logic but can't fully test IBM i behavior on other platforms
        // The actual IBM i path would be /QOpenSys/etc/manzan-<instance>
        String[] args = {"--instance=ibmi-test"};
        
        try {
            InstanceContext ctx = InstanceContext.fromEnvironment(args);
            String configDir = ctx.getConfigDirectory();
            
            assertNotNull("Config directory should not be null", configDir);
            assertTrue("Config directory should contain instance name", 
                      configDir.contains("ibmi-test"));
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
    }
    
    @Test
    public void testSessionIdUniquenessAcrossMultipleGenerations() throws IOException {
        String[] args = {"--instance=uniqueness-test"};
        InstanceContext ctx = InstanceContext.fromEnvironment(args);
        
        // Generate multiple session IDs and verify they're all unique
        java.util.Set<String> sessionIds = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            String sessionId = ctx.generateSessionId("test");
            assertFalse("Session ID should be unique: " + sessionId, 
                       sessionIds.contains(sessionId));
            sessionIds.add(sessionId);
            
            // Small delay to ensure timestamp changes
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        assertEquals("Should have generated 100 unique session IDs", 100, sessionIds.size());
    }
}