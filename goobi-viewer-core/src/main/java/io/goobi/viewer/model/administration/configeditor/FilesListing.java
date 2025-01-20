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

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.faces.model.DataModel;
import jakarta.faces.model.ListDataModel;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;

public class FilesListing implements Serializable {

    private static final long serialVersionUID = -1261644749731156548L;

    private static final Logger logger = LogManager.getLogger(FilesListing.class);

    private transient List<FileRecord> fileRecords = null;
    private transient DataModel<FileRecord> fileRecordsModel = null;

    public FilesListing() {
        // No need to bother if it is disabled
        if (DataManager.getInstance().getConfiguration().isConfigEditorEnabled()) {
            refresh();
        }
    }

    /**
     * 
     */
    public void refresh() {
        logger.trace("refresh");
        fileRecords = new ArrayList<>();
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".xml") || name.endsWith(".properties");
            }
        };

        File[] files = new File[0];
        for (String configPath : DataManager.getInstance().getConfiguration().getConfigEditorDirectories()) {
            File directory = new File(FileTools.adaptPathForWindows(configPath));
            if (directory.isDirectory()) {
                File[] dirFiles = directory.listFiles(filter);
                if (dirFiles != null) {
                    files = Stream.concat(Arrays.stream(files), Arrays.stream(dirFiles)).toArray(File[]::new);
                }
            }

        }

        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (int i = 0; i < files.length; ++i) {
            fileRecords.add(new FileRecord(files[i].toPath(), i));
        }

        fileRecordsModel = new ListDataModel<>(fileRecords);
    }

    public List<FileRecord> getFileRecords() {
        return fileRecords;
    }

    public DataModel<FileRecord> getFileRecordsModel() {
        return fileRecordsModel;
    }

    public int getMaxBackups() {
        return DataManager.getInstance().getConfiguration().getConfigEditorBackupFiles();
    }

}
