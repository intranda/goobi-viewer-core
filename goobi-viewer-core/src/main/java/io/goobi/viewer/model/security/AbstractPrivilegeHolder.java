package io.goobi.viewer.model.security;

public abstract class AbstractPrivilegeHolder implements IPrivilegeHolder {

    /** Constant array containing all constants for record privileges. */
    protected static final String[] PRIVS_RECORD =
            { PRIV_LIST, PRIV_VIEW_THUMBNAILS, PRIV_VIEW_IMAGES, PRIV_VIEW_VIDEO, PRIV_VIEW_AUDIO, PRIV_VIEW_FULLTEXT, PRIV_ZOOM_IMAGES,
                    PRIV_DOWNLOAD_IMAGES, PRIV_DOWNLOAD_ORIGINAL_CONTENT, PRIV_DOWNLOAD_PAGE_PDF, PRIV_DOWNLOAD_PDF, PRIV_DOWNLOAD_METADATA,
                    PRIV_GENERATE_IIIF_MANIFEST, PRIV_VIEW_UGC, PRIV_DOWNLOAD_BORN_DIGITAL_FILES };

    /** Constant array containing all constants for CMS privileges. */
    protected static final String[] PRIVS_CMS =
            { PRIV_CMS_PAGES, PRIV_CMS_MENU, PRIV_CMS_STATIC_PAGES,
                    PRIV_CMS_COLLECTIONS,
                    PRIV_CMS_CATEGORIES };
}
