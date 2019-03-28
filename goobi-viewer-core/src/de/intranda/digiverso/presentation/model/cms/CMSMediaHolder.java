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
package de.intranda.digiverso.presentation.model.cms;

import de.intranda.digiverso.presentation.managedbeans.CmsBean;

/**
 * Any object which may directly contain a {@link CMSMediaItem}. Only classes implementing this interface may be given a mediaItem 
 * in the selectMedia dialog, since the dialog uses {@link CmsBean#fillSelectedMediaHolder(CategorizableTranslatedSelectable)} to apply the selected MediaItem
 * 
 * @author florian
 *
 */
public interface CMSMediaHolder {

	public void setMediaItem(CMSMediaItem item);
	
	public CMSMediaItem getMediaItem();
	
	public String getMediaFilter();

	public boolean hasMediaItem();

	public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper();
}
