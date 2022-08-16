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

import java.io.Serializable;

public class FileRecord implements Serializable {

    private static final long serialVersionUID = -1074497836317565196L;

    private String fileName;
    private int number;
    private boolean readable;
    private boolean writable;
    private String fileType;

    public FileRecord() {

    }

    public FileRecord(String fileName, int number, boolean readable, boolean writable) {
        this.fileName = fileName;
        this.number = number;
        this.readable = readable;
        this.writable = writable;
        this.fileType = getFileTypeFromName(fileName);
    }

    private static String getFileTypeFromName(String name) {
        int lastIndex = name.lastIndexOf(".");
        if (lastIndex == -1) {
            return "";
        }
        return name.substring(lastIndex + 1);
    }

    public String getFileName() {
        return fileName;
    }

    public int getNumber() {
        return number;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public String getFileType() {
        return fileType;
    }

}
