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
package de.intranda.digiverso.presentation.modules.interfaces;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.BrowseElement;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * @author Florian Alpers
 *
 */
public class DefaultURLBuilder implements IURLBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultURLBuilder.class);
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.modules.interfaces.IURLBuilder#generateURL(de.intranda.digiverso.presentation.model.search.BrowseElement)
     */
    @Override
    public String generateURL(BrowseElement ele) {
        
        // For aggregated person search hits, start another search (label contains the person's name in this case)
        String url = "";
        if (ele.getMetadataGroupType() != null) {
            switch (ele.getMetadataGroupType()) {
                case PERSON:
                case CORPORATION:
                case LOCATION:
                case SUBJECT:
                case ORIGININFO:
                case OTHER:
                    // Person metadata search hit ==> execute search for that person
                    // TODO not for aggregated hits?
                    url = buildSearchUrl(ele.getOriginalFieldName(), ele.getLabel());
                    break;
                default:
                    PageType pageType = getPageType(ele);
                    url = buildPageUrl(ele.getPi(), ele.getImageNo(), ele.getLogId(), pageType);
                    break;
            }
        } else {
            PageType pageType = getPageType(ele);
            url = buildPageUrl(ele.getPi(), ele.getImageNo(), ele.getLogId(), pageType);
        }

        // logger.trace("generateUrl: {}", sb.toString());
        return url;
        
    }

    /**
     * @param ele
     * @param sb
     * @param pageType
     */
    protected String buildPageUrl(String pi, int imageNo, String logId, PageType pageType) {
        StringBuilder sb = new StringBuilder();
        sb.append(pageType.getName()).append('/').append(pi).append('/').append(imageNo).append('/');
        // Hack for viewers that need a language parameter instead of LOGID
        String theme = DataManager.getInstance().getConfiguration().getTheme();
        if (theme != null) {
            switch (theme) {
                case "geiwv":
                case "wienerlibrary-novemberpogrom":
                    sb.append(DataManager.getInstance().getLanguageHelper().getLanguage(BeanUtils.getLocale().getLanguage()).getIsoCode())
                            .append("/");
                    break;
                default:
                    sb.append(StringUtils.isNotEmpty(logId) ? logId : '-').append('/');
            }
        }
        return sb.toString();
    }

    /**
     * @param ele
     * @return
     */
    protected PageType getPageType(BrowseElement ele) {
        PageType pageType = ele.determinePageType();
        // Hack for linking TEI full-text hits to the full-text page
        if (DocType.UGC.equals(ele.getDocType())) {
            pageType = PageType.viewObject;
        } else if ("TEI".equals(ele.getLabel())) {
            pageType = PageType.viewFulltext;
        }
        return pageType;
    }

    /**
     * @param ele
     * @param sb
     * @return
     */
    protected String buildSearchUrl(String fieldName, String fieldValue) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(PageType.search.getName())
                    .append("/-/")
                    .append(fieldName)
                    .append(":\"")
                    .append(URLEncoder.encode(fieldValue, SearchBean.URL_ENCODING))
                    .append("\"/1/-/-/");
        } catch (UnsupportedEncodingException e) {
            logger.error("{}: {}", e.getMessage(), fieldValue);
            sb = new StringBuilder();
            sb.append('/').append(PageType.search.getName()).append("/-/").append(fieldName).append(":\"").append(fieldValue).append(
                    "\"/1/-/-/");
        }
        return sb.toString();
    }

}
