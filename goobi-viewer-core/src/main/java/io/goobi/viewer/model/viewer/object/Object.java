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

/**
 * Represents a single 3D object with its source file, format, and associated textures.
 *
 * @author Florian Alpers
 */
public class Object {

    private ObjectFormat type;
    private URI uri;
    private Point3D center = new Point3D(0, 0, 0);
    private Point3D rotation = new Point3D(0, 0, 0);
    private double distance = 0;

    /**
     * Creates a new Object instance.
     *
     * @param uri URI pointing to the 3D object resource
     */
    public Object(URI uri) {
        this.uri = uri;
        this.type = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));

    }

    /**
     * Creates a new Object instance.
     *
     * @param uri URI string pointing to the 3D object resource
     * @throws java.net.URISyntaxException if any.
     */
    public Object(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
        this.type = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));

    }

    /**
     * Getter for the field <code>type</code>.
     *

     */
    public ObjectFormat getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the type to set
     */
    public void setType(ObjectFormat type) {
        this.type = type;
    }

    /**
     * Getter for the field <code>uri</code>.
     *

     */
    public URI getUri() {
        return uri;
    }

    /**
     * Setter for the field <code>uri</code>.
     *
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Getter for the field <code>center</code>.
     *

     */
    public Point3D getCenter() {
        return center;
    }

    /**
     * Setter for the field <code>center</code>.
     *
     * @param center the center to set
     */
    public void setCenter(Point3D center) {
        this.center = center;
    }

    /**
     * Getter for the field <code>rotation</code>.
     *

     */
    public Point3D getRotation() {
        return rotation;
    }

    /**
     * Setter for the field <code>rotation</code>.
     *
     * @param rotation the rotation to set
     */
    public void setRotation(Point3D rotation) {
        this.rotation = rotation;
    }

}
