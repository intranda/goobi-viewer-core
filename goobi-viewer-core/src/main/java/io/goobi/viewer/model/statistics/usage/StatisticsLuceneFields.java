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
package io.goobi.viewer.model.statistics.usage;

import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains field names of STATISTICS_USAGE SOLR documents
 * 
 * @author florian
 *
 */
public final class StatisticsLuceneFields {

    /**
     * Format dates to/from the string representation used in SOLR
     */
    public static final DateTimeFormatter SOLR_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * DOCTYPE field value for usage statistics documents
     */
    public static final String USAGE_STATISTICS_DOCTYPE = "STATISTICS_USAGE";
    /**
     * SOLR field name containing the name of the viewer instance
     */
    public static final String VIEWER_NAME = "STATISTICS_VIEWERNAME";
    /**
     * SOLR field name containing the date at which the statistics were recorded
     */
    public static final String DATE = "STATISTICS_DATE";
    /**
     * SOLR field prefix for fields containing request counts for individual record identifiers. The full field name consists of the prefix followed
     * by a record identifier
     */
    public static final String RECORD_STATISTICS_PREFIX = "STATISTICS_RECORD_";

    /**
     * Get the complete SOLR field name for a given record identifier
     * 
     * @param pi the record identifier
     * @return {@link String}
     */
    public static String getFieldName(String pi) {
        return RECORD_STATISTICS_PREFIX + pi;
    }

    /**
     * Get the record identifier from the given SOLR field name
     * 
     * @param fieldname
     * @return the record identifier
     */
    public static String getPi(String fieldname) {
        if (StringUtils.isNotBlank(fieldname) && fieldname.contains(RECORD_STATISTICS_PREFIX)) {
            return fieldname.replace(RECORD_STATISTICS_PREFIX, "");
        }
        return "";
    }

    private StatisticsLuceneFields() {
        //hide default constructor
    }

}
