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
package io.goobi.viewer.model.security;

import java.util.List;
import java.util.Set;

/**
 * IPrivilegeHolder interface.
 */
public interface IPrivilegeHolder {

    // Data access privileges
    /** Constant <code>PREFIX_TICKET="TICKET_"</code> */
    public static final String PREFIX_TICKET = "TICKET_";
    /** Constant <code>PREFIX_PRIV="PRIV_"</code> */
    public static final String PREFIX_PRIV = "PRIV_";

    /** Constant <code>PRIV_ARCHIVE_DISPLAY_NODE="PRIV_ARCHIVE_DISPLAY_NODE"</code> */
    public static final String PRIV_ARCHIVE_DISPLAY_NODE = "ARCHIVE_DISPLAY_NODE";
    /** Constant <code>PRIV_LIST="LIST"</code> */
    public static final String PRIV_LIST = "LIST";
    /** Constant <code>PRIV_VIEW_IMAGES="VIEW_IMAGES"</code> */
    public static final String PRIV_VIEW_IMAGES = "VIEW_IMAGES";
    /** Constant <code>PRIV_VIEW_THUMBNAILS="VIEW_THUMBNAILS"</code> */
    public static final String PRIV_VIEW_THUMBNAILS = "VIEW_THUMBNAILS";
    /** Constant <code>PRIV_VIEW_FULLTEXT="VIEW_FULLTEXT"</code> */
    public static final String PRIV_VIEW_FULLTEXT = "VIEW_FULLTEXT";
    /** Constant <code>PRIV_VIEW_VIDEO="VIEW_VIDEO"</code> */
    public static final String PRIV_VIEW_VIDEO = "VIEW_VIDEO";
    /** Constant <code>PRIV_VIEW_AUDIO="VIEW_AUDIO"</code> */
    public static final String PRIV_VIEW_AUDIO = "VIEW_AUDIO";
    /** Constant <code>PRIV_VIEW_UGC="PRIV_VIEW_UGC"</code> */
    public static final String PRIV_VIEW_UGC = "VIEW_UGC";
    /** Constant <code>PRIV_VIEW_CMS="PRIV_VIEW_CMS"</code> */
    public static final String PRIV_VIEW_CMS = "VIEW_CMS";
    /** Constant <code>PRIV_VIEW_UGC="PRIV_VIEW_METADATA"</code> */
    public static final String PRIV_VIEW_METADATA = "VIEW_METADATA";
    /** Constant <code>PRIV_DOWNLOAD_PDF="DOWNLOAD_PDF"</code> */
    public static final String PRIV_DOWNLOAD_PDF = "DOWNLOAD_PDF";
    /** Constant <code>PRIV_DOWNLOAD_PAGE_PDF="DOWNLOAD_PAGE_PDF"</code> */
    public static final String PRIV_DOWNLOAD_PAGE_PDF = "DOWNLOAD_PAGE_PDF";
    /** Constant <code>PRIV_DOWNLOAD_ORIGINAL_CONTENT="DOWNLOAD_ORIGINAL_CONTENT"</code> */
    public static final String PRIV_DOWNLOAD_ORIGINAL_CONTENT = "DOWNLOAD_ORIGINAL_CONTENT";
    /** Constant <code>PRIV_DOWNLOAD_METADATA="DOWNLOAD_METADATA"</code> */
    public static final String PRIV_DOWNLOAD_METADATA = "DOWNLOAD_METADATA";
    /** Constant <code>PRIV_DOWNLOAD_IMAGES="DOWNLOAD_IMAGES"</code> */
    public static final String PRIV_DOWNLOAD_IMAGES = "DOWNLOAD_IMAGES";
    /**
     * Constant <code>PRIV_GENERATE_IIIF_MANIFEST="GENERATE_IIIF_MANIFEST"</code>
     * 
     * @deprecated iiif manifests are now always allowed, but individual resources therein may not be, depending on specific privileges
     */
    @Deprecated(since = "25.11")
    public static final String PRIV_GENERATE_IIIF_MANIFEST = "GENERATE_IIIF_MANIFEST";
    /** Constant <code>PRIV_ZOOM_IMAGES="ZOOM_IMAGES"</code> */
    public static final String PRIV_ZOOM_IMAGES = "ZOOM_IMAGES";
    /** Constant <code>PRIV_DOWNLOAD_BORN_DIGITAL_FILES="DOWNLOAD_BORN_DIGITAL_FILES"</code> */
    public static final String PRIV_DOWNLOAD_BORN_DIGITAL_FILES = "DOWNLOAD_BORN_DIGITAL_FILES";

    // Role privileges
    /** Constant <code>PRIV_DELETE_OCR_PAGE="DELETE_OCR_PAGE"</code> */
    public static final String PRIV_DELETE_OCR_PAGE = "DELETE_OCR_PAGE";
    /** Constant <code>PRIV_SET_REPRESENTATIVE_IMAGE="SET_REPRESENTATIVE_IMAGE"</code> */
    public static final String PRIV_SET_REPRESENTATIVE_IMAGE = "SET_REPRESENTATIVE_IMAGE";

    // CMS privileges
    /** Constant <code>PRIV_CMS_PAGES="CMS_PAGES"</code> */
    public static final String PRIV_CMS_PAGES = "CMS_PAGES";
    /** Constant <code>PRIV_CMS_PAGES="PRIV_LEGAL_DISCLAIMER"</code> */
    public static final String PRIV_LEGAL_DISCLAIMER = "PRIV_LEGAL_DISCLAIMER";
    /** Constant <code>PRIV_CMS_ALL_SUBTHEMES="CMS_ALL_SUBTHEMES"</code> */
    public static final String PRIV_CMS_ALL_SUBTHEMES = "CMS_ALL_SUBTHEMES";
    /** Constant <code>PRIV_CMS_ALL_CATEGORIES="CMS_ALL_CATEGORIES"</code> */
    public static final String PRIV_CMS_ALL_CATEGORIES = "CMS_ALL_CATEGORIES";
    /** Constant <code>PRIV_CMS_ALL_TEMPLATES="CMS_ALL_TEMPLATES"</code> */
    public static final String PRIV_CMS_ALL_TEMPLATES = "CMS_ALL_TEMPLATES";
    /** Constant <code>PRIV_CMS_MENU="CMS_MENU"</code> */
    public static final String PRIV_CMS_MENU = "CMS_MENU";
    /** Constant <code>PRIV_CMS_STATIC_PAGES="CMS_STATIC_PAGES"</code> */
    public static final String PRIV_CMS_STATIC_PAGES = "CMS_STATIC_PAGES";
    /** Constant <code>PRIV_CMS_COLLECTIONS="CMS_COLLECTIONS"</code> */
    public static final String PRIV_CMS_COLLECTIONS = "CMS_COLLECTIONS";
    /** Constant <code>PRIV_CMS_CATEGORIES="CMS_CATEGORIES"</code> */
    public static final String PRIV_CMS_CATEGORIES = "CMS_CATEGORIES";

    // Crowdsourcing privileges
    /** Constant <code>PRIV_CROWDSOURCING_ALL_CAMPAIGNS="CROWDSOURCING_ALL_CAMPAIGNS"</code> */
    public static final String PRIV_CROWDSOURCING_ALL_CAMPAIGNS = "CROWDSOURCING_ALL_CAMPAIGNS";
    /** Constant <code>PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN="CROWDSOURCING_ANNOTATE_CAMPAIGN"</code> */
    public static final String PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN = "CROWDSOURCING_ANNOTATE_CAMPAIGN";
    /** Constant <code>PRIV_CROWDSOURCING_REVIEW_CAMPAIGN="CROWDSOURCING_REVIEW_CAMPAIGN"</code> */
    public static final String PRIV_CROWDSOURCING_REVIEW_CAMPAIGN = "CROWDSOURCING_REVIEW_CAMPAIGN";

    public List<String> getSortedPrivileges(Set<String> privileges);

    public boolean addPrivilege(String privilege);

    public boolean removePrivilege(String privilege);

    /**
     * hasPrivilege.
     *
     * @param privilege a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasPrivilege(String privilege);

    /**
     * isPrivCmsPages.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsPages();

    /**
     * setPrivCmsPages.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsPages(boolean priv);

    /**
     * isPrivCmsMenu.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsMenu();

    /**
     * setPrivCmsMenu.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsMenu(boolean priv);

    /**
     * isPrivCmsAllSubthemes.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllSubthemes();

    /**
     * setPrivCmsAllSubthemes.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllSubthemes(boolean priv);

    /**
     * isPrivCmsAllCategories.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllCategories();

    /**
     * setPrivCmsAllCategories.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllCategories(boolean priv);

    /**
     * isPrivCmsAllTemplates.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsAllTemplates();

    /**
     * setPrivCmsAllTemplates.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsAllTemplates(boolean priv);

    /**
     * isPrivCmsStaticPages.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsStaticPages();

    /**
     * setPrivCmsStaticPages.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsStaticPages(boolean priv);

    /**
     * isPrivCmsCollections.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCollections();

    /**
     * setPrivCmsCollections.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCollections(boolean priv);

    /**
     * isPrivCmsCategories.
     *
     * @return a boolean.
     */
    public boolean isPrivCmsCategories();

    /**
     * setPrivCmsCategories.
     *
     * @param priv a boolean.
     */
    public void setPrivCmsCategories(boolean priv);

    /**
     * isPrivCrowdsourcingAllCampaigns.
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingAllCampaigns();

    /**
     * setPrivCrowdsourcingAllCampaigns.
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingAllCampaigns(boolean priv);

    /**
     * isPrivCrowdsourcingAnnotateCampaign.
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingAnnotateCampaign();

    /**
     * setPrivCrowdsourcingAnnotateCampaign.
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingAnnotateCampaign(boolean priv);

    /**
     * isPrivCrowdsourcingReviewCampaign.
     *
     * @return a boolean.
     */
    public boolean isPrivCrowdsourcingReviewCampaign();

    /**
     * setPrivCrowdsourcingReviewCampaign.
     *
     * @param priv a boolean.
     */
    public void setPrivCrowdsourcingReviewCampaign(boolean priv);

    /**
     * isPrivViewUgc.
     *
     * @return a boolean.
     */
    public boolean isPrivViewUgc();

    /**
     * setPrivViewUgc.
     *
     * @param priv a boolean.
     */
    public void setPrivViewUgc(boolean priv);
}
