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
package io.goobi.viewer.controller;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
public class PrettyUrlTools {

    public static String getRecordUrl(SolrDocument doc, PageType pageType) {
        String pi = doc.containsKey(SolrConstants.PI_TOPSTRUCT) ? doc.getFirstValue(SolrConstants.PI_TOPSTRUCT).toString() : "";
        if(StringUtils.isNotBlank(pi)) {
            String logId = doc.containsKey(SolrConstants.LOGID) ? doc.getFirstValue(SolrConstants.LOGID).toString() : "";
            String thumbPageNo = doc.containsKey(SolrConstants.THUMBPAGENO) ? doc.getFirstValue(SolrConstants.THUMBPAGENO).toString() : "";
            if(StringUtils.isNotBlank(logId)) {
                return getRecordURI(pi, thumbPageNo, logId, pageType);                
            } else {
                return getRecordURI(pi, pageType);
            }
        } else {
            return "";
        }
    }
    
    
    
    public static PageType getPreferredPageType(SolrDocument doc) {
        if(!doc.containsKey(SolrConstants.PI)) {
            throw new IllegalArgumentException("Can only get preferred pageType from main record document, i.e. one containing a PI field");
        }
        String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
        String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
        boolean anchorOrGroup = SolrTools.isAnchor(doc) || SolrTools.isGroup(doc);
        Boolean hasImages = (Boolean) doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE);

        PageType pageType = PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasImages, false);
        return pageType;
    }
    
    public static List<String> getSolrFieldsToDeterminePageType() {
        return Arrays.asList(
                SolrConstants.PI, 
                SolrConstants.ISANCHOR, 
                SolrConstants.ISWORK, 
                SolrConstants.LOGID, 
                SolrConstants.THUMBPAGENO, 
                SolrConstants.BOOL_IMAGEAVAILABLE,
                SolrConstants.DOCTYPE,
                SolrConstants.DOCSTRCT,
                SolrConstants.MIMETYPE);
    }
    
    private static String getRecordURI(String pi, PageType pageType) {
        String prettyId = "";
        switch(pageType) {
            case viewMetadata:
                prettyId = "metadata1";
                break;
            case viewToc:
                prettyId = "toc1";
                break;
            case viewObject:
            case viewImage:
            default:
                prettyId = "image1";
        }
        
        URL mappedUrl =
                PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(pi);
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString();
    }
    
    private static String getRecordURI(String pi, String imageNo, String logId, PageType pageType) {
        String prettyId = "";
        imageNo = StringUtils.isNotBlank(imageNo) ? imageNo : "-";
        logId = StringUtils.isNotBlank(logId) ? logId : "-";
        switch(pageType) {
            case viewMetadata:
                prettyId = "metadata3";
                break;
            case viewToc:
                prettyId = "toc3";
                break;
            case viewObject:
            case viewImage:
            default:
                prettyId = "image3";
        }
        
        URL mappedUrl =
                PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(pi, imageNo, logId);
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString();
    }
}
