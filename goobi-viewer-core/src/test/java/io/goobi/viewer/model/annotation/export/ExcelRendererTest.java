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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.security.user.User;

class ExcelRendererTest {

    private final ExcelRenderer renderer = new ExcelRenderer(new AnnotationConverter(new ApiUrls("https://viewer.goobi.io/api/v1/")));

    static CrowdsourcingAnnotation createAnnotation(long id, String pi, Integer page, String author, String body) {
        CrowdsourcingAnnotation anno = new CrowdsourcingAnnotation();
        anno.setId(id);
        anno.setTargetPI(pi);
        anno.setTargetPageOrder(page);
        anno.setCreator(new User(author));
        anno.setBody(body);
        return anno;
    }

    /**
     * @see ExcelRenderer#render(Map)
     * @verifies throw IllegalArgumentException if annotation map is null
     */
    @Test
    void render_shouldThrowIllegalArgumentExceptionIfAnnotationMapIsNull() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null));
    }

    /**
     * @see ExcelRenderer#render(Map)
     * @verifies throw IllegalArgumentException if annotation map is empty
     */
    @Test
    void render_shouldThrowIllegalArgumentExceptionIfAnnotationMapIsEmpty() {
        Map<String, List<CrowdsourcingAnnotation>> empty = Collections.emptyMap();
        assertThrows(IllegalArgumentException.class, () -> renderer.render(empty));
    }

    /**
     * @see ExcelRenderer#render(Map)
     * @verifies create one sheet per map entry named after the key
     */
    @Test
    void render_shouldCreateOneSheetPerMapEntryNamedAfterTheKey() throws IOException {
        Map<String, List<CrowdsourcingAnnotation>> map = new LinkedHashMap<>();
        map.put("TextualBody", List.of(createAnnotation(1L, "PPN1", 1, "alice", "first")));
        map.put("Unknown", List.of(createAnnotation(2L, "PPN2", 2, "bob", null)));

        try (XSSFWorkbook wb = renderer.render(map)) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("TextualBody", wb.getSheetName(0));
            assertEquals("Unknown", wb.getSheetName(1));
        }
    }

    /**
     * @see ExcelRenderer#render(Map)
     * @verifies write header row and one data row per annotation
     */
    @Test
    void render_shouldWriteHeaderRowAndOneDataRowPerAnnotation() throws IOException {
        Map<String, List<CrowdsourcingAnnotation>> map = Map.of("TextualBody",
                List.of(createAnnotation(42L, "PPN123", 5, "alice", "some text"), createAnnotation(43L, "PPN456", null, "bob", "more text")));

        try (XSSFWorkbook wb = renderer.render(map)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals(3, sheet.getPhysicalNumberOfRows());

            XSSFRow header = sheet.getRow(0);
            String[] expectedHeader = { "ID", "in", "on page", "Campaign", "Author", "values" };
            for (int i = 0; i < expectedHeader.length; i++) {
                assertEquals(expectedHeader[i], header.getCell(i).getStringCellValue());
            }

            XSSFRow first = sheet.getRow(1);
            assertEquals("42", first.getCell(0).getStringCellValue());
            assertEquals("PPN123", first.getCell(1).getStringCellValue());
            assertEquals("5", first.getCell(2).getStringCellValue());
            assertEquals("", first.getCell(3).getStringCellValue());
            assertEquals("alice", first.getCell(4).getStringCellValue());
            assertEquals("some text", first.getCell(5).getStringCellValue());

            XSSFRow second = sheet.getRow(2);
            assertEquals("43", second.getCell(0).getStringCellValue());
            assertEquals("", second.getCell(2).getStringCellValue());
            assertEquals("more text", second.getCell(5).getStringCellValue());
        }
    }

    /**
     * @see ExcelRenderer#render(Map)
     * @verifies apply bold font to header cells and regular font to data cells
     */
    @Test
    void render_shouldApplyBoldFontToHeaderCellsAndRegularFontToDataCells() throws IOException {
        Map<String, List<CrowdsourcingAnnotation>> map = Map.of("TextualBody", List.of(createAnnotation(1L, "PPN1", 1, "alice", "text")));

        try (XSSFWorkbook wb = renderer.render(map)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            for (int i = 0; i < 6; i++) {
                assertNotNull(sheet.getRow(0).getCell(i).getCellStyle());
                assertTrue(sheet.getRow(0).getCell(i).getCellStyle().getFont().getBold(), "header cell " + i + " should be bold");
                assertFalse(sheet.getRow(1).getCell(i).getCellStyle().getFont().getBold(), "data cell " + i + " should not be bold");
            }
        }
    }
}
