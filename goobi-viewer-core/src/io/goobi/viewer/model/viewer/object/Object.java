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
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package io.goobi.viewer.model.viewer.object;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>Object class.</p>
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
     * <p>Constructor for Object.</p>
     *
     * @param uri a {@link java.net.URI} object.
     */
    public Object(URI uri) {
        this.uri = uri;
        this.type = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));
        
    }
    
    /**
     * <p>Constructor for Object.</p>
     *
     * @throws java.net.URISyntaxException
     * @param uri a {@link java.lang.String} object.
     */
    public Object(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
        this.type = ObjectFormat.getByFileExtension(uri.toString().substring(uri.toString().lastIndexOf("/")));

    }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return the type
     */
    public ObjectFormat getType() {
        return type;
    }
    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param type the type to set
     */
    public void setType(ObjectFormat type) {
        this.type = type;
    }
    /**
     * <p>Getter for the field <code>uri</code>.</p>
     *
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }
    /**
     * <p>Setter for the field <code>uri</code>.</p>
     *
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }
    /**
     * <p>Getter for the field <code>center</code>.</p>
     *
     * @return the center
     */
    public Point3D getCenter() {
        return center;
    }
    /**
     * <p>Setter for the field <code>center</code>.</p>
     *
     * @param center the center to set
     */
    public void setCenter(Point3D center) {
        this.center = center;
    }
    /**
     * <p>Getter for the field <code>rotation</code>.</p>
     *
     * @return the rotation
     */
    public Point3D getRotation() {
        return rotation;
    }
    /**
     * <p>Setter for the field <code>rotation</code>.</p>
     *
     * @param rotation the rotation to set
     */
    public void setRotation(Point3D rotation) {
        this.rotation = rotation;
    }
    
    
    

}
