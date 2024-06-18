package io.goobi.viewer.model.files.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javax.servlet.http.Part;

public class FileUploader {

    private Part file = null;
    private Path downloadPath = null;
    private byte[] fileContents = null;
    private Exception error = null;

    public void upload() {
        if (file != null) {
            try (InputStream input = file.getInputStream()) {
                fileContents = input.readAllBytes();
            } catch (IOException e) {
                this.error = e;
            }
        }
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part uploadedFile) {
        this.file = uploadedFile;
    }

    public byte[] getFileContents() {
        return fileContents;
    }

    public boolean isError() {
        return this.error != null;
    }

    public boolean isUploaded() {
        return fileContents != null;
    }

    public Exception getError() {
        return this.error;
    }

    public Path getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    public boolean isReadoForUpload() {
        return file != null;
    }

}
