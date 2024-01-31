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

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Dataset;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.TypedResource;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;

public class ExcelRenderer {

    private static final Logger logger = LogManager.getLogger(ExcelRenderer.class);

    private final AnnotationConverter annotationConverter;

    /**
     * @param annotationConverter
     */
    public ExcelRenderer(AnnotationConverter annotationConverter) {
        this.annotationConverter = annotationConverter;
    }

    /**
     * 
     * @param annotationMap
     * @return {@link XSSFWorkbook}
     */
    public XSSFWorkbook render(Map<String, List<CrowdsourcingAnnotation>> annotationMap) {
        if (annotationMap == null) {
            throw new IllegalArgumentException("No annotations given");
        }

        if (annotationMap.size() == 0) {
            throw new IllegalArgumentException("Empty annotations map");
        }

        XSSFWorkbook wb = new XSSFWorkbook();

        for (Entry<String, List<CrowdsourcingAnnotation>> entry : annotationMap.entrySet()) {
            XSSFSheet sheet = wb.createSheet(entry.getKey());
            sheet.setDefaultColumnWidth(30);
            short height = 300;
            sheet.setDefaultRowHeight(height);
            createHeaderRow(sheet);
            List<CrowdsourcingAnnotation> annotations = entry.getValue();
            createDataRows(sheet, annotations);
        }

        return wb;
    }

    /**
     * @param sheet
     * @param annotations
     */
    public void createDataRows(XSSFSheet sheet, List<CrowdsourcingAnnotation> annotations) {
        int rowCounter = 1;
        for (CrowdsourcingAnnotation annotation : annotations) {
            try {
                createDataRow(annotation, sheet, rowCounter);
            } catch (DAOException e) {
                logger.error("Error creating data row for annotation {}", annotation);
            }
            rowCounter++;
        }
    }

    /**
     * @param annotation
     * @param sheet
     * @param rowCounter
     * @throws DAOException
     */
    public void createDataRow(CrowdsourcingAnnotation annotation, XSSFSheet sheet, int rowCounter) throws DAOException {
        XSSFRow row = sheet.createRow(rowCounter);
        row.setRowStyle(getDataCellStyle(sheet.getWorkbook()));
        XSSFCell idCell = row.createCell(0, CellType.STRING);
        idCell.setCellValue(annotation.getId().toString());
        XSSFCell recordCell = row.createCell(1, CellType.STRING);
        recordCell.setCellValue(annotation.getTargetPI());
        XSSFCell pageCell = row.createCell(2, CellType.STRING);
        String pageOrder = Optional.ofNullable(annotation.getTargetPageOrder()).map(Object::toString).orElse("");
        pageCell.setCellValue(pageOrder);
        XSSFCell campaignCell = row.createCell(3, CellType.STRING);
        campaignCell.setCellValue(Optional.ofNullable(annotation.getGenerator()).map(Question::getOwner).map(Campaign::getTitle).orElse(""));
        XSSFCell authorCell = row.createCell(4, CellType.STRING);
        authorCell.setCellValue(Optional.ofNullable(annotation.getCreator()).map(User::getDisplayName).orElse(""));
        XSSFCell bodyCell = row.createCell(5, CellType.STRING);
        bodyCell.setCellValue(getBodyValues(annotation).get(0));

        setCellStyles(row, getDataCellStyle(sheet.getWorkbook()));

    }

    /**
     * @param sheet
     */
    public void createHeaderRow(XSSFSheet sheet) {
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.setRowStyle(getHeaderCellStyle(sheet.getWorkbook()));
        XSSFCell idCell = titleRow.createCell(0, CellType.STRING);
        idCell.setCellStyle(getHeaderCellStyle(sheet.getWorkbook()));
        idCell.setCellValue("ID");
        XSSFCell recordCell = titleRow.createCell(1, CellType.STRING);
        recordCell.setCellValue("in");
        XSSFCell pageCell = titleRow.createCell(2, CellType.STRING);
        pageCell.setCellValue("on page");
        XSSFCell campaignCell = titleRow.createCell(3, CellType.STRING);
        campaignCell.setCellValue("Campaign");
        XSSFCell authorCell = titleRow.createCell(4, CellType.STRING);
        authorCell.setCellValue("Author");
        XSSFCell bodyCell = titleRow.createCell(5, CellType.STRING);
        bodyCell.setCellValue("values");

        setCellStyles(titleRow, getHeaderCellStyle(sheet.getWorkbook()));
    }

    private List<String> getBodyValues(CrowdsourcingAnnotation anno) {
        try {
            IResource bodyResource = annotationConverter.getBodyAsResource(anno);
            String type = "unknown";
            if (bodyResource instanceof TypedResource typedBodyResource) {
                type = typedBodyResource.getType();
            }
            switch (type) {
                case "TextualBody":
                    TextualResource res = (TextualResource) bodyResource;
                    return Collections.singletonList(res.getText());
                case "AuthorityResource":
                    return Collections.singletonList(bodyResource.getId().toString());
                case "Dataset":
                    Dataset dataset = (Dataset) bodyResource;
                    StringBuilder sb = new StringBuilder();
                    for (Entry<String, List<String>> entry : dataset.getData().entrySet()) {
                        String label = entry.getKey();
                        String value = entry.getValue().stream().collect(Collectors.joining(", "));
                        sb.append(label).append(": ").append(value).append("\t");
                    }
                    return Collections.singletonList(sb.toString().trim());
                default:
                    return Collections.singletonList(anno.getBody());

            }
        } catch (IOException e) {
            logger.error("Error writing body fields", e);
            return Collections.singletonList(anno.getBody());
        }
    }

    /**
     * 
     * @param row
     * @param style
     */
    private static void setCellStyles(Row row, CellStyle style) {
        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            cell.setCellStyle(style);
        }
    }

    private static CellStyle getHeaderCellStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * @param wb
     * @return {@link XSSFCellStyle}
     */
    private static XSSFCellStyle getDataCellStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        //        style.setAlignment(HorizontalAlignment.RIGHT);
        XSSFFont font = wb.createFont();
        style.setFont(font);
        return style;
    }
}
