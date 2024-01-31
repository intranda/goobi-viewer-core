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
package io.goobi.viewer.model.iiif.search.parser;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * SolrSearchParser class.
 * </p>
 *
 * @author florian
 */
public class SolrSearchParser extends AbstractSearchParser {

    private static final Logger logger = LogManager.getLogger(SolrSearchParser.class);

    private static final List<String> PAGEFIELDLIST = Arrays.asList(SolrConstants.ORDER, SolrConstants.WIDTH, SolrConstants.HEIGHT);

    private Map<Integer, Dimension> pageSizes = new HashMap<>();

    /**
     * <p>
     * getPageSize.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a {@link java.lang.Integer} object.
     * @return a {@link java.awt.Dimension} object.
     */
    public Dimension getPageSize(String pi, Integer pageNo) {
        if (!pageSizes.containsKey(pageNo)) {
            String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi;
            query += " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE;
            query += " +" + SolrConstants.ORDER + ":" + pageNo;
            try {
                SolrDocument pageDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, PAGEFIELDLIST);
                if (pageDoc != null) {
                    Integer width = Optional.ofNullable(SolrTools.getAsInt(pageDoc.getFieldValue(SolrConstants.WIDTH))).orElse(0);
                    Integer height = Optional.ofNullable(SolrTools.getAsInt(pageDoc.getFieldValue(SolrConstants.HEIGHT))).orElse(0);
                    pageSizes.put(pageNo, new Dimension(width, height));
                }
            } catch (PresentationException | IndexUnreachableException | NullPointerException e) {
                logger.error(e.toString(), e);
            }
        }

        if (pageSizes.get(pageNo) != null) {
            return pageSizes.get(pageNo);
        }
        return new Dimension(0, 0);
    }

}
