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
package io.goobi.viewer.model.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Wrapper class for metadata parameter value groups, so that JSF can iterate through them properly.
 */
public class MetadataValue implements Serializable {

    private static final long serialVersionUID = -3162322038017977356L;

    private static final Logger logger = LoggerFactory.getLogger(MetadataValue.class);

    private final List<String> paramLabels = new ArrayList<>();

    /**
     * List of lists with parameter values. The top list represents the different parameters, with each containing one or more values for that
     * parameters.
     */
    private final List<List<String>> paramValues = new ArrayList<>();
    private final List<String> paramMasterValueFragments = new ArrayList<>();
    private final List<String> paramPrefixes = new ArrayList<>();
    private final List<String> paramSuffixes = new ArrayList<>();
    private final List<String> paramUrls = new ArrayList<>();
    private final Map<String, String> normDataUrls = new HashMap<>();
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
     * @should not add empty prefix
     * @should not add empty suffix
     * @should add separator between values if no prefix used
     * @should use master value fragment correctly
     * @return a {@link java.lang.String} object.
     */
    public String getComboValueShort(int index) {
        if (paramValues.size() <= index || paramValues.get(index) == null || paramValues.get(index).isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String paramValue : paramValues.get(index)) {
            if (StringUtils.isEmpty(paramValue)) {
                continue;
            }

            boolean addPrefix = true;
            boolean addSuffix = true;
            if (index == 0) {
                addPrefix = false;
            }

            String masterFragment = "{0}";
            // Configured master value fragment overrides prefix/suffix
            if (paramMasterValueFragments.size() > index && StringUtils.isNotEmpty(paramMasterValueFragments.get(index))) {
                addPrefix = false;
                addSuffix = false;
                masterFragment = ViewerResourceBundle.getTranslation(paramMasterValueFragments.get(index), null);
                logger.trace("master fragment: {}", masterFragment);
            }
            // Only add prefix if the total parameter value lengths is > 0 so far
            if (addPrefix && paramPrefixes.size() > index && StringUtils.isNotEmpty(paramPrefixes.get(index))) {
                sb.append(paramPrefixes.get(index));
            } else if (sb.length() > 0) {
                // Use separator between values if no prefix is used
                sb.append(", ");
            }
            if (paramUrls.size() > index && StringUtils.isNotEmpty(paramUrls.get(index))) {
                StringBuilder sbUrl = new StringBuilder();
                sbUrl.append("<a href=\"").append(paramUrls.get(index)).append("\">").append(paramValue).append("</a>");
                masterFragment = masterFragment.replace("{0}", sbUrl.toString());
            } else {
                masterFragment = masterFragment.replace("{0}", paramValue);
            }
            sb.append(masterFragment);
            if (addSuffix && paramSuffixes.size() > index && StringUtils.isNotEmpty(paramSuffixes.get(index))) {
                sb.append(paramSuffixes.get(index));
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
            return Helper.getTranslation(paramLabels.get(index), null) + ": ";
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
     * <p>Getter for the field <code>paramMasterValueFragments</code>.</p>
     *
     * @return the paramMasterValueFragments
     */
    public List<String> getParamMasterValueFragments() {
        return paramMasterValueFragments;
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
     * <p>Getter for the field <code>normDataUrls</code>.</p>
     *
     * @return the normDataUrls
     */
    public Map<String, String> getNormDataUrls() {
        return normDataUrls;
    }

    /**
     * <p>getNormDataUrlKeys.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getNormDataUrlKeys() {
        if (!normDataUrls.isEmpty()) {
            return new ArrayList<>(normDataUrls.keySet());
        }

        return null;
    }

    /**
     * <p>getNormDataUrl.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @return Not URL-encoded norm data URL
     */
    public String getNormDataUrl(String key) {
        return getNormDataUrl(key, false);
    }

    /**
     * <p>getNormDataUrl.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param urlEncode a boolean.
     * @return if urlEncode=true, then URL-encoded norm data URL; otherwise not encoded norm data URL
     */
    public String getNormDataUrl(String key, boolean urlEncode) {
        if (urlEncode) {
            return BeanUtils.escapeCriticalUrlChracters(normDataUrls.get(key));
        }

        return normDataUrls.get(key);
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
