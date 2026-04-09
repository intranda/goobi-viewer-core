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

/**
 * Represents a point in 3D space with x, y, and z coordinates.
 *
 * @author Florian Alpers
 */
public class Point3D {

    private double x;
    private double y;
    private double z;

    /**
     * Creates a new Point3D instance.
     *
     * @param x a double.
     * @param y a double.
     * @param z a double.
     */
    public Point3D(double x, double y, double z) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Getter for the field <code>x</code>.
     *
     * @return the X coordinate value
     */
    public double getX() {
        return x;
    }

    /**
     * Setter for the field <code>x</code>.
     *
     * @param x the X coordinate value
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Getter for the field <code>y</code>.
     *
     * @return the Y coordinate value
     */
    public double getY() {
        return y;
    }

    /**
     * Setter for the field <code>y</code>.
     *
     * @param y the Y coordinate value
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Getter for the field <code>z</code>.
     *
     * @return the Z coordinate value
     */
    public double getZ() {
        return z;
    }

    /**
     * Setter for the field <code>z</code>.
     *
     * @param z the Z coordinate value
     */
    public void setZ(double z) {
        this.z = z;
    }

}
