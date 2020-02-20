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

/**
 * <p>
 * Point3D class.
 * </p>
 *
 * @author Florian Alpers
 */
public class Point3D {

    private double x;
    private double y;
    private double z;

    /**
     * <p>
     * Constructor for Point3D.
     * </p>
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
     * <p>
     * Getter for the field <code>x</code>.
     * </p>
     *
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * <p>
     * Setter for the field <code>x</code>.
     * </p>
     *
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * <p>
     * Getter for the field <code>y</code>.
     * </p>
     *
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * <p>
     * Setter for the field <code>y</code>.
     * </p>
     *
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * <p>
     * Getter for the field <code>z</code>.
     * </p>
     *
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * <p>
     * Setter for the field <code>z</code>.
     * </p>
     *
     * @param z the z to set
     */
    public void setZ(double z) {
        this.z = z;
    }

}
