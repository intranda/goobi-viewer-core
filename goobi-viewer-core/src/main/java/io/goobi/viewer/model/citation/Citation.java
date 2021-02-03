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
package io.goobi.viewer.model.citation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.output.Bibliography;
import io.goobi.viewer.controller.DataManager;

public class Citation {

    private static final Logger logger = LoggerFactory.getLogger(Citation.class);

    public static final String AUTHOR = "author";
    public static final String DOI = "DOI";
    public static final String ISBN = "ISBN";
    public static final String ISSN = "ISSN";
    public static final String ISSUED = "issued";
    public static final String LANGUAGE = "language";
    public static final String PUBLISHER_PLACE = "placepublish";
    public static final String PUBLISHER = "publisher";
    public static final String TITLE = "title";

    private String id;
    private final String style;
    private final CSLType type;
    private final Map<String, List<String>> fields;

    /**
     * Constructor.
     * 
     * @param id
     * @param style
     * @param type
     * @param fields Map containing metadata fields
     */
    public Citation(String id, String style, CSLType type, Map<String, List<String>> fields) {
        if (id == null) {
            throw new IllegalArgumentException("id may not be null");
        }
        if (style == null) {
            throw new IllegalArgumentException("style may not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }
        if (fields == null) {
            throw new IllegalArgumentException("fields may not be null");
        }

        this.id = id;
        this.style = style;
        this.type = type;
        this.fields = fields;
    }

    /**
     * 
     * @param style
     * @param outputFormat
     * @param items
     * @return
     * @throws IOException
     */
    Bibliography makeAdhocBibliography(String style, String outputFormat,
            CSLItemData... items) throws IOException {
        // logger.trace("makeAdhocBibliography");

        //        CSL csl = new CSL(provider, style);
        //        logger.trace("CLS created");
        //        csl.setOutputFormat(outputFormat);
        //
        //        String[] ids = new String[items.length];
        //        for (int i = 0; i < items.length; ++i) {
        //            ids[i] = items[i].getId();
        //        }
        //        csl.registerCitationItems(ids);
        //
        //        return csl.makeBibliography();

        CSL csl = DataManager.getInstance().getCitationProcessor(style);
        if (csl == null) {
            throw new IllegalStateException("CSL not created for: " + style);
        }
        csl.reset();
        csl.setOutputFormat(outputFormat);
        String[] ids = new String[items.length];
        for (int i = 0; i < items.length; ++i) {
            ids[i] = items[i].getId();
            logger.trace("Item data id: {}", items[i].getId());
        }
        csl.registerCitationItems(ids);

        return csl.makeBibliography();
    }

    /**
     * 
     * @return Full citation string
     * @throws IOException
     * @should return apa citation correctly
     */
    public String getCitationString() throws IOException {
        logger.trace("Citation string generation START");
        //        if (item == null || provider == null) {
        //            build();
        //        }
        //        String ret = CSL.makeAdhocBibliography(style, item).makeString();

        CSLItemData itemData = DataManager.getInstance().getCitationItemDataProvider().addItemData(id, fields, type);
        String ret = makeAdhocBibliography(style, "html", itemData).makeString();

        logger.trace("Citation string generation END");
        return ret;
    }
}
