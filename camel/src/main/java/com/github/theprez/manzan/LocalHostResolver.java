package com.github.theprez.manzan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

public class LocalHostResolver {
    public static String getFQDN() throws IOException {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (IOException e) {
            Process p = Runtime.getRuntime().exec("/QOpenSys/usr/bin/hostname");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return br.readLine().toLowerCase().trim();
            }
        }
    }
}
