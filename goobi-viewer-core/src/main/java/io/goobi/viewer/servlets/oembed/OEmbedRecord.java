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

import java.net.URI;
import java.net.URISyntaxException;

import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * OEmbedRecord class.
 */
public class OEmbedRecord {

    private StructElement structElement;
    private PhysicalElement physicalElement;
    private URI uri = null;

    /**
     * <p>Constructor for OEmbedRecord.
     *
     * @param uri URL string to parse into a URI
     * @throws java.net.URISyntaxException
     */
    public OEmbedRecord(String uri) throws URISyntaxException {
        this.uri = PathConverter.toURI(uri);
    }

    /**
     * <p>Constructor for OEmbedRecord.
     */
    public OEmbedRecord() {

    }

    /**
     * Getter for the field <code>structElement</code>.
     *
     * @return the structElement
     */
    public StructElement getStructElement() {
        return structElement;
    }

    /**
     * Setter for the field <code>structElement</code>.
     *
     * @param structElement the structElement to set
     */
    public void setStructElement(StructElement structElement) {
        this.structElement = structElement;
    }

    /**
     * Getter for the field <code>physicalElement</code>.
     *
     * @return the physicalElement
     */
    public PhysicalElement getPhysicalElement() {
        return physicalElement;
    }

    /**
     * Setter for the field <code>physicalElement</code>.
     *
     * @param physicalElement the physicalElement to set
     */
    public void setPhysicalElement(PhysicalElement physicalElement) {
        this.physicalElement = physicalElement;
    }

    /**
     * <p>isRichResponse.
     *
     * @return a boolean
     */
    public boolean isRichResponse() {
        return this.uri != null;
    }

    /**
     * <p>Getter for the field <code>uri</code>.
     *
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * <p>Setter for the field <code>uri</code>.
     *
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }
}
