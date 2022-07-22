/**
 * This file is part of the Goobi Solr Indexer - a content indexing tool for the Goobi viewer and OAI-PMH/SRU interfaces.
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
package io.goobi.viewer.model.statistics.usage;

import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

/**
 * @author florian
 *
 */
public class StatisticsLuceneFields {

    public static final DateTimeFormatter solrDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    
    public static final String USAGE_STATISTICS_DOCTYPE = "STATISTICS_USAGE";
    public static final String VIEWER_NAME = "STATISTICS_VIEWERNAME";
    public static final String DATE = "STATISTICS_DATE";
    public static final String RECORD_STATISTICS_PREFIX = "STATISTICS_RECORD_";
    
    public static String getFieldName(String pi) {
        return RECORD_STATISTICS_PREFIX + pi;
    }

    public static String getPi(String fieldname) {
        if(StringUtils.isNotBlank(fieldname) && fieldname.contains(RECORD_STATISTICS_PREFIX)) {
            return fieldname.replace(RECORD_STATISTICS_PREFIX, "");
        } else {
            return "";
        }
    }
    
    private StatisticsLuceneFields() {
        //hide default constructor
    }
    
}
