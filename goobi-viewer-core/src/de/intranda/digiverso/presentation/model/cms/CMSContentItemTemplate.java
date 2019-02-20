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

/**
 * A contentItem to be used in a CMSPage template. Stores a value for 
 * content item mode
 * 
 * @author Florian Alpers
 *
 */
public class CMSContentItemTemplate extends CMSContentItem {
	
	private String mediaFilter = "";

    private ContentItemMode mode = ContentItemMode.simple;
    
    /**
     * @param type
     */
    public CMSContentItemTemplate(CMSContentItemType type) {
        super(type);
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(ContentItemMode mode) {
        this.mode = mode;
    }
    
    /**
     * @return the mode
     */
    @Override
    public ContentItemMode getMode() {
        return mode;
    }

    @Override
    public String getMediaFilter() {
    	return mediaFilter;
    }

    /**
	 * @param mediaFilter the mediaFilter to set
	 */
	public void setMediaFilter(String mediaFilter) {
		this.mediaFilter = mediaFilter == null ? "" : mediaFilter;
	}
}

