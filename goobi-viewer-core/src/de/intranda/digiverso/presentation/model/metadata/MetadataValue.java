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
package de.intranda.digiverso.presentation.model.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

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
     *
     * @param index
     * @return
     * @should construct param correctly
     * @should construct multivalued param correctly
     * @should not add prefix if first param
     * @should return empty string if value index larger than number of values
     * @should return empty string if value is empty
     * @should not add null prefix
     * @should not add null suffix
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
                    //                logger.trace("URL: {}: {}", index,paramUrls.get(index));
                } else {
                    //                logger.trace("Non-URL: {}: {}", index, paramValues.get(index));
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
     * @return the paramLabels
     */
    public String getParamLabelWithColon(int index) {
        if (paramLabels.size() > index && paramLabels.get(index) != null) {
            return Helper.getTranslation(paramLabels.get(index), null) + ": ";
        }
        return "";
    }

    /**
     * @return the paramLabels
     */
    public List<String> getParamLabels() {
        return paramLabels;
    }

    /**
     * @return the paramValues
     */
    public List<List<String>> getParamValues() {
        return paramValues;
    }

    /**
     * @return the paramPrefixes
     */
    public List<String> getParamPrefixes() {
        return paramPrefixes;
    }

    /**
     * @return the paramSuffixes
     */
    public List<String> getParamSuffixes() {
        return paramSuffixes;
    }

    /**
     * @return the paramUrls
     */
    public List<String> getParamUrls() {
        return paramUrls;
    }

    /**
     * @return the normDataUrls
     */
    public Map<String, String> getNormDataUrls() {
        return normDataUrls;
    }

    public List<String> getNormDataUrlKeys() {
        if (!normDataUrls.isEmpty()) {
            return new ArrayList<>(normDataUrls.keySet());
        }

        return null;
    }

    /**
     * 
     * @param key
     * @return Not URL-encoded norm data URL
     */
    public String getNormDataUrl(String key) {
        return getNormDataUrl(key, false);
    }

    /**
     * 
     * @param key
     * @param urlEncode
     * @return if urlEncode=true, then URL-encoded norm data URL; otherwise not encoded norm data URL
     */
    public String getNormDataUrl(String key, boolean urlEncode) {
        if (urlEncode) {
            return BeanUtils.escapeCriticalUrlChracters(normDataUrls.get(key));
        }

        return normDataUrls.get(key);
    }

    public boolean hasParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return true;
        }
        return false;
    }

    public String getParamValue(String paramLabel) {
        int index = paramLabels.indexOf(paramLabel);
        if (index > -1 && index < paramValues.size()) {
            return paramValues.get(index).get(0);
        }
        return "";
    }

    /**
     * @return the masterValue
     */
    public String getMasterValue() {
        if (StringUtils.isEmpty(masterValue)) {
            return "{0}";
        }

        return masterValue;
    }

    /**
     * @param masterValue the masterValue to set
     */
    public void setMasterValue(String masterValue) {
        this.masterValue = masterValue;
    }

    /**
     * @return the groupType
     */
    public String getGroupTypeForUrl() {
        if (StringUtils.isEmpty(groupType)) {
            return "-";
        }
        return groupType;
    }

    /**
     * @param groupType the groupType to set
     */
    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

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
