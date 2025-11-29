package com.maxprograms.filter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Tracker {

    private static Tracker instance;
    private File logFile;

    private Tracker() {
        // Prevent instantiation
        String home = System.getenv("MXP_HOME");
        if (home == null) {
            home = System.getProperty("user.home") + "/maxprograms";
        }
        File homeFolder = new File(home);
        if (!homeFolder.exists()) {
            homeFolder.mkdirs();
        }
        File logFolder = new File(homeFolder, "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        logFile = new File(logFolder, "access_log.tsv");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(logFile)) {
                    String labels = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", "Timestamp", "Country", "Status", "Browser", "OS", "RemoteAddr", "Referer", "Page");
                    fos.write(labels.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }
                System.out.println("Created log file at " + logFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized Tracker getInstance() {
        if (instance == null) {
            instance = new Tracker();
        }
        return instance;
    }

    public void track(String remoteAddr, String referer, String uri, String browser, String os, int status) throws IOException {
        String country = IpUtils.getCountryForIp(remoteAddr);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String logEntry = String.format("%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\n", timestamp, country, status, browser, os, remoteAddr, referer, uri);
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(logEntry.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        }
    }

}
