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
package io.goobi.viewer.model.administration.configeditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileRecord {

    private final Path file;
    private final int number;
    private final String fileType;

    /**
     * 
     * @param file
     * @param number
     */
    public FileRecord(Path file, int number) {
        this.file = file;
        this.number = number;
        this.fileType = getFileTypeFromName(file.getFileName().toString());
    }

    private static String getFileTypeFromName(String name) {
        int lastIndex = name.lastIndexOf(".");
        if (lastIndex == -1) {
            return "";
        }
        return name.substring(lastIndex + 1);
    }

    public Path getFile() {
        return file;
    }

    public String getFileName() {
        return file.getFileName().toString();
    }

    public int getNumber() {
        return number;
    }

    public boolean isReadable() {
        return Files.isReadable(file);
    }

    public boolean isWritable() {
        return Files.isWritable(file);
    }

    public String getFileType() {
        return fileType;
    }

    @Override
    public int hashCode() {
        return this.file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && this.getClass().equals(obj.getClass())) {
            return Objects.equals(this.getFile(), ((FileRecord) obj).getFile());
        } else {
            return false;
        }
    }

}
