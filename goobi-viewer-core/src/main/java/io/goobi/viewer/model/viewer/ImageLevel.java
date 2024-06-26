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
package io.goobi.viewer.model.viewer;

import java.awt.Dimension;

/**
 * <p>
 * ImageLevel class.
 * </p>
 */
public class ImageLevel implements Comparable<ImageLevel> {

    private String url;
    private Dimension size;
    private int rotation;

    /**
     * <p>
     * Constructor for ImageLevel.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param size a {@link java.awt.Dimension} object.
     */
    public ImageLevel(String url, Dimension size) {
        super();
        this.url = url;
        this.size = size;
        this.rotation = 0;
    }

    /**
     * <p>
     * Constructor for ImageLevel.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param width a int.
     * @param height a int.
     */
    public ImageLevel(String url, int width, int height) {
        super();
        this.url = url;
        this.size = new Dimension(width, height);
        this.rotation = 0;
    }

    /**
     * <p>
     * Constructor for ImageLevel.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param size a {@link java.awt.Dimension} object.
     * @param currentRotate a int.
     */
    public ImageLevel(String url, Dimension size, int currentRotate) {
        super();
        this.url = url;
        this.size = size;
        this.rotation = currentRotate;
    }

    /**
     * <p>
     * Getter for the field <code>url</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Getter for the field <code>size</code>.
     * </p>
     *
     * @return a {@link java.awt.Dimension} object.
     */
    public Dimension getSize() {
        if (rotation % 180 == 0) {
            return size;
        }
        return new Dimension(size.height, size.width);
    }

    /**
     * <p>
     * getWidth.
     * </p>
     *
     * @return a int.
     */
    public int getWidth() {
        return rotation % 180 == 90 ? size.height : size.width;
    }

    /**
     * <p>
     * getHeight.
     * </p>
     *
     * @return a int.
     */
    public int getHeight() {
        return rotation % 180 == 90 ? size.width : size.height;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[\"" + url + "\"," + getWidth() + "," + getHeight() + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(ImageLevel other) {
        return Integer.compare(size.width, other.size.width);
    }

}
