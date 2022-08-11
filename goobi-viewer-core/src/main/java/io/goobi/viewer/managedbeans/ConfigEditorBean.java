package io.goobi.viewer.managedbeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.model.administration.configeditor.BackupRecord;
import io.goobi.viewer.model.administration.configeditor.FileRecord;
import io.goobi.viewer.model.administration.configeditor.FilesListing;

@ManagedBean
@SessionScoped
public class ConfigEditorBean implements Serializable {

    private static final long serialVersionUID = -4120457702630667052L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigEditorBean.class);

    private static final FilesListing filesListing = new FilesListing();

    private static File[] files;
    private static String[] fileNames;
    private static List<FileRecord> fileRecords;
    private static DataModel<FileRecord> fileRecordsModel;
    private static int maxBackups; // maximum number of backup files that can be stored
    private static boolean limitedBackups; // default to be unlimited given non-positive maxBackups
    static {
        files = filesListing.getFiles();
        fileNames = filesListing.getFileNames();
        fileRecords = filesListing.getFileRecords();
        fileRecordsModel = filesListing.getFileRecordsModel();
        maxBackups = filesListing.getMaxBackups();
        limitedBackups = maxBackups > 0;
    }

    // Whether to render the backend or not
    private boolean renderBackend;

    // Fields for FileEdition
    private int fileInEditionNumber;
    private String fileContent;
    private String temp = ""; // Used to check if the content of the textarea is modified

    // ReadOnly or editable
    private boolean editable = false;

    // Fields for Backups
    private List<BackupRecord> backupRecords;
    private DataModel<BackupRecord> backupRecordsModel;
    private File[] backupFiles;
    private String[] backupNames;
    private int backupNumber;
    private String backupsPath; // to maintain the backup files

    // Whether there is anything to download
    private boolean downloadable = false;

    // Fields for File-IO
    private FileInputStream fileInputStream = null;
    private FileOutputStream fileOutputStream = null;
    private FileChannel inputChannel, outputChannel;
    private FileLock inputLock = null, outputLock = null;

    // Whether the opened config file is "config-viewer.xml"
    private boolean isConfigViewer;

    // Used to render the CodeMirror editor properly, values can be "properties" or "xml"
    private String currentConfigFileType;
    private String fullCurrentConfigFileType; // "." + currentConfigFileType

    private boolean nightMode = false;

    public ConfigEditorBean() {
        renderBackend = filesListing.isEnabled();
    }

    @PostConstruct
    public void init() {
        if (!renderBackend) {
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
        backupRecords = new ArrayList<>(Arrays.asList(
                new BackupRecord("No Backup File Found", -1)));
        backupRecordsModel = new ListDataModel<>(backupRecords);
    }

    public boolean isRenderBackend() {
        return renderBackend;
    }

    public DataModel<FileRecord> getFileRecordsModel() {
        return fileRecordsModel;
    }

    public String[] getFileNames() {
        return fileNames;
    }

    public int getFileInEditionNumber() {
        return fileInEditionNumber;
    }

    public void setFileInEditionNumber(int fileInEditionNumber) {
        this.fileInEditionNumber = fileInEditionNumber;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public List<FileRecord> getFileRecords() {
        return fileRecords;
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

    public boolean isDownloadable() {
        return downloadable;
    }

    public void setDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
    }

    public String getCurrentConfigFileType() {
        return currentConfigFileType;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void changeNightMode() {
        nightMode = !nightMode;
    }

    ///////////////////// Hidden Text /////////////////////////
    private String hiddenText = "Hi, how are you doing?";

    public String getHiddenText() {
        return hiddenText;
    }
    ///////////////////////////////////////////////////////////

    public void openFile() throws IOException {

        String pathString = files[fileInEditionNumber].getAbsolutePath();
        Path filePath = Path.of(pathString);
        try {
            fileInputStream = new FileInputStream(pathString);
            inputChannel = fileInputStream.getChannel();

            if (fileOutputStream != null) {
                if (outputLock.isValid()) {
                    outputLock.release();
                }
                fileOutputStream.close();
            }

            // get an exclusive lock if the file is editable, otherwise a shared lock
            if (editable) {
                // outputLock also locks reading this file in Windows, so read it prior to creating the lock
                fileContent = Files.readString(filePath);
                fileOutputStream = new FileOutputStream(pathString, true); // appending instead of covering 
                outputChannel = fileOutputStream.getChannel();
                outputLock = outputChannel.tryLock();
                if (outputLock == null) {
                    throw new OverlappingFileLockException();
                }
            } else { // READ_ONLY
                inputLock = inputChannel.tryLock(0, Long.MAX_VALUE, true);
                if (inputLock == null) {
                    throw new OverlappingFileLockException();
                }
                fileContent = Files.readString(filePath);
            }

            // fileContent = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8.name());
            temp = fileContent;
        } catch (OverlappingFileLockException oe) {
            logger.trace("The region specified is already locked by another process.", oe);

        } catch (IOException e) {
            logger.trace("IOException caught in the method openFile()", e);
        } finally {
            if (inputLock != null && inputLock.isValid()) {
                inputLock.release();
            }
            fileInputStream.close();
        }

        hiddenText = "New File Chosen!";
    }

    public void editFile(boolean writable) {
        showBackups(writable);
    }

    public void saveFile() throws IOException {

        // No need to duplicate if no modification is done.
        if (temp.equals(fileContent)) {
            hiddenText = "No Modification Detected!";
            return;
        }

        // Use the filename without extension to create a folder for its backup_copies.
        String newBackupFolderPath = backupsPath + files[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", "");
        File newBackupFolder = new File(newBackupFolderPath);
        if (!newBackupFolder.exists()) {
            newBackupFolder.mkdir();
        }
        // Save the latest modification to the original path.
        Path originalPath = Path.of(files[fileInEditionNumber].getAbsolutePath());

        try {
            // Files.writeString(originalPath, fileContent, StandardCharsets.UTF_8);
            // In Windows, the exact same stream/channel that holds the outputLock must be used to write to avoid IOException
            IOUtils.write(fileContent, fileOutputStream, StandardCharsets.UTF_8);
            // if the "config-viewer.xml" is being edited, then the original content of the block <configEditor> should be written back
            if (isConfigViewer) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                Document document = documentBuilder.parse(originalPath.toFile());
                document.getDocumentElement().normalize();

                // get the parent node <configEditor>
                Node configEditor = document.getElementsByTagName("configEditor").item(0);

                // set the values of the attributes "enabled" and "maximum" back
                configEditor.getAttributes().getNamedItem("enabled").setNodeValue(renderBackend ? "true" : "false");
                configEditor.getAttributes().getNamedItem("maximum").setNodeValue(String.valueOf(maxBackups));

                // get the list of all <directory> elements
                NodeList directoryList = configEditor.getChildNodes();

                // remove these modified elements
                while (directoryList.getLength() > 0) {
                    Node node = directoryList.item(0);
                    configEditor.removeChild(node);
                }
                // rewrite the backed-up values "configPaths" into this block
                for (String configPath : DataManager.getInstance().getConfiguration().getConfigEditorDirectories()) {
                    Node newNode = document.createElement("directory");
                    newNode.setTextContent(configPath);
                    configEditor.appendChild(document.createTextNode("\n\t"));
                    configEditor.appendChild(newNode);
                }
                configEditor.appendChild(document.createTextNode("\n    "));

                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                DOMSource src = new DOMSource(document);
                StreamResult result = new StreamResult(originalPath.toFile());
                transformer.transform(src, result);

                // save the modified content again
                fileContent = Files.readString(originalPath);

                if (temp.equals(fileContent)) {
                    hiddenText = "No Valid Modification Detected!";
                    return;
                }
            }

            // Use a time stamp to distinguish the backups.
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS").format(new java.util.Date()).replace(":", "").replaceFirst("[.]", "");
            Path newBackupPath = Path.of(newBackupFolderPath + "/" + files[fileInEditionNumber].getName() + "." + timeStamp);
            // save the original content to backup files
            Files.writeString(newBackupPath, temp, StandardCharsets.UTF_8);

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
        } finally {
            if (outputLock != null && outputLock.isValid()) {
                outputLock.release();
            }
            fileOutputStream.close();
        }

        temp = fileContent;

        // refresh the backup metadata
        backupFiles = newBackupFolder.listFiles();
        Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // last modified comes on top

        int length = backupFiles.length;
        if (limitedBackups && length > maxBackups) {
            // remove the oldest backup
            backupFiles[length - 1].delete();
            length -= 1;
        }

        backupNames = new String[length];
        backupRecords = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
            backupRecords.add(new BackupRecord(backupNames[i], i));
        }
        backupRecordsModel = new ListDataModel<>(backupRecords);
        downloadable = true;

        hiddenText = "File saved!";
    }

    public void showBackups() {
        showBackups(false);
    }

    public void showBackups(boolean writable) {
        FileRecord record = fileRecordsModel.getRowData();
        isConfigViewer = record.getFileName().equals("config-viewer.xml"); // Modifications of "config-viewer.xml" should be limited
        currentConfigFileType = record.getFileType();
        fullCurrentConfigFileType = ".".concat(currentConfigFileType);

        fileInEditionNumber = record.getNumber();
        editable = writable;

        logger.info("fileInEditionNumber: {}; fileName: {}", fileInEditionNumber, record.getFileName());

        File backups = new File(backupsPath + files[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", ""));
        if (backups.exists()) {
            backupFiles = backups.listFiles();
            Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // last modified comes on top
            backupNames = new String[backupFiles.length];
            backupRecords = new ArrayList<>();
            for (int i = 0; i < backupFiles.length; ++i) {
                backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
                backupRecords.add(new BackupRecord(backupNames[i], i));
            }
            downloadable = true;

        } else {
            backupFiles = null;
            backupNames = null;
            backupRecords = new ArrayList<>(Arrays.asList(
                    new BackupRecord("No Backup File Found", -1)));
            downloadable = false;
        }
        backupRecordsModel = new ListDataModel<>(backupRecords);

        try {
            openFile();
        } catch (IOException e) {
            logger.trace("IOException caught in the method showBackups(boolean)", e);
        }
    }

    public void downloadFile() throws IOException {
        BackupRecord record = backupRecordsModel.getRowData();
        backupNumber = record.getNumber();
        File backupFile = new File(backupFiles[backupNumber].getAbsolutePath());
        String fileName = fileNames[fileInEditionNumber].concat(".").concat(record.getName());

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext ec = facesContext.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("text/".concat(currentConfigFileType));
        ec.setResponseHeader("Content-Length", String.valueOf(Files.size(backupFile.toPath())));
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        OutputStream outputStream = ec.getResponseOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {

            if (GetAction.isClientAbort(e)) {
                logger.trace("Download of '{}' aborted: {}", fileName, e.getMessage());
                return;
            } else {
                throw e;
            }
        }
        //		outputStream.flush();
        //		outputStream.close();
        facesContext.responseComplete();
        outputStream.close();
        hiddenText = "File downloaded!";
    }

    public void cancelEdition() {
        try {
            openFile();

        } catch (Exception e) {
            logger.trace("Exception caught in cancelEdition()", e);
        }
    }

}
