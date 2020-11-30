/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.annotation.export;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Dataset;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.TypedResource;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.questions.Question;

public class ExcelRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ExcelRenderer.class);

    private final AnnotationsResourceBuilder annotationBuilder;
    
    /**
     * @param annotationBuilder
     */
    public ExcelRenderer(AnnotationsResourceBuilder annotationBuilder) {
       this.annotationBuilder = annotationBuilder;
    }

    public HSSFWorkbook render(Map<String, List<PersistentAnnotation>> annotationMap) {
        if (annotationMap == null) {
            throw new IllegalArgumentException("No annotations given");
        }

        if (annotationMap.size() == 0) {
            throw new IllegalArgumentException("Empty annotations map");
        }

        HSSFWorkbook wb = new HSSFWorkbook();

        for (String type : annotationMap.keySet()) {
            HSSFSheet sheet = wb.createSheet(type);
            sheet.setDefaultColumnWidth(30);
            short height = 300;
            sheet.setDefaultRowHeight(height);
            createHeaderRow(sheet);
            List<PersistentAnnotation> annotations = annotationMap.get(type);
            createDataRows(sheet, annotations);
        }

        return wb;
    }

    /**
     * @param sheet
     * @param annotations
     */
    public void createDataRows(HSSFSheet sheet, List<PersistentAnnotation> annotations) {
        int rowCounter = 1;
        for (PersistentAnnotation annotation : annotations) {
            try {
                createDataRow(annotation, sheet, rowCounter);
            } catch (DAOException e) {
                logger.error("Error creating data row for annotation " + annotation);
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
    public void createDataRow(PersistentAnnotation annotation, HSSFSheet sheet, int rowCounter) throws DAOException {
        HSSFRow row = sheet.createRow(rowCounter);
        row.setRowStyle(getDataCellStyle(sheet.getWorkbook()));
        HSSFCell idCell = row.createCell(0, CellType.STRING);
        idCell.setCellValue(annotation.getId().toString());
        HSSFCell recordCell = row.createCell(1, CellType.STRING);
        recordCell.setCellValue(annotation.getTargetPI());
        HSSFCell pageCell = row.createCell(2, CellType.STRING);
        String pageOrder = Optional.ofNullable(annotation.getTargetPageOrder()).map(i -> i.toString()).orElse("");
        pageCell.setCellValue(pageOrder);
        HSSFCell campaignCell = row.createCell(3, CellType.STRING);
        campaignCell.setCellValue(Optional.ofNullable(annotation.getGenerator()).map(Question::getOwner).map(Campaign::getTitle).orElse(""));
        HSSFCell authorCell = row.createCell(4, CellType.STRING);
        authorCell.setCellValue(annotation.getCreator().getDisplayName());
        HSSFCell bodyCell = row.createCell(5, CellType.STRING);
        bodyCell.setCellValue(getBodyValues(annotation).get(0));
        
        setCellStyles(row, getDataCellStyle(sheet.getWorkbook()));

    }



    /**
     * @param sheet
     */
    public void createHeaderRow(HSSFSheet sheet) {
        HSSFRow titleRow = sheet.createRow(0);
        titleRow.setRowStyle(getHeaderCellStyle(sheet.getWorkbook()));
        HSSFCell idCell = titleRow.createCell(0, CellType.STRING);
        idCell.setCellStyle(getHeaderCellStyle(sheet.getWorkbook()));
        idCell.setCellValue("ID");
        HSSFCell recordCell = titleRow.createCell(1, CellType.STRING);
        recordCell.setCellValue("in");
        HSSFCell pageCell = titleRow.createCell(2, CellType.STRING);
        pageCell.setCellValue("on page");
        HSSFCell campaignCell = titleRow.createCell(3, CellType.STRING);
        campaignCell.setCellValue("Campaign");
        HSSFCell authorCell = titleRow.createCell(4, CellType.STRING);
        authorCell.setCellValue("Author");
        HSSFCell bodyCell = titleRow.createCell(5, CellType.STRING);
        bodyCell.setCellValue("values");
        
        setCellStyles(titleRow, getHeaderCellStyle(sheet.getWorkbook()));
    }
    
    private List<String> getBodyValues(PersistentAnnotation anno) {
        try {            
            IResource bodyResource = annotationBuilder.getBodyAsResource(anno);
            String type = "unknown";
            if(bodyResource instanceof TypedResource) {
                type = ((TypedResource) bodyResource).getType();
            }
            switch(type) {
                case "TextualBody":
                    TextualResource res = (TextualResource)bodyResource;
                    return Collections.singletonList(res.getText());
                case "AuthorityResource":
                    return Collections.singletonList(bodyResource.getId().toString());
                case "Dataset":
                    Dataset dataset = (Dataset)bodyResource;
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
        } catch(IOException e) {
            logger.error("Error writing body fields", e);
            return Collections.singletonList(anno.getBody());
        }
    }
    
    /**
     * 
     */
    private void setCellStyles(Row row, CellStyle style) {
        Iterator<Cell> cells = row.cellIterator();
        while(cells.hasNext()) {
            Cell cell = cells.next();
            cell.setCellStyle(style);
        }
    }

    private CellStyle getHeaderCellStyle(HSSFWorkbook wb) {
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        HSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    /**
     * @param workbook
     * @return
     */
    private HSSFCellStyle getDataCellStyle(HSSFWorkbook wb) {
        HSSFCellStyle style = wb.createCellStyle();
//        style.setAlignment(HorizontalAlignment.RIGHT);
        HSSFFont font = wb.createFont();
        style.setFont(font);
        return style;
    }
}
