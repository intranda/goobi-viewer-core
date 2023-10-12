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
package io.goobi.viewer.model.cms;

import java.util.Collections;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;

public class SimpleMediaHolder implements CMSMediaHolder {

    private CMSMediaItem mediaItem = null;
    private CategorizableTranslatedSelectable<CMSMediaItem> mediaItemWrapper = null;
    private final String filter;

    public SimpleMediaHolder() {
        this.filter = "";
    }

    public SimpleMediaHolder(String filter) {
        this.filter = filter;
    }

    public SimpleMediaHolder(CMSMediaItem item) {
        this();
        setMediaItem(item);
    }

    public SimpleMediaHolder(CMSMediaItem item, String filter) {
        this(filter);
        setMediaItem(item);
    }

    @Override
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
        if (mediaItem != null) {
            this.mediaItemWrapper = new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        } else {
            this.mediaItemWrapper = null;
        }
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return this.mediaItem;
    }

    @Override
    public String getMediaFilter() {
        return filter;
    }

    @Override
    public String getMediaTypes() {
        return filter;
    }

    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        return this.mediaItemWrapper;
    }

}
