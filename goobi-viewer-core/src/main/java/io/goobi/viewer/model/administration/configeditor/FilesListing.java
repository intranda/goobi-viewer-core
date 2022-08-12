package io.goobi.viewer.model.administration.configeditor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;

public class FilesListing implements Serializable {

    private static final long serialVersionUID = 1L;

    private File[] files = null;
    private String[] fileNames = null;
    private List<FileRecord> fileRecords = null;
    private DataModel<FileRecord> fileRecordsModel = null;

    public FilesListing() {
        // No need to bother if it is disabled
        if (isEnabled()) {
            fileRecords = new ArrayList<>();
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    return name.endsWith(".xml") || name.endsWith(".properties");
                }
            };

            files = new File[0];

            for (String configPath : DataManager.getInstance().getConfiguration().getConfigEditorDirectories()) {
                File f = new File(FileTools.adaptPathForWindows(configPath));
                files = Stream.concat(Arrays.stream(files), Arrays.stream(f.listFiles(filter))).toArray(File[]::new);
            }

            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
            fileNames = new String[files.length];
            for (int i = 0; i < files.length; ++i) {
                fileNames[i] = files[i].getName();
                fileRecords.add(new FileRecord(fileNames[i], i, files[i].canRead(), files[i].canWrite()));
            }

            fileRecordsModel = new ListDataModel<>(fileRecords);
        }
    }

    public File[] getFiles() {
        return files;
    }

    public String[] getFileNames() {
        return fileNames;
    }

    public List<FileRecord> getFileRecords() {
        return fileRecords;
    }

    public DataModel<FileRecord> getFileRecordsModel() {
        return fileRecordsModel;
    }

    public boolean isEnabled() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    public int getMaxBackups() {
        return DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups();
    }

}
