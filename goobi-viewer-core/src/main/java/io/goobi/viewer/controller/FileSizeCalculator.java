package io.goobi.viewer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

public class FileSizeCalculator {

    private FileSizeCalculator() {
    }
    
    public static long getFileSize(Path path) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return Files.size(path);
        } else {
            return 0l;
        }
    }

    public static String formatSize(long sizeInBytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes;

        while (size > 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }
}
