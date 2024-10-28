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
package io.goobi.viewer.managedbeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.DataModel;
import jakarta.faces.model.ListDataModel;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.omnifaces.util.Faces;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.configeditor.BackupRecord;
import io.goobi.viewer.model.administration.configeditor.FileLocks;
import io.goobi.viewer.model.administration.configeditor.FileRecord;
import io.goobi.viewer.model.administration.configeditor.FilesListing;
import io.goobi.viewer.model.files.upload.FileUploader;
import io.goobi.viewer.model.xml.XMLError;

/**
 * <p>
 * AdminConfigEditorBean class.
 * </p>
 */
@Named
@SessionScoped
public class AdminConfigEditorBean implements Serializable {

    private static final long serialVersionUID = -4120457702630667052L;

    private static final Logger logger = LogManager.getLogger(AdminConfigEditorBean.class);

    /** Manual edit locks for files. */
    private static FileLocks fileLocks = new FileLocks();

    /** Object that handles the reading of listed files. */
    private final FilesListing filesListing = new FilesListing();

    // Fields for FileEdition
    private int fileInEditionNumber = -1;
    private transient FileRecord currentFileRecord;
    private String fileContent;
    private String unmodifiledFileContent = ""; // Used to check if the content of the textarea is modified

    // ReadOnly or editable
    private boolean editable = false;

    // Fields for Backups
    private List<BackupRecord> backupRecords = new ArrayList<>();
    private transient DataModel<BackupRecord> backupRecordsModel;
    private File[] backupFiles;
    private String[] backupNames;
    private String backupsPath; // to maintain the backup files

    // Fields for File-IO
    //    private transient FileOutputStream fileOutputStream = null;
    private transient FileLock inputLock = null;
    //    private transient FileLock outputLock = null;

    // Used to render the CodeMirror editor properly, values can be "properties" or "xml"
    private String fullCurrentConfigFileType; // "." + currentConfigFileType

    private boolean nightMode = false;

    private FileUploader fileUploader = new FileUploader();

    /**
     * <p>
     * Constructor for AdminConfigEditorBean.
     * </p>
     */
    public AdminConfigEditorBean() {
        //
    }

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        if (!DataManager.getInstance().getConfiguration().isConfigEditorEnabled()) {
            // Give a message that the config-editor is not activated.
            logger.warn("The ConfigEditor is not activated!");
            return;
        }

        // Create the folder "backups" if necessary.
        backupsPath = FileTools.adaptPathForWindows(DataManager.getInstance().getConfiguration().getConfigLocalPath() + "backups/");
        File backups = new File(backupsPath);
        if (!backups.exists()) {
            backups.mkdir();
        }

        // Initialize Backup List 
        backupFiles = null;
        backupNames = null;
        backupRecords = new ArrayList<>();
        backupRecordsModel = new ListDataModel<>(backupRecords);
    }

    /**
     * <p>
     * isRenderBackend.
     * </p>
     *
     * @return a boolean
     */
    public boolean isRenderBackend() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    /**
     * <p>
     * refresh.
     * </p>
     */
    public void refresh() {
        filesListing.refresh();
    }

    public void selectBackup(int backupNumber) throws IOException {
        if (backupNumber > -1 && backupNumber < backupFiles.length) {
            Path path = backupFiles[backupNumber].toPath();
            fileContent = Files.readString(path);
        }
    }

    public void upload(Path file) {
        if (this.fileUploader.isReadoForUpload()) {
            this.fileUploader.upload();
            if (this.fileUploader.isUploaded()) {
                try {
                    Files.write(file, this.fileUploader.getFileContents(), StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    FileRecord uploadedRecord = getRecord(file.getFileName().toString());
                    if (uploadedRecord != null) {
                        this.createBackup(uploadedRecord);
                        if (uploadedRecord.equals(currentFileRecord)) {
                            this.selectFileAndShowBackups(uploadedRecord.isWritable());
                        }

                    }
                } catch (IOException e) {
                    logger.error("Error uploading file {}: {}", file, e);
                    Messages.error("Error uploading file");
                } finally {
                    this.fileUploader = new FileUploader();
                }
            }
        }
    }

    public void download(Path file) throws IOException {
        Faces.sendFile(file, file.getFileName().toString(), true);
    }

    /**
     * <p>
     * getFileRecordsModel.
     * </p>
     *
     * @return a {@link jakarta.faces.model.DataModel} object
     */
    public DataModel<FileRecord> getFileRecordsModel() {
        return filesListing.getFileRecordsModel();
    }

    /**
     * <p>
     * Getter for the field <code>fileInEditionNumber</code>.
     * </p>
     *
     * @return a int
     */
    public int getFileInEditionNumber() {
        return fileInEditionNumber;
    }

    /**
     * <p>
     * Setter for the field <code>fileInEditionNumber</code>.
     * </p>
     *
     * @param fileInEditionNumber a int
     */
    public void setFileInEditionNumber(int fileInEditionNumber) {
        this.fileInEditionNumber = fileInEditionNumber;
    }

    /**
     * <p>
     * Getter for the field <code>currentFileRecord</code>.
     * </p>
     *
     * @return the currentFileRecord
     */
    public FileRecord getCurrentFileRecord() {
        return currentFileRecord;
    }

    /**
     * <p>
     * Getter for the field <code>fileContent</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getFileContent() {
        return fileContent;
    }

    /**
     * <p>
     * Setter for the field <code>fileContent</code>.
     * </p>
     *
     * @param fileContent a {@link java.lang.String} object
     */
    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    /**
     * <p>
     * Getter for the field <code>backupRecords</code>.
     * </p>
     *
     * @return a {@link java.util.List} object
     */
    public List<BackupRecord> getBackupRecords() {
        return backupRecords;
    }

    /**
     * <p>
     * Getter for the field <code>backupRecordsModel</code>.
     * </p>
     *
     * @return a {@link jakarta.faces.model.DataModel} object
     */
    public DataModel<BackupRecord> getBackupRecordsModel() {
        return backupRecordsModel;
    }

    /**
     * <p>
     * isEditable.
     * </p>
     *
     * @return a boolean
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * <p>
     * Setter for the field <code>editable</code>.
     * </p>
     *
     * @param editable a boolean
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * <p>
     * isBackupsAvailable.
     * </p>
     *
     * @return a boolean
     */
    public boolean isBackupsAvailable() {
        return !backupRecords.isEmpty();
    }

    /**
     * <p>
     * getCurrentConfigFileType.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getCurrentConfigFileType() {
        if (currentFileRecord != null) {
            return currentFileRecord.getFileType();
        }

        return "";
    }

    /**
     * Determines whether the given fileRecord is locked by a different user session.
     *
     * @param fileRecord a {@link io.goobi.viewer.model.administration.configeditor.FileRecord} object
     * @return true if file path locked by other session id; false otherwise
     */
    public boolean isFileLocked(FileRecord fileRecord) {
        if (fileRecord == null) {
            return false;
        }

        return fileLocks.isFileLockedByOthers(fileRecord.getFile(), BeanUtils.getSession().getId());
    }

    /**
     * <p>
     * isNightMode.
     * </p>
     *
     * @return a boolean
     */
    public boolean isNightMode() {
        return nightMode;
    }

    /**
     * <p>
     * changeNightMode.
     * </p>
     */
    public void changeNightMode() {
        nightMode = !nightMode;
    }

    /**
     * <p>
     * openFile.
     * </p>
     *
     * @throws java.io.IOException
     */
    public synchronized void openFile() throws IOException {
        if (fileInEditionNumber < 0) {
            return;
        }

        currentFileRecord = filesListing.getFileRecords().get(fileInEditionNumber);

        Path filePath = currentFileRecord.getFile();

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            FileChannel inputChannel = fis.getChannel();

            // get an exclusive lock if the file is editable, otherwise a shared lock
            if (editable) {
                String sessionId = BeanUtils.getSession().getId();
                // File already locked by someone else
                if (fileLocks.isFileLockedByOthers(filePath, sessionId)) {
                    Messages.error("admin__config_editor__file_locked_msg");
                    return;
                }
                fileLocks.lockFile(filePath, sessionId);
                logger.trace("{} locked for session ID {}", filePath.toAbsolutePath(), sessionId);
                // outputLock also locks reading this file in Windows, so read it prior to creating the lock
                fileContent = Files.readString(filePath);
            } else { // READ_ONLY
                inputLock = inputChannel.tryLock(0, Long.MAX_VALUE, true);
                if (inputLock == null) {
                    throw new OverlappingFileLockException();
                }
                fileContent = Files.readString(filePath);
            }
            unmodifiledFileContent = fileContent;
        } catch (OverlappingFileLockException oe) {
            logger.trace("The region specified is already locked by another process.", oe);

        } catch (IOException e) {
            logger.trace("IOException caught in the method openFile()", e);
        } finally {
            if (inputLock != null && inputLock.isValid()) {
                inputLock.release();
            }
        }
    }

    /**
     * <p>
     * closeCurrentFileAction.
     * </p>
     *
     * @return Navigation outcome
     */
    public String closeCurrentFileAction() {
        if (currentFileRecord == null) {
            return "";
        }

        fileInEditionNumber = -1;
        currentFileRecord = null;

        refresh();

        return "pretty:adminConfigEditor";
    }

    /**
     * Unlock the given file for the given session id in the static (global) fileLocks object
     *
     * @param file a {@link java.nio.file.Path} object
     * @param sessionId a {@link java.lang.String} object
     */
    public static void unlockFile(Path file, String sessionId) {
        logger.trace("Unlocking file {} for session {}", file, sessionId);
        if (file != null) {
            fileLocks.unlockFile(file, sessionId);
        }
    }

    /**
     * <p>
     * editFile.
     * </p>
     *
     * @param writable a boolean
     * @return a {@link java.lang.String} object
     */
    public String editFile(boolean writable) {
        selectFileAndShowBackups(writable);
        return "pretty:adminConfigEditorFilename";
    }

    /**
     * Saves the currently open file.
     *
     * @return Navigation outcome
     */
    public synchronized String saveCurrentFileAction() {
        logger.trace("saveCurrentFileAction");
        if (currentFileRecord == null) {
            logger.error("No record selected");
            return "";
        }

        // No need to duplicate if no modification is done.
        //        if (temp.equals(fileContent)) {
        //            return "";
        //        }
        //        if (fileOutputStream == null) {
        //            logger.error("No FileOutputStream");
        //            return "";
        //        }

        // Save the latest modification to the original path.
        Path originalPath = currentFileRecord.getFile();

        // Abort if file locked by someone else
        if (fileLocks.isFileLockedByOthers(originalPath, BeanUtils.getSession().getId())) {
            Messages.error("admin__config_editor__file_locked_msg");
            return "";
        }

        try {
            // Check XML validity
            if ("xml".equals(currentFileRecord.getFileType())) {
                List<XMLError> errors = XmlTools.checkXMLWellformed(fileContent);
                if (!errors.isEmpty()) {
                    boolean abort = false;
                    for (XMLError error : errors) {
                        Messages.error(String.format("%s: Line %d column %d: %s", error.getSeverity(), error.getLine(), error.getColumn(),
                                error.getMessage()));
                        if (error.getSeverity().equals("ERROR") || error.getSeverity().equals("FATAL")) {
                            abort = true;
                        }
                    }
                    if (abort) {
                        Messages.error("admin__config_editor__error_save_xml_invalid");
                        return "";
                    }
                }
            }

            // if the "config_viewer.xml" is being edited, then the original content of the block <configEditor> should be written back
            if (isConfigViewer()) {
                logger.debug("Saving {}, changes to config editor settings will be reverted...", Configuration.CONFIG_FILE_NAME);
                org.jdom2.Document doc = XmlTools.getDocumentFromString(fileContent, StandardCharsets.UTF_8.name());
                if (doc != null && doc.getRootElement() != null) {
                    int origConfigEditorMax = DataManager.getInstance().getConfiguration().getConfigEditorBackupFiles();
                    List<String> origConfigEditorDirectories = DataManager.getInstance().getConfiguration().getConfigEditorDirectories();
                    Element eleConfigEditor = doc.getRootElement().getChild("configEditor");
                    // Re-add configEditor element, if removed
                    if (eleConfigEditor == null) {
                        eleConfigEditor = new Element("configEditor");
                        doc.getRootElement().addContent(eleConfigEditor);
                    }
                    // Restore previous enabled value
                    Attribute attEnabled = eleConfigEditor.getAttribute("enabled");
                    if (attEnabled != null) {
                        attEnabled.setValue("true");
                    } else {
                        eleConfigEditor.setAttribute("enabled", "true");
                    }
                    // Restore previous backupFiles value
                    Attribute attBackupFiles = eleConfigEditor.getAttribute("backupFiles");
                    if (attBackupFiles != null) {
                        attBackupFiles.setValue(String.valueOf(origConfigEditorMax));
                    } else {
                        eleConfigEditor.setAttribute("backupFiles", String.valueOf(origConfigEditorMax));
                    }
                    // Replace directory list
                    eleConfigEditor.removeChildren("directory");
                    if (!origConfigEditorDirectories.isEmpty()) {
                        for (String dir : origConfigEditorDirectories) {
                            eleConfigEditor.addContent(new Element("directory").setText(dir));
                        }
                    }
                    logger.trace("configEditor settings restored");

                    fileContent = XmlTools.getXMLOutputter().outputString(doc);
                }
            }

            if (unmodifiledFileContent.equals(fileContent)) {
                return "";
            }

            Files.writeString(originalPath, fileContent, StandardCharsets.UTF_8);
            // In Windows, the exact same stream/channel that holds the outputLock must be used to write to avoid IOException
            //            IOUtils.write(fileContent, fileOutputStream, StandardCharsets.UTF_8);

            // Use the filename without extension to create a folder for its backup_copies.
            String newBackupFolderPath = backupsPath + currentFileRecord.getFileName().replaceFirst("[.][^.]+$", "");
            File newBackupFolder = new File(newBackupFolderPath);
            if (!newBackupFolder.exists()) {
                newBackupFolder.mkdir();
            }
            createBackup(newBackupFolderPath, currentFileRecord.getFileName(), unmodifiledFileContent);
            refreshBackups(newBackupFolder);
        } catch (IOException | JDOMException | ParserConfigurationException | SAXException e) {
            logger.error(e.getMessage(), e);
        }

        unmodifiledFileContent = fileContent;

        Messages.info("updatedSuccessfully");
        return "";
    }

    public void createBackup(FileRecord rec) throws IOException {
        String newBackupFolderPath = backupsPath + rec.getFileName().replaceFirst("[.][^.]+$", "");
        File newBackupFolder = new File(newBackupFolderPath);
        if (!newBackupFolder.exists()) {
            newBackupFolder.mkdir();
        }
        createBackup(newBackupFolderPath, rec.getFileName(), Files.readString(rec.getFile()));
        refreshBackups(newBackupFolder);
    }

    /**
     * Creates a timestamped backup of the given file name and content.
     *
     * @param backupFolderPath Backup folder path
     * @param fileName File name
     * @param content File content
     * @throws java.io.IOException
     */
    public static void createBackup(String backupFolderPath, String fileName, String content) throws IOException {
        if (backupFolderPath == null) {
            throw new IllegalAccessError("backupFolderPath may not be null");
        }
        if (fileName == null) {
            throw new IllegalAccessError("fileName may not be null");
        }
        if (content == null) {
            throw new IllegalAccessError("content may not be null");
        }

        // Use a time stamp to distinguish the backups.
        String timeStamp = DateTools.format(LocalDateTime.now(), DateTools.FORMATTERFILENAME, false);
        Path newBackupPath = Path.of(backupFolderPath, fileName + "." + timeStamp);
        // save the original content to backup files
        Files.writeString(newBackupPath, content, StandardCharsets.UTF_8);
    }

    /**
     * <p>
     * refreshBackups.
     * </p>
     *
     * @param backupFolder a {@link java.io.File} object
     */
    public void refreshBackups(File backupFolder) {
        backupRecords.clear();
        if (backupFolder == null || !Files.isDirectory(backupFolder.toPath())) {
            backupFiles = null;
            backupNames = null;
            return;
        }

        // refresh the backup metadata
        backupFiles = backupFolder.listFiles();
        int length = backupFiles.length;
        if (length > 0) {
            // Sort by date (descending)
            if (length > 1) {
                sortFilesByDateModified(backupFiles);
            }

            // Trim old backup files, if so configured
            if (DataManager.getInstance().getConfiguration().getConfigEditorBackupFiles() > 0) {
                while (length > DataManager.getInstance().getConfiguration().getConfigEditorBackupFiles()) {
                    try {
                        Files.delete(backupFiles[--length].toPath());
                        logger.trace("Rotated away backup: {}", backupFiles[length].toPath().getFileName());
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
                backupFiles = backupFolder.listFiles();
                if (backupFiles.length > 1) {
                    sortFilesByDateModified(backupFiles);
                }
            }

            backupNames = new String[backupFiles.length];
            for (int i = 0; i < length; ++i) {
                backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
                backupRecords.add(new BackupRecord(backupNames[i], i));
                logger.trace("Backup file: {}", backupFiles[i].getName());
            }
        } else {
            backupFiles = null;
            backupNames = null;
        }

        backupRecordsModel = new ListDataModel<>(backupRecords);
    }

    /**
     * 
     * @param backupFiles
     */
    static void sortFilesByDateModified(File[] backupFiles) {
        if (backupFiles == null || backupFiles.length == 0) {
            return;
        }

        Arrays.sort(backupFiles, (a, b) -> {
            try {
                return Files.getLastModifiedTime(b.toPath()).compareTo(Files.getLastModifiedTime(a.toPath()));
            } catch (IOException e) {
                logger.error(e.getMessage());
                return 0;
            }
        }); // last modified comes on top
    }

    /**
     * <p>
     * isConfigViewer.
     * </p>
     *
     * @return true if currently editing config_viewer.xml; false otherwise
     */
    public boolean isConfigViewer() {
        return currentFileRecord != null && currentFileRecord.getFileName().equals(Configuration.CONFIG_FILE_NAME);
    }

    /**
     * <p>
     * selectFileAndShowBackups.
     * </p>
     *
     * @param writable a boolean
     */
    public void selectFileAndShowBackups(boolean writable) {

        currentFileRecord = filesListing.getFileRecordsModel().getRowData();
        fullCurrentConfigFileType = ".".concat(currentFileRecord.getFileType());

        fileInEditionNumber = currentFileRecord.getNumber();
        editable = writable;

        logger.info("fileInEditionNumber: {}; fileName: {}", fileInEditionNumber, currentFileRecord.getFileName());
        refreshBackups(new File(backupsPath + currentFileRecord.getFileName().replaceFirst("[.][^.]+$", "")));

        try {
            openFile();
        } catch (IOException e) {
            logger.trace("IOException caught in the method showBackups(boolean)", e);
        }
    }

    /**
     * <p>
     * showBackups.
     * </p>
     */
    public void showBackups() {
        selectFileAndShowBackups(false);
    }

    /**
     * <p>
     * downloadFile.
     * </p>
     *
     * @param rec {@link io.goobi.viewer.model.administration.configeditor.BackupRecord} for which to download the file
     * @return Navigation outcome
     * @throws java.io.IOException
     */
    public String downloadFile(BackupRecord rec) throws IOException {
        logger.trace("downloadFile: {}", rec != null ? rec.getName() : "null");
        if (rec == null) {
            throw new IllegalArgumentException("rec may not be null");
        }
        if (currentFileRecord == null) {
            return "";
        }

        File backupFile = new File(backupFiles[rec.getNumber()].getAbsolutePath());
        String fileName = currentFileRecord.getFileName().concat(".").concat(rec.getName());

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext ec = facesContext.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("text/".concat(currentFileRecord.getFileType()));
        ec.setResponseHeader("Content-Length", String.valueOf(Files.size(backupFile.toPath())));
        ec.setResponseHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");
        try (OutputStream outputStream = ec.getResponseOutputStream(); FileInputStream fileInputStream = new FileInputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (GetAction.isClientAbort(e)) {
                logger.trace("Download of '{}' aborted: {}", fileName, e.getMessage());
                return "";
            }
            throw e;
        }
        facesContext.responseComplete();
        return "";
    }

    /**
     * Removes file locks for the given session id.
     *
     * @param sessionId a {@link java.lang.String} object
     */
    public static void clearLocksForSessionId(String sessionId) {
        fileLocks.clearLocksForSessionId(sessionId);
    }

    /**
     * <p>
     * getCurrentFileName.
     * </p>
     *
     * @return File name of the currently selected file record row
     */
    public String getCurrentFileName() {
        if (currentFileRecord == null) {
            return "-";
        }

        return currentFileRecord.getFileName();
    }

    /**
     * Getter for the URL pattern.
     *
     * @param fileName a {@link java.lang.String} object
     * @throws java.io.FileNotFoundException
     */
    public void setCurrentFileName(String fileName) throws FileNotFoundException {
        logger.trace("setCurrentFileName: {}", fileName);

        if ("-".equals(fileName)) {
            closeCurrentFileAction();
            return;
        }

        String decodedFileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        if (currentFileRecord != null && currentFileRecord.getFileName().equals(decodedFileName)) {
            return;
        }

        refresh();

        int row = 0;
        for (FileRecord fileRecord : filesListing.getFileRecords()) {
            if (fileRecord.getFileName().equals(decodedFileName)) {
                filesListing.getFileRecordsModel().setRowIndex(row);
                selectFileAndShowBackups(fileRecord.isWritable());
                return;
            }
            row++;
        }

        throw new FileNotFoundException(decodedFileName);
    }

    public FileRecord getRecord(String filename) {
        String decodedFileName = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        return filesListing.getFileRecords()
                .stream()
                .filter(rec -> rec.getFileName().equals(decodedFileName))
                .findAny()
                .orElse(null);
    }

    /**
     * <p>
     * getCurrentFilePath.
     * </p>
     *
     * @return a {@link java.nio.file.Path} object
     */
    public Path getCurrentFilePath() {
        return Optional.ofNullable(currentFileRecord).map(FileRecord::getFile).orElse(null);
    }

    public FileUploader getFileUploader() {
        return fileUploader;
    }
}
