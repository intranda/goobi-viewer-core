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
package io.goobi.viewer.connector.oai.model.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Wrapper class for metadata parameter value groups, so that JSF can iterate through them properly.
 */
public class MetadataValue implements Serializable {

    private static final long serialVersionUID = -3162322038017977356L;

    private final List<String> paramLabels = new ArrayList<>();

    /**
     * List of lists with parameter values. The top list represents the different parameters, with each containing one or more values for that
     * parameters.
     */
    private final List<List<String>> paramValues = new ArrayList<>();
    private final List<String> paramPrefixes = new ArrayList<>();
    private final List<String> paramSuffixes = new ArrayList<>();
    private final List<String> paramUrls = new ArrayList<>();
    private String masterValue;
    private String groupType;

    /**
     * Package-private constructor.
     * 
     * @param masterValue
     */
    MetadataValue(String masterValue) {
        this.masterValue = masterValue;
    }

    /**
     * <p>getComboValueShort.</p>
     *
     * @param index a int.
     * @should construct param correctly
     * @should construct multivalued param correctly
     * @should not add prefix if first param
     * @should return empty string if value index larger than number of values
     * @should return empty string if value is empty
     * @should not add null prefix
     * @should not add null suffix
     * @return a {@link java.lang.String} object.
     */
    public String getComboValueShort(int index) {
        StringBuilder sb = new StringBuilder();

        if (paramValues.size() > index && paramValues.get(index) != null && !paramValues.get(index).isEmpty()) {
            for (String paramValue : paramValues.get(index)) {
                if (StringUtils.isEmpty(paramValue)) {
                    continue;
                }
                boolean addPrefix = true;
                if (index == 0) {
                    addPrefix = false;
                }
                // Only add prefix if the total parameter value lengths is > 0 so far
                if (addPrefix && paramPrefixes.size() > index && paramPrefixes.get(index) != null) {
                    sb.append(paramPrefixes.get(index));
                }
                if (paramUrls.size() > index && StringUtils.isNotEmpty(paramUrls.get(index))) {
                    sb.append("<a href=\"").append(paramUrls.get(index)).append("\">").append(paramValue).append("</a>");
                } else {
                    sb.append(paramValue);
                }
                if (paramSuffixes.size() > index && paramSuffixes.get(index) != null) {
                    sb.append(paramSuffixes.get(index));
                }
            }
        }

        return sb.toString();
    }

    /**
     * <p>getParamLabelWithColon.</p>
     *
     * @return the paramLabels
     * @param index a int.
     */
    public String getParamLabelWithColon(int index) {
        if (paramLabels.size() > index && paramLabels.get(index) != null) {
            return ViewerResourceBundle.getTranslation(paramLabels.get(index), null) + ": ";
        }
        return "";
    }

    /**
     * <p>Getter for the field <code>paramLabels</code>.</p>
     *
     * @return the paramLabels
     */
    public List<String> getParamLabels() {
        return paramLabels;
    }

    /**
     * <p>Getter for the field <code>paramValues</code>.</p>
     *
     * @return the paramValues
     */
    public List<List<String>> getParamValues() {
        return paramValues;
    }

    /**
     * <p>Getter for the field <code>paramPrefixes</code>.</p>
     *
     * @return the paramPrefixes
     */
    public List<String> getParamPrefixes() {
        return paramPrefixes;
    }

    /**
     * <p>Getter for the field <code>paramSuffixes</code>.</p>
     *
     * @return the paramSuffixes
     */
    public List<String> getParamSuffixes() {
        return paramSuffixes;
    }

    /**
     * <p>Getter for the field <code>paramUrls</code>.</p>
     *
     * @return the paramUrls
     */
    public List<String> getParamUrls() {
        return paramUrls;
    }

    /**
     * <p>hasParamValue.</p>
     *
     * @param paramLabel a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return true;
        }
        return false;
    }

    /**
     * <p>getParamValue.</p>
     *
     * @param paramLabel a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return paramValues.get(index).get(0);
        }
        return "";
    }

    /**
     * <p>Getter for the field <code>masterValue</code>.</p>
     *
     * @return the masterValue
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            return "{0}";
        }

        return masterValue;
    }

    /**
     * <p>Setter for the field <code>masterValue</code>.</p>
     *
     * @param masterValue the masterValue to set
     */
    public void setMasterValue(String masterValue) {
        this.masterValue = masterValue;
    }

    /**
     * <p>getGroupTypeForUrl.</p>
     *
     * @return the groupType
     */
    public String getGroupTypeForUrl() {
        if (StringUtils.isEmpty(groupType)) {
            return "-";
        }
        return groupType;
    }

    /**
     * <p>Setter for the field <code>groupType</code>.</p>
     *
     * @param groupType the groupType to set
     */
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (List<String> params : this.paramValues) {
            for (String s : params) {
                sb.append("ParamValue_").append(count).append(": ").append(s).append(' ');
                count++;
            }
        }
        return sb.toString();
    }
}
