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
package io.goobi.viewer.modules.interfaces;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * <p>
 * DefaultURLBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class DefaultURLBuilder implements IURLBuilder {

    private static final Logger logger = LogManager.getLogger(DefaultURLBuilder.class);

    /* (non-Javadoc)
     * @see io.goobi.viewer.modules.interfaces.IURLBuilder#generateURL(io.goobi.viewer.model.search.BrowseElement)
     */
    /**
     * {@inheritDoc}
     * 
     * @should only add page if not topStruct or page greater than one
     * @should only add logId if not topStruct
     */
    @Override
    public String generateURL(BrowseElement ele) {

        // For aggregated person search hits, start another search (label contains the person's name in this case)
        String url = "";
        boolean topstruct = ele.isWork() || ele.isAnchor() || ele.isGroup();
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
                    url = buildPageUrl(ele.getPi(), ele.getImageNo(), ele.getLogId(), pageType, topstruct);
                    break;
            }
        } else if (ele.isCmsPage() && ele.getPi().startsWith("CMS")) {
            url = "cms/" + ele.getPi().substring(3) + "/";
        } else {
            PageType pageType = getPageType(ele);
            if (PageType.viewFulltext.equals(pageType) && ele.isHasTeiFiles()) {
                // Add language to the URL if record has TEI full-text
                Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(BeanUtils.getLocale().getLanguage());
                url = new StringBuilder().append(pageType.getName())
                        .append('/')
                        .append(ele.getPi())
                        .append('/')
                        .append(ele.getImageNo())
                        .append('/')
                        .append(lang != null ? lang.getIsoCode() : "eng")
                        .append("/")
                        .toString();
            } else {
                url = buildPageUrl(ele.getPi(), ele.getImageNo(), ele.getLogId(), pageType, topstruct);
            }
        }

        // logger.trace("generateUrl: {}", url);
        return url;

    }

    /**
     * {@inheritDoc}
     * 
     * @should only add page if not topStruct or page greater than one
     * @should only add logId if not topStruct
     */
    @Override
    public String buildPageUrl(String pi, int imageNo, String logId, PageType pageType, boolean topStruct) {
        StringBuilder sb = new StringBuilder();
        String view = pageType.getName();

        // Check for CMS page as the default view for this record; do not override page-specific URLs
        if (topStruct && StringUtils.isNotEmpty(pi)) {
            try {
                CMSPage cmsPage = DataManager.getInstance().getDao().getCMSPageDefaultViewForRecord(pi);
                if (cmsPage != null) {
                    logger.trace("CMS default view page found: {}", cmsPage.getId());
                    return cmsPage.getRelativeUrlPath();
                }
            } catch (DAOException e) {
                logger.error(e.getMessage());
            }
        }

        sb.append(view)
                .append('/')
                .append(pi)
                .append('/');
        if (!topStruct || imageNo > 1) {
            sb.append(imageNo)
                    .append('/');
        }
        if (!topStruct) {
            sb.append(StringUtils.isNotEmpty(logId) ? logId : '-')
                    .append('/');
        }

        return sb.toString();
    }

    /**
     * <p>
     * getPageType.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.search.BrowseElement} object.
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
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
     * <p>
     * buildSearchUrl.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param fieldValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
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
            sb.append('/').append(PageType.search.getName()).append("/-/").append(fieldName).append(":\"").append(fieldValue).append("\"/1/-/-/");
        }
        return sb.toString();
    }

}
