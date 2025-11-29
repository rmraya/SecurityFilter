package com.maxprograms.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IpUtils {

    // Get data from https://github.com/sapics/ip-location-db

    // Directlink from CDN:

    // https://cdn.jsdelivr.net/npm/@ip-location-db/geolite2-country/geolite2-country-ipv4.csv

    private static final String DEFAULT_CSV_PATH = "geolite2-country-ipv4.csv";
    private static final List<IpRange> IP_RANGES = new ArrayList<>();
    private static boolean rangesLoaded = false;

    public static long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ipAddress);
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(parts[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid octet in IP: " + parts[i]);
            }
            result = (result << 8) | octet;
        }
        return result;
    }

    public static String getCountryForIp(String ipAddress) {
        ensureRangesLoaded();
        long ip = ipToLong(ipAddress);
        int index = findRangeIndex(ip);
        return index >= 0 ? IP_RANGES.get(index).country : "--";
    }

    private static synchronized void loadIpRanges() throws IOException {
        if (rangesLoaded) {
            return;
        }

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
        String csvPath = new File(logFolder, DEFAULT_CSV_PATH).getAbsolutePath();

        List<IpRange> ranges = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(csvPath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }
                String[] columns = line.split(",", 3);
                if (columns.length != 3) {
                    throw new IllegalArgumentException("Invalid CSV format at line " + lineNumber + ": " + line);
                }
                long start = ipToLong(columns[0].trim());
                long end = ipToLong(columns[1].trim());
                if (start > end) {
                    throw new IllegalArgumentException(
                            "Start IP greater than end IP at line " + lineNumber + ": " + line);
                }
                String country = columns[2].trim();
                ranges.add(new IpRange(start, end, country));
            }
        }

        ranges.sort(Comparator.comparingLong(range -> range.start));
        IP_RANGES.clear();
        IP_RANGES.addAll(ranges);
        rangesLoaded = true;
    }

    private static void ensureRangesLoaded() {
        if (!rangesLoaded) {
            try {
                loadIpRanges();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load IP ranges from " + DEFAULT_CSV_PATH, e);
            }
        }
    }

    // Binary search to locate the range covering the given IP.
    private static int findRangeIndex(long ip) {
        int low = 0;
        int high = IP_RANGES.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            IpRange range = IP_RANGES.get(mid);
            if (ip < range.start) {
                high = mid - 1;
            } else if (ip > range.end) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private static final class IpRange {
        private final long start;
        private final long end;
        private final String country;

        private IpRange(long start, long end, String country) {
            this.start = start;
            this.end = end;
            this.country = country;
        }
    }
}
