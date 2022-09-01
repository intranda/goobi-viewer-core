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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.configeditor.BackupRecord;
import io.goobi.viewer.model.administration.configeditor.FileLocks;
import io.goobi.viewer.model.administration.configeditor.FileRecord;
import io.goobi.viewer.model.administration.configeditor.FilesListing;

@Named
@SessionScoped
public class AdminConfigEditorBean implements Serializable {

    private static final long serialVersionUID = -4120457702630667052L;

    private static final Logger logger = LoggerFactory.getLogger(AdminConfigEditorBean.class);

    /** Manual edit locks for files. */
    private static final FileLocks fileLocks = new FileLocks();

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
    private List<BackupRecord> backupRecords;
    private transient DataModel<BackupRecord> backupRecordsModel;
    private File[] backupFiles;
    private String[] backupNames;
    private String backupsPath; // to maintain the backup files

    // Fields for File-IO
    //    private transient FileOutputStream fileOutputStream = null;
    private transient FileLock inputLock = null;
    //    private transient FileLock outputLock = null;

    // Whether the opened config file is "config_viewer.xml"
    private boolean isConfigViewer;

    // Used to render the CodeMirror editor properly, values can be "properties" or "xml"
    private String fullCurrentConfigFileType; // "." + currentConfigFileType

    private boolean nightMode = false;

    public AdminConfigEditorBean() {
        //
    }

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

    public boolean isRenderBackend() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    public void refresh() {
        filesListing.refresh();
    }

    public DataModel<FileRecord> getFileRecordsModel() {
        return filesListing.getFileRecordsModel();
    }

    public int getFileInEditionNumber() {
        return fileInEditionNumber;
    }

    public void setFileInEditionNumber(int fileInEditionNumber) {
        this.fileInEditionNumber = fileInEditionNumber;
    }
    
    /**
     * @return the currentFileRecord
     */
    public FileRecord getCurrentFileRecord() {
        return currentFileRecord;
    }

    /**
     * @return the currentFileRecord
     */
    public FileRecord getCurrentFileRecord() {
        return currentFileRecord;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public List<BackupRecord> getBackupRecords() {
        return backupRecords;
    }

    public DataModel<BackupRecord> getBackupRecordsModel() {
        return backupRecordsModel;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isBackupsAvailable() {
        return !backupRecords.isEmpty();
    }

    public String getCurrentConfigFileType() {
        if (currentFileRecord != null) {
            return currentFileRecord.getFileType();
        }

        return "";
    }

    /**
     * Determines whether the given fileRecord is locked by a different user session.
     * 
     * @param fileRecord
     * @return true if file path locked by other session id; false otherwise
     */
    public boolean isFileLocked(FileRecord fileRecord) {
        if (fileRecord == null) {
            return false;
        }

        return fileLocks.isFileLockedByOthers(fileRecord.getFile(), BeanUtils.getSession().getId());
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void changeNightMode() {
        nightMode = !nightMode;
    }

    /**
     * 
     * @throws IOException
     */
    public synchronized void openFile() throws IOException {
        if (fileInEditionNumber < 0) {
            return;
        }

        String sessionId = BeanUtils.getSession().getId();
        if (currentFileRecord != null) {
            fileLocks.unlockFile(currentFileRecord.getFile(), sessionId);
        }

        currentFileRecord = filesListing.getFileRecords().get(fileInEditionNumber);

        Path filePath = currentFileRecord.getFile();

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            FileChannel inputChannel = fis.getChannel();

            // Release write lock
            //            if (fileLocks.containsKey(filePath) && fileLocks.get(filePath).equals(sessionId)) {
            //                fileLocks.remove(filePath);
            //            }

            //            if (fileOutputStream != null) {
            //                if (outputLock.isValid()) {
            //                    outputLock.release();
            //                }
            //                fileOutputStream.close();
            //            }

            // get an exclusive lock if the file is editable, otherwise a shared lock
            if (editable) {

                // File already locked by someone else
                if (fileLocks.isFileLockedByOthers(filePath, sessionId)) {
                    Messages.error("admin__config_editor__file_locked_msg");
                    return;
                }
                fileLocks.lockFile(filePath, sessionId);
                logger.trace("{} locked for session ID {}", filePath.toAbsolutePath(), sessionId);
                // outputLock also locks reading this file in Windows, so read it prior to creating the lock
                fileContent = Files.readString(filePath);
                //                fileOutputStream = new FileOutputStream(pathString, false); // appending instead of covering 
                //                outputLock = fileOutputStream.getChannel().tryLock();
                //                if (outputLock == null) {
                //                    throw new OverlappingFileLockException();
                //                }
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
     * 
     * @return
     */
    public String closeCurrentFileAction() {
        if (currentFileRecord == null) {
            return "";
        }

        fileLocks.unlockFile(currentFileRecord.getFile(), BeanUtils.getSession().getId());
        //            if (outputLock != null && outputLock.isValid()) {
        //                outputLock.release();
        //            }
        //            fileOutputStream.close();

        fileInEditionNumber = -1;
        currentFileRecord = null;

        refresh();

        return "pretty:adminConfigEditor";
    }

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
            Files.writeString(originalPath, fileContent, StandardCharsets.UTF_8);
            // In Windows, the exact same stream/channel that holds the outputLock must be used to write to avoid IOException
            //            IOUtils.write(fileContent, fileOutputStream, StandardCharsets.UTF_8);
            // if the "config_viewer.xml" is being edited, then the original content of the block <configEditor> should be written back
            if (isConfigViewer) {
                logger.debug("Saving {}, changes to config editor settings will be reverted...", Configuration.CONFIG_FILE_NAME);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                
                DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                Document document = documentBuilder.parse(originalPath.toFile());
                document.getDocumentElement().normalize();

                // get the parent node <configEditor>
                NodeList configEditorList = document.getElementsByTagName("configEditor");
                if (configEditorList != null) {
                    Node configEditor = document.getElementsByTagName("configEditor").item(0);

                    // set the values of the attributes "enabled" and "maximum" back
                    if (configEditor.getAttributes().getNamedItem("enabled") != null) {
                        boolean origConfigEditorEnabled = DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
                        configEditor.getAttributes().getNamedItem("enabled").setNodeValue(String.valueOf(origConfigEditorEnabled));
                    }
                    if (configEditor.getAttributes().getNamedItem("maximum") != null) {
                        int origConfigEditorMax = DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups();
                        configEditor.getAttributes().getNamedItem("maximum").setNodeValue(String.valueOf(origConfigEditorMax));
                    }

                    // get the list of all <directory> elements
                    NodeList directoryList = configEditor.getChildNodes();

                    // remove these modified elements
                    while (directoryList.getLength() > 0) {
                        Node node = directoryList.item(0);
                        configEditor.removeChild(node);
                    }
                    // rewrite the backed-up values "configPaths" into this block
                    List<String> origConfigEditorDirectories = DataManager.getInstance().getConfiguration().getConfigEditorDirectories();
                    for (String configPath : origConfigEditorDirectories) {
                        Node newNode = document.createElement("directory");
                        newNode.setTextContent(configPath);
                        configEditor.appendChild(document.createTextNode("\n\t"));
                        configEditor.appendChild(newNode);
                    }
                    configEditor.appendChild(document.createTextNode("\n    "));
                }

                TransformerFactory tf = TransformerFactory.newInstance();
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                Transformer transformer = tf.newTransformer();
                DOMSource src = new DOMSource(document);
                StreamResult result = new StreamResult(originalPath.toFile());
                transformer.transform(src, result);

                // save the modified content again
                fileContent = Files.readString(originalPath);

                if (unmodifiledFileContent.equals(fileContent)) {
                    return "";
                }
            }

            // Use the filename without extension to create a folder for its backup_copies.
            String newBackupFolderPath = backupsPath + currentFileRecord.getFileName().replaceFirst("[.][^.]+$", "");
            File newBackupFolder = new File(newBackupFolderPath);
            if (!newBackupFolder.exists()) {
                newBackupFolder.mkdir();
            }
            createBackup(newBackupFolderPath, currentFileRecord.getFileName(), unmodifiledFileContent);
            refreshBackups(newBackupFolder);
        } catch (IOException e) {
            logger.trace("IOException caught in the method saveFile()", e);
        } catch (SAXException e) {
            logger.trace("SAXException caught in the method saveFile()", e);
        } catch (ParserConfigurationException e) {
            logger.trace("ParserConfigurationException caught in the method saveFile()", e);
        } catch (TransformerConfigurationException e) {
            logger.trace("TransformerConfigurationException caught in the method saveFile()", e);
        } catch (TransformerException e) {
            logger.trace("TransformerException caught in the method saveFile()", e);
        }

        unmodifiledFileContent = fileContent;

        Messages.info("updatedSuccessfully");
        return "";
    }

    /**
     * Creates a timestamped backup of the given file name and content.
     * 
     * @param backupFolderPath Backup folder path
     * @param backup File name root
     * @param content File content
     * @throws IOException
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
        String timeStamp = DateTools.format(LocalDateTime.now(), DateTools.formatterFileName, false);
        Path newBackupPath = Path.of(backupFolderPath, fileName + "." + timeStamp);
        // save the original content to backup files
        Files.writeString(newBackupPath, content, StandardCharsets.UTF_8);
    }

    /**
     * 
     * @param backupFolder
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
                Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // last modified comes on top
            }

            // Trim old backup files, if so configured
            if (DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups() > 0) {
                while (length > DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups()) {
                    try {
                        Files.delete(backupFiles[length--].toPath());
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
                backupFiles = backupFolder.listFiles();
            }

            backupNames = new String[length];
            for (int i = 0; i < length; ++i) {
                backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
                backupRecords.add(new BackupRecord(backupNames[i], i));
            }
        } else {
            backupFiles = null;
            backupNames = null;
        }

        backupRecordsModel = new ListDataModel<>(backupRecords);
    }

    /**
     * 
     * @param writable
     */
    public void selectFileAndShowBackups(boolean writable) {
        currentFileRecord = filesListing.getFileRecordsModel().getRowData();
        isConfigViewer = currentFileRecord.getFileName().equals(Configuration.CONFIG_FILE_NAME); // Modifications of "config_viewer.xml" should be limited
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

    public void showBackups() {
        selectFileAndShowBackups(false);
    }

    /**
     * @param rec {@link BackupRecord} for which to download the file
     * @return Navigation outcome
     * @throws IOException
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
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
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
     * @param sessionId
     */
    public static void clearLocksForSessionId(String sessionId) {
        fileLocks.clearLocksForSessionId(sessionId);
    }

    /**
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
     * @param fileName
     * @throws FileNotFoundException
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
}
