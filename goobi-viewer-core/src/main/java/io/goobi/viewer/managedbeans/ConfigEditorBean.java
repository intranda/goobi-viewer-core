package io.goobi.viewer.managedbeans;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.administration.configeditor.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
//import javax.swing.JFileChooser;
//import javax.swing.JOptionPane;
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



@ManagedBean
@SessionScoped
public class ConfigEditorBean implements Serializable{

	private static final long serialVersionUID = 1L;
//	private static final String installationPath = "/home/zehong/work/test/JSF-fileIO/config/"; // should be changed to Goobi-Viewer installation path
	private static final String installationPath = DataManager.getInstance().getConfiguration().getConfigLocalPath(); 
	private static final Logger logger = LoggerFactory.getLogger(ConfigEditorBean.class);
	
	private static final FilesListing filesListing = new FilesListing();
	
	private static File[] files;
	private static String[] fileNames;
	private static ArrayList<FileRecord> fileRecords;
	private static DataModel<FileRecord> fileRecordsModel;
	private static List<String> configPaths; // paths where the editable / viewable config files live, which should not be modified
	static {
		files = filesListing.getFiles();
		fileNames = filesListing.getFileNames();
		fileRecords = filesListing.getFileRecords();
		fileRecordsModel = filesListing.getFileRecordsModel();
		configPaths = filesListing.getConfigPaths();
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
	private ArrayList<BackupRecord> backupRecords;
	private DataModel<BackupRecord> backupRecordsModel;
	private File[] backupFiles;
	private String[] backupNames;
	private int backupNumber;
	private String backupsPath; // to maintain the backup files

	// Whether there is anything to download
	private boolean downloadable = false;
	
	// Fields for File-IO
	private FileInputStream fileInputStream;
	private FileOutputStream fileOutputStream;
	private FileChannel inputChannel, outputChannel;
	private FileLock inputLock, outputLock;
	
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
//			
//			JOptionPane.showMessageDialog(null,
//					"Der Konfig-Editor ist deaktiviert!",
//					"Warnung",
//					JOptionPane.INFORMATION_MESSAGE);
			System.out.println("Der Konfig Editor ist deaktiviert!");
			return;
		}

		// Create the folder "backups" if necessary.
		backupsPath = installationPath + "backups/";
		File backups = new File(backupsPath);
		if (!backups.exists()) {
			backups.mkdir();
		}
		
		// Initialize Backup List 
		backupFiles = null;
		backupNames = null;
		backupRecords = new ArrayList<BackupRecord> (Arrays.asList(
			new BackupRecord("No Backup File Found", -1)));
		backupRecordsModel = new ListDataModel<BackupRecord>(backupRecords);
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

	public ArrayList<FileRecord> getFileRecords() {
		return fileRecords;
	}
			
	public ArrayList<BackupRecord> getBackupRecords() {
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
		System.out.println(nightMode ? "night" : "day");
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
				fileOutputStream.close();
			}
			
			// get an exclusive lock if the file is editable, otherwise a shared lock
			if (editable) {
				fileOutputStream = new FileOutputStream(pathString, true); // appending instead of covering 
				outputChannel = fileOutputStream.getChannel();
				outputLock = outputChannel.lock();
			} else { // READ_ONLY
				inputLock = inputChannel.lock(0, Long.MAX_VALUE, true);
			}
			
			fileContent = Files.readString(filePath);
			temp = fileContent;
		} catch(OverlappingFileLockException oe) {
			System.out.println("The region specified is already locked by another file.");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			fileInputStream.close();
		}
		
		hiddenText = "New File Chosen!";
	}

	public void editFile(boolean writable) {
		showBackups(writable);
	}
	
	public void saveFile() throws IOException {
		System.out.println("Save Clicked!");
		
		// No need to duplicate if no modification is done.
		if (temp.equals(fileContent)) {
			hiddenText = "No Modification Detected!";
			return;
		}
				
		// Use the filename without extension to create a folder for its backup_copies.
		String path = backupsPath + files[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", "");
		File backupFolder = new File(path);
		if (!backupFolder.exists()) {
			backupFolder.mkdir();
		}
		// Save the latest modification to the original path.
		Path originalPath = Path.of(files[fileInEditionNumber].getAbsolutePath());
		// Use a time stamp to distinguish the backups.
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss").format(new java.util.Date());
		Path filePath = Path.of(path + "/" + files[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", "") + "_" + timeStamp + fullCurrentConfigFileType);
		try {
			Files.writeString(originalPath, fileContent, StandardCharsets.UTF_8);
			// if the "config-viewer.xml" is being edited, then the original content of the block <configEditor> should be written back
			if (isConfigViewer) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
				Document document = documentBuilder.parse(originalPath.toFile());
				document.getDocumentElement().normalize();
				
				// get the parent node <configEditor>
				Node configEditor = document.getElementsByTagName("configEditor").item(0);
				
				// set the attribute "enabled"
				configEditor.getAttributes().item(0).setNodeValue(renderBackend ? "true" : "false");
				
				// get the list of all <directory> elements
				NodeList directoryList = configEditor.getChildNodes();
				
				// remove these modified elements
				while (directoryList.getLength() > 0) {
					Node node = directoryList.item(0);
					configEditor.removeChild(node);
				}
				// rewrite the backed-up values "configPaths" into this block
				for (String configPath : configPaths) {
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
			
			}
			Files.writeString(filePath, fileContent, StandardCharsets.UTF_8); // save changes to backup files
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			fileOutputStream.close();
		}
				
		temp = fileContent;
		
		// refresh the backup metadata
		backupFiles = backupFolder.listFiles();
		Arrays.sort(backupFiles, (a,b) -> Long.compare(a.lastModified(), b.lastModified()));
		backupNames = new String[backupFiles.length];
		backupRecords = new ArrayList<BackupRecord>();
		for (int i = 0; i < backupFiles.length; ++i) {
			backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
			backupRecords.add(new BackupRecord(backupNames[i], i));
		}
		backupRecordsModel = new ListDataModel<BackupRecord>(backupRecords);
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
		System.out.println("fileInEditionNumber: " + fileInEditionNumber + " " + record.getFileName());
		System.out.println(fileRecordsModel.getRowIndex());
		File backups = new File(backupsPath + files[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", "")); 
		if (backups.exists()) {
			backupFiles = backups.listFiles();
			Arrays.sort(backupFiles, (a,b) -> Long.compare(a.lastModified(), b.lastModified()));
			backupNames = new String[backupFiles.length];
			backupRecords = new ArrayList<BackupRecord>();
			for (int i = 0; i < backupFiles.length; ++i) {
				backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
				backupRecords.add(new BackupRecord(backupNames[i], i));
			}
			downloadable = true;
			
		} else {
			backupFiles = null;
			backupNames = null;
			backupRecords = new ArrayList<BackupRecord> (Arrays.asList(
				new BackupRecord("No Backup File Found", -1)));
			downloadable = false;
		}
		backupRecordsModel = new ListDataModel<BackupRecord>(backupRecords);
		
		try {
			openFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
/*	
// The following method saves everything into the "Downloads" folder.
//
	public void downloadFile() {
		String downloads = System.getProperty("user.home") + "/Downloads/";
		
		BackupRecord record = backupRecordsModel.getRowData();
		backupNumber = record.getNumber();

		try {
//			String absPath = backupFiles[backupNumber].getAbsolutePath();
			InputStream in = new FileInputStream(new File(backupFiles[backupNumber].getAbsolutePath()));
//			Files.copy(in,  Paths.get(backupFiles[backupNumber].getName()), StandardCopyOption.REPLACE_EXISTING);

			Files.copy(in,  Paths.get(downloads + fileNames[fileInEditionNumber].replaceFirst("[.][^.]+$", "").replaceFirst(fullCurrentConfigFileType, "") + "_" + record.getName() + fullCurrentConfigFileType), StandardCopyOption.REPLACE_EXISTING);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println(downloads+record.getName());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		hiddenText = "File downloaded!";
	}
*/
/*	
	public void downloadFile() {
		String downloads = System.getProperty("user.home") + "/Downloads/";
		
		BackupRecord record = backupRecordsModel.getRowData();
		backupNumber = record.getNumber();
		
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(downloads));
		chooser.setDialogTitle("Select folder to save");
		chooser.setDialogType(JFileChooser.SAVE_DIALOG);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.showSaveDialog(null);
		
//		System.out.println(chooser.getSelectedFile());
//		System.out.println(chooser.getSelectedFile().getClass().getName()); // java.io.File
		
		String storagePath = chooser.getSelectedFile().toString();
		
		try {
			InputStream in = new FileInputStream(new File(backupFiles[backupNumber].getAbsolutePath()));
			Files.copy(in,  Paths.get(storagePath + "/" + fileNames[fileInEditionNumber].replaceFirst("[.][^.]+$", "").replaceFirst(fullCurrentConfigFileType, "") + "_" + record.getName() + fullCurrentConfigFileType), StandardCopyOption.REPLACE_EXISTING);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		hiddenText = "File downloaded!";
	}
*/
	
	public void downloadFile() throws IOException {
		BackupRecord record = backupRecordsModel.getRowData();
		backupNumber = record.getNumber();
		File backupFile = new File(backupFiles[backupNumber].getAbsolutePath());
		String fileName = fileNames[fileInEditionNumber].replaceFirst("[.][^.]+$", "").replaceFirst(fullCurrentConfigFileType, "") + "_" + record.getName() + fullCurrentConfigFileType;
		
		System.out.println("fileName is " + fileName);
		
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext ec = facesContext.getExternalContext();
		ec.responseReset();
		ec.setResponseContentType("text/" + currentConfigFileType);
		
		System.out.println(currentConfigFileType);
		
		ec.setResponseHeader("Content-Length", String.valueOf(Files.size(backupFile.toPath())));
		
		System.out.println(Files.size(backupFile.toPath()));
		
		ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		OutputStream outputStream = ec.getResponseOutputStream();
		
		System.out.println("outputStream opened!");
		
		try(FileInputStream fileInputStream = new FileInputStream(backupFile)){
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				System.out.println(buffer);
			}
		} catch (IOException e) {
			
			if (GetAction.isClientAbort(e)) {
				//hiddenText = String.format("Download of '%s' aborted: %s", fileName, e.getMessage());
				System.out.println(String.format("Download of '%s' aborted: %s", fileName, e.getMessage()));
				logger.trace("Download of '{}' aborted: {}", fileName, e.getMessage());
				return;
			} else {
				throw e;
			}
//			e.printStackTrace();
		}
		System.out.println("After catch");
//		outputStream.flush();
//		outputStream.close();
		facesContext.responseComplete();
		hiddenText = "File downloaded!";
		System.out.println(hiddenText);
	}
	
	public void cancelEdition() {
		try {
			System.out.println("Cancel clicked!");
			openFile();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


























