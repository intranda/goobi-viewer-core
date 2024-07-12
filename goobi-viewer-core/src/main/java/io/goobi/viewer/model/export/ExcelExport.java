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
package io.goobi.viewer.model.export;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 */
public class ExcelExport {

    private static final Logger logger = LogManager.getLogger(ExcelExport.class);

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
            return Thread.interrupted();
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
            return false;
        }
    }

    public void close() throws IOException {
        if (workbook != null) {
            workbook.close();
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
