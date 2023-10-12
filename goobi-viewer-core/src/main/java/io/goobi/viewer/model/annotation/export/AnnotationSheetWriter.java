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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.TypedResource;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;

/**
 * @author florian
 *
 */
public class AnnotationSheetWriter {

    public static final String UNKNOWN_RESOURCE_TYPE = "Unknown";

    private final ExcelRenderer excelRenderer;
    private final AnnotationConverter annotationConverter = new AnnotationConverter();

    public AnnotationSheetWriter() {
        this.excelRenderer = new ExcelRenderer(annotationConverter);
    }

    /**
     * @param os
     * @param annotations
     * @throws IOException
     */
    public void createExcelSheet(OutputStream os, List<CrowdsourcingAnnotation> annotations) throws IOException {

        Map<String, List<CrowdsourcingAnnotation>> annoMap = annotations.stream().collect(Collectors.groupingBy(this::getBodyType));

        XSSFWorkbook wb = excelRenderer.render(annoMap);
        wb.write(os);
        os.flush();
    }

    private String getBodyType(CrowdsourcingAnnotation anno) {
        try {
            IResource body = annotationConverter.getBodyAsResource(anno);
            if (body instanceof TypedResource) {
                return ((TypedResource) body).getType();
            }
        } catch (IOException e) {
        }
        return UNKNOWN_RESOURCE_TYPE;
    }

}
