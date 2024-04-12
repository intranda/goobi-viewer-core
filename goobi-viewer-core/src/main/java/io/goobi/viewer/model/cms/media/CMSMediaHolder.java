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
package io.goobi.viewer.model.cms.media;

import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;

/**
 * Any object which may directly contain a {@link io.goobi.viewer.model.cms.media.CMSMediaItem}. Only classes implementing this interface may be given
 * a mediaItem in the selectMedia dialog, since the dialog uses
 * {@link io.goobi.viewer.managedbeans.CmsBean#fillSelectedMediaHolder(CategorizableTranslatedSelectable)} to apply the selected MediaItem
 *
 * @author florian
 */
public interface CMSMediaHolder {

    /**
     * <p>
     * setMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     */
    public void setMediaItem(CMSMediaItem item);

    /**
     * <p>
     * getMediaItem.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     */
    public CMSMediaItem getMediaItem();

    /**
     * <p>
     * A regular expression determining which filenames are shown in the media list and may be uploaded
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMediaFilter();

    /**
     * A string representing the allowed media formates for a user. Should be a comma separated list of format names or filename suffixes
     * 
     * @return {@link String}
     */
    public String getMediaTypes();

    /**
     * <p>
     * hasMediaItem.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasMediaItem();

    /**
     * <p>
     * getMediaItemWrapper.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CategorizableTranslatedSelectable} object.
     */
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper();
}
