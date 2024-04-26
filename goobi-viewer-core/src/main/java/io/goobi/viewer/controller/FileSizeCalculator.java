/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

public final class FileSizeCalculator {

    private FileSizeCalculator() {
    }

    public static long getFileSize(Path path) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return Files.size(path);
        } else {
            return 0L;
        }
    }

    public static String formatSize(long sizeInBytes) {
        String[] units = { "B", "KB", "MB", "GB", "TB" };
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
