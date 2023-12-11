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
package io.goobi.viewer.servlets.oembed;

import io.goobi.viewer.exceptions.ViewerConfigurationException;

/**
 * <p>
 * PhotoOEmbedResponse class.
 * </p>
 */
public class PhotoOEmbedResponse extends OEmbedResponse {

    private String url;

    /**
     * Constructor.
     *
     * @param rec a {@link io.goobi.viewer.servlets.oembed.OEmbedRecord} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public PhotoOEmbedResponse(OEmbedRecord rec) throws ViewerConfigurationException {
        this.type = "photo";
        this.width = 300;
        this.height = 450;
        this.title = rec.getStructElement().getLabel();
        generateUrl(rec, width);
    }

    /**
     *
     * @param rec
     * @param record
     * @throws ViewerConfigurationException
     */
    private void generateUrl(OEmbedRecord rec, int size) {
        if (rec == null) {
            throw new IllegalArgumentException("record may not be null");
        }

        url = rec.getPhysicalElement().getImageUrl(size);
    }

    /**
     * <p>
     * Getter for the field <code>url</code>.
     * </p>
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Setter for the field <code>url</code>.
     * </p>
     *
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
