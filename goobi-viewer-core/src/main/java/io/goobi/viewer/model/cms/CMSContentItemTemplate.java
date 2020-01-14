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
package io.goobi.viewer.model.cms;

import org.apache.commons.lang3.StringUtils;

/**
 * A contentItem to be used in a CMSPage template. Stores a value for
 * content item mode
 *
 * @author Florian Alpers
 */
public class CMSContentItemTemplate extends CMSContentItem {
	
	private String mediaFilter = "";
    private ContentItemMode mode = ContentItemMode.simple;
    private String inlineHelp = null;
    private boolean preview = false;
    
    /**
     * <p>Constructor for CMSContentItemTemplate.</p>
     *
     * @param type a CMSContentItemType object.
     */
    public CMSContentItemTemplate(CMSContentItemType type) {
        super(type);
    }

    /**
     * <p>Setter for the field <code>mode</code>.</p>
     *
     * @param mode the mode to set
     */
    public void setMode(ContentItemMode mode) {
        this.mode = mode;
    }
    
    /** {@inheritDoc} */
    @Override
    public ContentItemMode getMode() {
        return mode;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isHasInlineHelp() {
        return StringUtils.isNotBlank(inlineHelp);
    }
    
    /**
     * {@inheritDoc}
     *
     * Message key to display when clicking the inline help button
     */
    @Override
    public String getInlineHelp() {
        return this.inlineHelp;
    }
    
    /**
     * <p>Setter for the field <code>inlineHelp</code>.</p>
     *
     * @param inlineHelp the inlineHelp to set
     */
    public void setInlineHelp(String inlineHelp) {
        this.inlineHelp = inlineHelp;
    }

    /** {@inheritDoc} */
    @Override
    public String getMediaFilter() {
    	return mediaFilter;
    }

	/**
	 * <p>Setter for the field <code>mediaFilter</code>.</p>
	 *
	 * @param mediaFilter the mediaFilter to set
	 */
	public void setMediaFilter(String mediaFilter) {
		this.mediaFilter = mediaFilter == null ? "" : mediaFilter;
	}
	
	/* (non-Javadoc)
	 * @see io.goobi.viewer.model.cms.CMSContentItem#isPreview()
	 */
	/** {@inheritDoc} */
	@Override
	public boolean isPreview() {
	    return this.preview;
	}
	
    /**
     * <p>Setter for the field <code>preview</code>.</p>
     *
     * @param preview the preview to set
     */
    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}

