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
package io.goobi.viewer.model.annotation.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;

class AnnotationSheetWriterTest extends AbstractTest {

    private static XSSFWorkbook writeAndReadBack(List<CrowdsourcingAnnotation> annotations) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new AnnotationSheetWriter().createExcelSheet(os, annotations);
        return new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()));
    }

    /**
     * @see AnnotationSheetWriter#createExcelSheet(java.io.OutputStream, List)
     * @verifies write workbook that can be read back as xlsx
     */
    @Test
    void createExcelSheet_shouldWriteWorkbookThatCanBeReadBackAsXlsx() throws IOException {
        List<CrowdsourcingAnnotation> annotations = List.of(ExcelRendererTest.createAnnotation(7L, "PPN789", 3, "carol", "Grüße & <Zeichen>"));

        try (XSSFWorkbook wb = writeAndReadBack(annotations)) {
            assertEquals(1, wb.getNumberOfSheets());
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals(2, sheet.getPhysicalNumberOfRows());
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("7", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("PPN789", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("3", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("carol", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("Grüße & <Zeichen>", sheet.getRow(1).getCell(5).getStringCellValue());
            assertTrue(sheet.getRow(0).getCell(0).getCellStyle().getFont().getBold());
        }
    }

    /**
     * @see AnnotationSheetWriter#createExcelSheet(java.io.OutputStream, List)
     * @verifies group annotations into sheets by body type
     */
    @Test
    void createExcelSheet_shouldGroupAnnotationsIntoSheetsByBodyType() throws IOException {
        List<CrowdsourcingAnnotation> annotations = List.of(
                ExcelRendererTest.createAnnotation(1L, "PPN1", 1, "alice", "first text"),
                ExcelRendererTest.createAnnotation(2L, "PPN2", 2, "bob", null),
                ExcelRendererTest.createAnnotation(3L, "PPN3", 3, "carol", "second text"));

        try (XSSFWorkbook wb = writeAndReadBack(annotations)) {
            assertEquals(2, wb.getNumberOfSheets());

            XSSFSheet textual = wb.getSheet("TextualBody");
            assertNotNull(textual);
            assertEquals(3, textual.getPhysicalNumberOfRows());
            assertEquals("1", textual.getRow(1).getCell(0).getStringCellValue());
            assertEquals("3", textual.getRow(2).getCell(0).getStringCellValue());

            XSSFSheet unknown = wb.getSheet(AnnotationSheetWriter.UNKNOWN_RESOURCE_TYPE);
            assertNotNull(unknown);
            assertEquals(2, unknown.getPhysicalNumberOfRows());
            assertEquals("2", unknown.getRow(1).getCell(0).getStringCellValue());
        }
    }
}
