package io.goobi.viewer.model.export;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class ExcelExport {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExport.class);

    private SXSSFWorkbook workbook;
    private String fileName;

    /**
     * @param os
     * @return True if successful; false otherwise
     */
    public boolean writeToResponse(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException("os may not be null");
        }
        if (workbook == null) {
            logger.warn("No workbook set, cannot write.");
            return false;
        }

        try {
            workbook.write(os);
            if (Thread.interrupted()) {
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public void close() {
        if (workbook != null) {
            workbook.dispose();
        }
    }

    /**
     * @return the workbook
     */
    public SXSSFWorkbook getWorkbook() {
        return workbook;
    }

    /**
     * @param workbook the workbook to set
     * @return this
     */
    public ExcelExport setWorkbook(SXSSFWorkbook workbook) {
        this.workbook = workbook;
        return this;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     * @return this
     */
    public ExcelExport setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

}
