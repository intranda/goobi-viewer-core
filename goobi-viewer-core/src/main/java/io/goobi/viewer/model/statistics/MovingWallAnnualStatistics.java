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
package io.goobi.viewer.model.statistics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Produces a list of record identifiers that were unlocked during the set year.
 */
public class MovingWallAnnualStatistics {

    private final int year;

    /**
     * 
     * @param year
     */
    public MovingWallAnnualStatistics(int year) {
        this.year = year;
    }

    /**
     * 
     * @return Solr query for the set year
     * @should build query correctly
     */
    String getQuery() {
        return "+" + SolrConstants.ISWORK + ":true +" + SolrConstants.DATE_PUBLICRELEASEDATE + ":[" + year + "-01-01T00:00:00.000Z TO " + year
                + "-12-31T23:59:59.999Z]";
    }

    /**
     * @param separator
     * @return CSV {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String exportAsCSV(String separator) throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(getQuery(), SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.DATE_PUBLICRELEASEDATE, "asc")),
                        Arrays.asList(SolrConstants.PI, SolrConstants.DATE_PUBLICRELEASEDATE));
        StringBuilder sb = new StringBuilder();
        sb.append("date").append(separator).append("identifier").append(separator);
        for (SolrDocument doc : docs) {
            String pi = SolrTools.getAsString(doc.getFieldValue(SolrConstants.PI));
            Date date = (Date) doc.getFieldValue(SolrConstants.DATE_PUBLICRELEASEDATE);
            if (pi != null && date != null) {
                sb.append('\n')
                        .append(DateTools.format(date, DateTools.FORMATTERISO8601DATETIME, true))
                        .append(separator)
                        .append(pi)
                        .append(separator);
            }
        }

        return sb.toString();
    }

}
