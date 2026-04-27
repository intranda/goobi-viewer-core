/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.RequestHandler;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * Solr-related utility methods (such as query building, result parsing, etc.).
 * </p>
 *
 */
public final class SolrSearchTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrSearchTools.class);

    /** Constant <code>MAX_HITS=Integer.MAX_VALUE</code> */
    public static final int MAX_HITS = Integer.MAX_VALUE;

    private SolrSearchTools() {
    }

    /**
     * build the query String depending on the variables
     * 
     * @param from
     * @param until
     * @param setSpec
     * @param metadataPrefix
     * @param excludeAnchor
     * @param additionalQuery
     * @return Generated query
     * @should add from until to setSpec queries
     */
    static String buildQueryString(String from, String until, String setSpec, String metadataPrefix, boolean excludeAnchor, String additionalQuery) {
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("+(+(").append(SolrConstants.ISWORK).append(":true");
        if (!excludeAnchor) {
            sbQuery.append(' ').append(SolrConstants.ISANCHOR).append(":true");
        }
        sbQuery.append(' ').append(SolrConstants.DATEDELETED).append(":*)");
        if (StringUtils.isNotEmpty(additionalQuery)) {
            sbQuery.append(additionalQuery);
        }
        sbQuery.append(')');

        // setSpec
        if (setSpec != null) {
            boolean defaultSet = true;
            // Use DC as the set field by default 
            String setQuery = SolrConstants.DC + ":" + setSpec;

            // Check whether this is an additional set and if so, use its custom query

            List<Set> additionalSetList = DataManager.getInstance().getConfiguration().getAdditionalSets();
            for (Set s : additionalSetList) {
                if (s.getSetSpec().equals(setSpec)) {
                    defaultSet = false;
                    sbQuery = new StringBuilder(); // Replace query
                    setQuery = s.getSetQuery();
                    break;
                }
            }

            // Check whether this is an all-values set and if so, use its field
            if (defaultSet && setSpec.contains(":")) {
                List<Set> allValuesSetList = DataManager.getInstance().getConfiguration().getAllValuesSets();
                for (Set s : allValuesSetList) {
                    if (s.getSetName().equals(setSpec.substring(0, setSpec.indexOf(":")))) {
                        setQuery = setSpec;
                        break;
                    }
                }
            }
            if (sbQuery.length() > 0) {
                sbQuery.append(" +");
            }
            sbQuery.append(setQuery);
        }

        // Solr timestamp range is irrelevant for iv_* formats
        if (!Metadata.IV_OVERVIEWPAGE.getMetadataPrefix().equals(metadataPrefix)
                && !Metadata.IV_CROWDSOURCING.getMetadataPrefix().equals(metadataPrefix)
                && (from != null || until != null)) {
            long fromTimestamp = RequestHandler.getFromTimestamp(from);
            long untilTimestamp = RequestHandler.getUntilTimestamp(until);
            if (fromTimestamp == untilTimestamp) {
                untilTimestamp += 999;
            }
            sbQuery.append(" +")
                    .append(SolrConstants.DATEUPDATED)
                    .append(":[")
                    .append(normalizeDate(String.valueOf(fromTimestamp)))
                    .append(" TO ")
                    .append(normalizeDate(String.valueOf(untilTimestamp)))
                    .append(']');

        }

        return sbQuery.toString();
    }

    /**
     * Returns the blacklist filter suffix (if enabled), followed by the user-agnostic access condition suffix. For the purposes of OAI, the privilege
     * to download metadata is checked rather than the privilege to list a record.
     *
     * @param request
     * @return a {@link java.lang.String} object.
     * @throws IndexUnreachableException
     */
    public static String getAllSuffixes(HttpServletRequest request) {
        return SearchHelper.getAllSuffixes(request, true, true, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA);
    }

    /**
     * 
     * @param date
     * @return date normalized to 13 digits
     */
    static String normalizeDate(String date) {
        if (date != null) {
            if (date.charAt(0) == '-' || date.length() > 12) {
                return date;
            }
            switch (date.length()) {
                case 8:
                    return "00000" + date;
                case 9:
                    return "0000" + date;
                case 10:
                    return "000" + date;
                case 11:
                    return "00" + date;
                case 12:
                    return "0" + date;
                default: // nothing
            }
        }

        return date;
    }

    /**
     * Returns the latest DATEUPDATED value on the given <code>SolrDocument</code> that is no larger than <code>untilTimestamp</code>. Returns 0 if no
     * such value is found.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param untilTimestamp a long.
     * @return Latest DATEUPDATED value that is less than or equals untilTimestamp on doc; 0 if none found.
     * @should return correct value
     * @should return 0 if no valid value is found
     * @should ignore untilTimestamp if zero
     */
    public static Long getLatestValidDateUpdated(SolrDocument doc, long untilTimestamp) {
        logger.trace("getLatestValidDateUpdated: {}", untilTimestamp);
        long ret = 0;
        Collection<Object> dateUpdatedValues = doc.getFieldValues(SolrConstants.DATEUPDATED);
        if (dateUpdatedValues != null && !dateUpdatedValues.isEmpty()) {
            // Get latest DATEUPDATED values
            for (Object o : dateUpdatedValues) {
                long dateUpdated = (Long) o;
                if (dateUpdated > ret && (untilTimestamp == 0 || dateUpdated <= untilTimestamp)) {
                    ret = dateUpdated;
                }
            }
        }

        return ret;
    }

    /**
     * Returns a list with all (string) values for the given field name in the given SolrDocument.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @should return empty list if doc null
     * @should return empty list if no values for fieldName found
     * @should return all values for the given field
     */
    public static List<String> getMetadataValues(SolrDocument doc, String fieldName) {
        if (doc == null) {
            return Collections.emptyList();
        }

        Collection<Object> values = doc.getFieldValues(fieldName);
        if (values == null) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof String s) {
                ret.add(s);
            } else {
                ret.add(String.valueOf(value));
            }
        }

        return ret;
    }

    /**
     * <p>
     * getFieldCount.
     * </p>
     *
     * @param queryResponse a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @param field a {@link java.lang.String} object.
     * @return a long.
     * @should throw IllegalArgumentException if queryResponse null
     * @should throw IllegalArgumentException if field null
     */
    public static long getFieldCount(QueryResponse queryResponse, String field) {
        if (queryResponse == null) {
            throw new IllegalArgumentException("queryResponse may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }

        long ret = 0;
        FieldStatsInfo info = queryResponse.getFieldStatsInfo().get(field);
        if (info != null) {
            Object count = info.getCount();
            if (count instanceof Long || count instanceof Integer) {
                ret = (long) count;
            } else if (count instanceof Double d) {
                ret = d.longValue();
            }
            logger.trace("Total hits via {} value count: {}", field, ret);
        }

        return ret;
    }

    /**
     * <p>
     * getAdditionalDocstructsQuerySuffix.
     * </p>
     *
     * @param additionalDocstructTypes a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     * @should build query suffix correctly
     */
    public static String getAdditionalDocstructsQuerySuffix(List<String> additionalDocstructTypes) {
        if (additionalDocstructTypes == null || additionalDocstructTypes.isEmpty()) {
            return "";
        }

        StringBuilder sbQuerySuffix = new StringBuilder();
        sbQuerySuffix.append(" OR (").append(SolrConstants.DOCTYPE).append(":DOCSTRCT AND (");
        int count = 0;
        for (String docstructType : additionalDocstructTypes) {
            if (StringUtils.isNotBlank(docstructType)) {
                if (count > 0) {
                    sbQuerySuffix.append(" OR ");
                }
                sbQuerySuffix.append(SolrConstants.DOCSTRCT).append(':').append(docstructType);
                count++;
            } else {
                logger.warn("Empty element found in <additionalDocstructTypes>.");
            }
        }
        sbQuerySuffix.append("))");
        // Avoid returning an invalid subquery if all configured values are blank
        if (count == 0) {
            return "";
        }

        return sbQuerySuffix.toString();
    }

    /**
     * <p>
     * getUrnPrefixBlacklistSuffix.
     * </p>
     *
     * @param urnPrefixBlacklist a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     * @should build query suffix correctly
     */
    public static String getUrnPrefixBlacklistSuffix(List<String> urnPrefixBlacklist) {
        StringBuilder sbQuerySuffix = new StringBuilder();
        if (urnPrefixBlacklist != null && !urnPrefixBlacklist.isEmpty()) {
            int count = 0;
            for (final String p : urnPrefixBlacklist) {
                if (StringUtils.isNotBlank(p)) {
                    String urnPrefix = ClientUtils.escapeQueryChars(p);
                    urnPrefix += '*';
                    sbQuerySuffix.append(" -")
                            .append("URN_UNTOKENIZED:")
                            .append(urnPrefix)
                            .append(" -")
                            .append("IMAGEURN_UNTOKENIZED:")
                            .append(urnPrefix)
                            .append(" -")
                            .append("IMAGEURN_OAI_UNTOKENIZED:")
                            .append(urnPrefix);
                    count++;
                } else {
                    logger.warn("Empty element found in <additionalDocstructTypes>.");
                }
            }
            // Avoid returning an invalid subquery if all configured values are blank
            if (count == 0) {
                return "";
            }
        }

        return sbQuerySuffix.toString();
    }
}
