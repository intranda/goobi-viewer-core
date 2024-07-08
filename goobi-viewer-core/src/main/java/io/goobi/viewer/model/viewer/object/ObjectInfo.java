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
package io.goobi.viewer.model.viewer.object;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * ObjectInfo class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ObjectInfo {

    private ObjectFormat format;
    private URI uri;
    private List<URI> resources;
    private Point3D center = new Point3D(0, 0, 0);
    private Point3D rotation = new Point3D(0, 0, 0);
    private Map<URI, Long> resourceSizes = new HashMap<>();

    /**
     * <p>
     * Constructor for ObjectInfo.
     * </p>
     *
     * @param uri a {@link java.net.URI} object.
     */
    public ObjectInfo(URI uri) {
        this.uri = uri;
        this.format = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));

    }

    /**
     * <p>
     * Constructor for ObjectInfo.
     * </p>
     *
     * @param uri a {@link java.lang.String} object.
     * @throws java.net.URISyntaxException if any.
     */
    public ObjectInfo(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
        this.format = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));

    }

    /**
     * <p>
     * Getter for the field <code>format</code>.
     * </p>
     *
     * @return the type
     */
    public ObjectFormat getFormat() {
        return format;
    }

    /**
     * <p>
     * Setter for the field <code>format</code>.
     * </p>
     *
     * @param format a {@link io.goobi.viewer.model.viewer.object.ObjectFormat} object.
     */
    public void setFormat(ObjectFormat format) {
        this.format = format;
    }

    /**
     * <p>
     * Getter for the field <code>uri</code>.
     * </p>
     *
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * <p>
     * Setter for the field <code>uri</code>.
     * </p>
     *
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * <p>
     * Getter for the field <code>center</code>.
     * </p>
     *
     * @return the center
     */
    public Point3D getCenter() {
        return center;
    }

    /**
     * <p>
     * Setter for the field <code>center</code>.
     * </p>
     *
     * @param center the center to set
     */
    public void setCenter(Point3D center) {
        this.center = center;
    }

    /**
     * <p>
     * Getter for the field <code>rotation</code>.
     * </p>
     *
     * @return the rotation
     */
    public Point3D getRotation() {
        return rotation;
    }

    /**
     * <p>
     * Setter for the field <code>rotation</code>.
     * </p>
     *
     * @param rotation the rotation to set
     */
    public void setRotation(Point3D rotation) {
        this.rotation = rotation;
    }

    /**
     * <p>
     * Getter for the field <code>resources</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<URI> getResources() {
        return resources;
    }

    /**
     * <p>
     * Setter for the field <code>resources</code>.
     * </p>
     *
     * @param resources a {@link java.util.List} object.
     */
    public void setResources(List<URI> resources) {
        this.resources = resources;
    }

    public Map<URI, Long> getResourceSizes() {
        return resourceSizes;
    }

    public void setSize(URI resourceURI, Long size) {
        this.resourceSizes.put(resourceURI, size);
    }

}
