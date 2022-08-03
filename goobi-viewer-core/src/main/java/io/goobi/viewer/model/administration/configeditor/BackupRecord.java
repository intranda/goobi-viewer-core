package io.goobi.viewer.model.administration.configeditor;


public class BackupRecord {
	
	private String name;
	private int number;
	
	public BackupRecord(String name, int i) {
		this.name = name;
		this.number = i;
	}

	public String getName() {
		return name;
	}

	public int getNumber() {
		return number;
	}
	
}
