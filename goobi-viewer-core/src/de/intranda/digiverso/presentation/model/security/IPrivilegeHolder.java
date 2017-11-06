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
package de.intranda.digiverso.presentation.model.security;

public interface IPrivilegeHolder {

    // Data access privileges
    public static final String _PRIV_PREFIX = "PRIV_";
    public static final String PRIV_LIST = "LIST";
    public static final String PRIV_VIEW_IMAGES = "VIEW_IMAGES";
    public static final String PRIV_VIEW_THUMBNAILS = "VIEW_THUMBNAILS";
    public static final String PRIV_VIEW_FULLTEXT = "VIEW_FULLTEXT";
    public static final String PRIV_VIEW_VIDEO = "VIEW_VIDEO";
    public static final String PRIV_VIEW_AUDIO = "VIEW_AUDIO";
    public static final String PRIV_DOWNLOAD_PDF = "DOWNLOAD_PDF";
    public static final String PRIV_DOWNLOAD_ORIGINAL_CONTENT = "DOWNLOAD_ORIGINAL_CONTENT";
    public static final String PRIV_SET_REPRESENTATIVE_IMAGE = "SET_REPRESENTATIVE_IMAGE";
    public static final String PRIV_EDIT_OVERVIEW_PAGE = "FORCE_OVERVIEW_PAGE";
    public static final String PRIV_DELETE_OCR_PAGE = "DELETE_OCR_PAGE";

    public boolean hasPrivilege(String privilege);

    public boolean isPrivList();

    public void setPrivList(boolean priv);

    public boolean isPrivViewImages();

    public void setPrivViewImages(boolean priv);

    public boolean isPrivViewThumbnails();

    public void setPrivViewThumbnails(boolean priv);

    public boolean isPrivViewFulltext();

    public void setPrivViewFulltext(boolean priv);

    public boolean isPrivViewVideo();

    public void setPrivViewVideo(boolean priv);

    public boolean isPrivViewAudio();

    public void setPrivViewAudio(boolean priv);

    public boolean isPrivDownloadPdf();

    public void setPrivDownloadPdf(boolean priv);

    public boolean isPrivDownloadOriginalContent();

    public void setPrivDownloadOriginalContent(boolean priv);

    public boolean isPrivSetRepresentativeImage();

    public void setPrivSetRepresentativeImage(boolean priv);
}
