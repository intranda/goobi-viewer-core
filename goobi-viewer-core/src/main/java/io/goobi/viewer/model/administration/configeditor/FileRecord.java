package io.goobi.viewer.model.administration.configeditor;


public class FileRecord {

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
		return name.substring(lastIndex+1);
	}

	public String getFileName() {
//		System.out.println("getFileName()");
		return fileName;
	}

	public int getNumber() {
//		System.out.println("getNumber()");
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
