package com.github.theprez.manzan;

import java.io.IOException;

import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;

public class IBMiConnectionSupplier {
    private static boolean s_isIBMi = checkIsIBMi();

    private static boolean checkIsIBMi() {
        final String osName = System.getProperty("os.name", "Misty");
        return "os400".equalsIgnoreCase(osName) || "os/400".equalsIgnoreCase(osName);
    }

    public static AS400 getSystemConnection() throws IOException, AS400SecurityException {
        if (s_isIBMi) {
            return new AS400("localhost", "*CURRENT", "*CURRENT");
        }
        return ApplicationConfig.get().getRemoteConnection();
    }

    public static boolean isIBMi() {
        return s_isIBMi;
    }
}
