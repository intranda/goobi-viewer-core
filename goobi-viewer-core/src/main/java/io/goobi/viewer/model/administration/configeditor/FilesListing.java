package io.goobi.viewer.model.administration.configeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;


public class FilesListing implements Serializable{

	private static final long serialVersionUID = 1L;

	private static final String configFile = DataManager.getInstance().getConfiguration().getConfigLocalPath() + "config_viewer.xml"; 
	
	private static List<String> configPaths; // paths where the editable/viewable config files live
	private static boolean enabled;
	private static int maxBackups;
	static {
		configPaths = new ArrayList<String>();
		// Get information from "config-viewer.xml"
		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			InputStream in = new FileInputStream(configFile);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					String elementName = startElement.getName().getLocalPart();
					if (elementName != "configEditor") {
						continue; // keep looking for the entrance of configEditor
					}
					// get the attribute value of configEditor - "enabled"
					Attribute attrEnabled = startElement.getAttributeByName(new QName("enabled"));
					enabled = attrEnabled.getValue().toLowerCase().equals("true");
					
					// get the maximum number of backups
					Attribute attrMaximum = startElement.getAttributeByName(new QName("maximum"));
					maxBackups = (attrMaximum == null) ? 0 : Integer.parseInt(attrMaximum.getValue().toString());
					System.out.println(maxBackups);

					// Block entrance found, "enabled" assigned, time for another loop
					break;		
				}
			}
			// No need to bother if it is disabled
			if (enabled) {
				while (eventReader.hasNext()) {
					XMLEvent event = eventReader.nextEvent();
					
					if (event.isStartElement()) {
						StartElement startElement = event.asStartElement();
						String elementName = startElement.getName().getLocalPart();
						if (elementName != "directory") {
							continue; 
						} else {
							event = eventReader.nextEvent();
							configPaths.add(event.asCharacters().getData());
//							configPaths.add(FileTools.adaptPathForWindows(event.asCharacters().getData()));
						}
					}
					if (event.isEndElement()) {
						EndElement endElement = event.asEndElement();
						if (endElement.getName().getLocalPart().equals("configEditor")) {
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private File[] files = null;
	private String[] fileNames = null;
	private ArrayList<FileRecord> fileRecords = null;
	private DataModel<FileRecord> fileRecordsModel = null;
	

	public FilesListing() {
		// No need to bother if it is disabled
		if (enabled) {
			fileRecords = new ArrayList<FileRecord>();
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File f, String name) {
					return name.endsWith(".xml") || name.endsWith(".properties");
				}
			};
			
			files = new File[0];
			
			for (String configPath : configPaths) {
				File f = new File(FileTools.adaptPathForWindows(configPath));
				files = Stream.concat(Arrays.stream(files), Arrays.stream(f.listFiles(filter))).toArray(File[] :: new);
			}
			
			Arrays.sort(files, (a,b) -> a.getName().compareTo(b.getName()));
			fileNames = new String[files.length];
			for (int i = 0; i < files.length; ++i) {
				fileNames[i] = files[i].getName();
				fileRecords.add(new FileRecord(fileNames[i], i, files[i].canRead(), files[i].canWrite()));
			}
			
			fileRecordsModel = new ListDataModel<FileRecord>(fileRecords);
		}
	}

	public File[] getFiles() {
		return files;
	}

	public String[] getFileNames() {
		return fileNames;
	}

	public ArrayList<FileRecord> getFileRecords() {
		return fileRecords;
	}

	public DataModel<FileRecord> getFileRecordsModel() {
		return fileRecordsModel;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public List<String> getConfigPaths() {
		return configPaths;
	}

	public int getMaxBackups() {
		return maxBackups;
	}
	
	

}
