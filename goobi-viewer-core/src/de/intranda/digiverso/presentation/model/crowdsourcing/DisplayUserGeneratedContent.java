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
package de.intranda.digiverso.presentation.model.crowdsourcing;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * UserGeneratedContent stub class for content display.
 */
public class DisplayUserGeneratedContent {

    public enum ContentType {

        PERSON,
        CORPORATION,
        ADDRESS,
        COMMENT,
        PICTURE;

        public String getName() {
            return this.name();
        }

        public static ContentType getByName(String name) {
            for (ContentType type : ContentType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }

            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(DisplayUserGeneratedContent.class);
    public static final NumberFormat format = new DecimalFormat("00000000");

    private Long id;

    private ContentType type;

    private String pi;

    private Integer page;

    private String label;

    private String displayCoordinates;

    private String areaString;

    private User updatedBy;

    private Date dateUpdated;

    /** Default constructor (needed for persistence). */
    public DisplayUserGeneratedContent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the type
     */
    public ContentType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(ContentType type) {
        this.type = type;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the <code>label</code>, if set, otherwise <code>pi</code>.
     *
     * @return
     */
    public String getDisplayLabel() {
        return StringUtils.isNotEmpty(label) ? label : pi;
    }

    /**
     * @return the updatedBy
     */
    public User getUpdatedBy() {
        return updatedBy;
    }

    /**
     * @param updatedBy the updatedBy to set
     */
    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    public String getDateUpdatedAsString() {
        if (dateUpdated != null) {
            String dateString = DateTools.formatterDEDate.print(dateUpdated.getTime());
            return dateString;
        }
        return null;
    }

    public String getTimeUpdatedAsString() {
        if (dateUpdated != null) {
            String dateString = DateTools.formatterISO8601Time.print(dateUpdated.getTime());
            return dateString;
        }
        return null;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the areaString
     */
    public String getAreaString() {
        return areaString;
    }

    /**
     * @param areaString the areaString to set
     */
    public void setAreaString(String areaString) {
        this.areaString = areaString;
    }

    public boolean hasArea() {
        return (!(getAreaString() == null) && !getAreaString().isEmpty());
    }

    public boolean mayHaveArea() {
        return true;
    }

    /**
     * @param coordinates
     * @return
     */
    public static int[] convertToIntArray(double[] coordinates) {
        int[] intCoords = new int[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            Double d = coordinates[i];
            intCoords[i] = (int) Math.round(d);
        }
        return intCoords;
    }

    public static double[] convertToDoubleArray(int[] coordinates) {
        double[] doubleCoords = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            Integer k = coordinates[i];
            doubleCoords[i] = k;
        }
        return doubleCoords;
    }

    public String getDisplayCoordinates() {
        return displayCoordinates;
    }

    public void setDisplayCoordinates(String displayCoordinates) {
        this.displayCoordinates = displayCoordinates;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.crowdsourcing.AbstractCrowdsourcingUpdate#getDisplayPage()
     */
    public Integer getDisplayPage() {
        return page;
    }

    public static class DateComparator implements Comparator<DisplayUserGeneratedContent> {

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(DisplayUserGeneratedContent o1, DisplayUserGeneratedContent o2) {
            return o1.dateUpdated.compareTo(o2.dateUpdated);
        }
    }

    public boolean isEmpty() {
        return StringUtils.isNotEmpty(getLabel()) || !getType().equals(ContentType.COMMENT);
    }

    public String getTypeAsString() {
        return getType().getName();
    }

    /**
     * 
     * @param doc
     * @return UserGeneratedContent generated from the given Solr document
     * @throws IndexUnreachableException
     * @should construct content correctly
     */
    public static DisplayUserGeneratedContent buildFromSolrDoc(SolrDocument doc) throws IndexUnreachableException {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        String type = (String) doc.getFieldValue(SolrConstants.UGCTYPE);
        if (type == null || ContentType.getByName(type) == null) {
            logger.error("Cannot build UGC Solr doc, UGCTYPE '{}' not found.", type);
            return null;
        }
        DisplayUserGeneratedContent ret = new DisplayUserGeneratedContent();
        long iddoc = Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC));
        ret.setId(iddoc);
        ret.setType(ContentType.getByName(type));
        ret.setAreaString((String) doc.getFieldValue(SolrConstants.UGCCOORDS));
        ret.setDisplayCoordinates((String) doc.getFieldValue(SolrConstants.UGCCOORDS));

        StructElement se = new StructElement(iddoc, doc);
        ret.setLabel(generateUgcLabel(se));

        return ret;
    }

    /**
     * Builds label out of user-generated content metadata.
     * 
     * @param se
     * @return the generated label
     * @should generate person label correctly
     * @should generate corporation label correctly
     * @should generate address label correctly
     * @should generate comment label correctly
     * @should return label field value if ugc type unknown
     */
    public static String generateUgcLabel(StructElement se) {
        if (se == null) {
            throw new IllegalArgumentException("se may not be null");
        }

        if (se.getMetadataValue(SolrConstants.UGCTYPE) != null) {
            switch (se.getMetadataValue(SolrConstants.UGCTYPE)) {
                case "PERSON": {
                    StringBuilder sb = new StringBuilder();
                    String first = se.getMetadataValue("MD_FIRSTNAME");
                    String last = se.getMetadataValue("MD_LASTNAME");
                    if (StringUtils.isNotEmpty(last)) {
                        sb.append(last);
                    }
                    if (StringUtils.isNotEmpty(first)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(first);
                    }
                    return sb.toString();
                }
                case "CORPORATION": {
                    StringBuilder sb = new StringBuilder();
                    String address = se.getMetadataValue("MD_ADDRESS");
                    String corp = se.getMetadataValue("MD_CORPORATION");
                    if (StringUtils.isNotEmpty(corp)) {
                        sb.append(corp);
                    }
                    if (StringUtils.isNotEmpty(address)) {
                        sb.append(" (").append(corp).append(')');
                    }
                    return sb.toString();
                }
                case "ADDRESS": {
                    StringBuilder sb = new StringBuilder();
                    String street = se.getMetadataValue("MD_STREET");
                    String houseNumber = se.getMetadataValue("MD_HOUSENUMBER");
                    String district = se.getMetadataValue("MD_DISTRICT");
                    String city = se.getMetadataValue("MD_CITY");
                    String country = se.getMetadataValue("MD_COUNTRY");
                    if (StringUtils.isNotEmpty(street)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(street);
                        if (StringUtils.isNotEmpty(houseNumber)) {
                            sb.append(", ").append(houseNumber);
                        }
                    }
                    if (StringUtils.isNotEmpty(district)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(district);
                    }
                    if (StringUtils.isNotEmpty(city)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(city);
                    }
                    if (StringUtils.isNotEmpty(country)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(country);
                    }
                    return sb.toString();
                }
                case "COMMENT":
                    return se.getMetadataValue("MD_TEXT");
                default:
                    return se.getMetadataValue(SolrConstants.LABEL);
            }
        }

        return se.getMetadataValue(SolrConstants.LABEL);
    }

}
