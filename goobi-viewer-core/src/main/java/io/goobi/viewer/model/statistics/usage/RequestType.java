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

import java.util.Arrays;

import com.ibm.icu.impl.UResource.Array;

/**
 * @author florian
 *
 *         A list of types of requets to count independently for usage statistics. When recording a request in {@link UsageStatisticsRecorder} you
 *         need to pass an appropriate type. Statistics for each type are recorded independently
 */
public enum RequestType {

    /**
     * Call of a viewer html page belonging to a record
     */
    RECORD_VIEW(0, 1, "statistics__views"),
    /**
     * Download of a file (pdf, epub) belonging to a record
     */
    FILE_DOWNLOAD(2, 3, "statistics__downloads"),
    /**
     * REST-call to an image or other media resource of a record
     */
    MEDIA_RESOURCE(4, 5, "statistics__media_requests");

    private final int totalCountIndex;
    private final int uniqueCountIndex;
    private final String label;

    private RequestType(int totalCountIndex, int uniqueCountIndex, String label) {
        this.totalCountIndex = totalCountIndex;
        this.uniqueCountIndex = uniqueCountIndex;
        this.label = label;
    }

    /**
     * Get a message key serving as label to this type
     * 
     * @return a message key
     */
    public String getLabel() {
        return label;
    }

    /**
     * Index of the total request count within the array of values of the SOLR-field recording requests for a record
     * 
     * @return the totalCountIndex
     */
    public int getTotalCountIndex() {
        return totalCountIndex;
    }

    /**
     * Index of the count of requests by a unique http session within the array of values of the SOLR-field recording requests for a record
     * 
     * @return the uniqueCountIndex
     */
    public int getUniqueCountIndex() {
        return uniqueCountIndex;
    }

    /**
     * Index of the count for this type in {@link RequestType} within {@link SessionUsageStatistics}
     * 
     * @return the ordinal of the instance
     */
    public int getSessionCountIndex() {
        return this.ordinal();
    }

    /**
     * Get the RequestType for the given index of the count array in {@link RequestType} within {@link SessionUsageStatistics}
     * 
     * @param index
     * @return the type
     */
    public static RequestType getTypeForSessionCountIndex(int index) {
        RequestType[] types = RequestType.values();
        return Arrays.stream(types).filter(t -> t.getSessionCountIndex() == index).findAny().orElse(null);
    }

    /**
     * Get the RequestType for the given index of the count array for total count in the SOLR field for the counts of a record identifier
     * 
     * @param index
     * @return the type
     */
    public static RequestType getTypeForTotalCountIndex(int index) {
        RequestType[] types = RequestType.values();
        return Arrays.stream(types).filter(t -> t.getTotalCountIndex() == index).findAny().orElse(null);
    }

    /**
     * Get the RequestType for the given index of the count array for unique count in the SOLR field for the counts of a record identifier
     * 
     * @param index
     * @return the type
     */
    public static RequestType getTypeForUniqueCountIndex(int index) {
        RequestType[] types = RequestType.values();
        return Arrays.stream(types).filter(t -> t.getUniqueCountIndex() == index).findAny().orElse(null);
    }

    /**
     * Get the values for which request are recorded
     * 
     * @return An array
     */
    public static RequestType[] getUsedValues() {
        return new RequestType[] { RequestType.RECORD_VIEW, RequestType.FILE_DOWNLOAD };
    }
}
