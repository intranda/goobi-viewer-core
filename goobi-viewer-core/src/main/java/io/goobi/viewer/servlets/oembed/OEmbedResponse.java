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
package io.goobi.viewer.servlets.oembed;

/**
 * <p>
 * Abstract OEmbedResponse class.
 * </p>
 */
public abstract class OEmbedResponse {

    // Required

    protected String version = "1.0";
    protected String type;
    protected Integer width;
    protected Integer height;

    // Optional

    protected String title;
    protected String authorName;
    protected String authorUrl;
    protected String providerName;
    protected String providerUrl;
    protected Integer cacheAge;
    protected String thumbnailUrl;
    protected Integer thumbnailWidth;
    protected Integer thumbnailHeight;

    /**
     * <p>
     * Getter for the field <code>version</code>.
     * </p>
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * <p>
     * Setter for the field <code>version</code>.
     * </p>
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * <p>
     * Getter for the field <code>width</code>.
     * </p>
     *
     * @return the width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * <p>
     * Setter for the field <code>width</code>.
     * </p>
     *
     * @param width the width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * <p>
     * Getter for the field <code>height</code>.
     * </p>
     *
     * @return the height
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * <p>
     * Setter for the field <code>height</code>.
     * </p>
     *
     * @param height the height to set
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>
     * Setter for the field <code>title</code>.
     * </p>
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * <p>
     * Getter for the field <code>authorName</code>.
     * </p>
     *
     * @return the authorName
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * <p>
     * Setter for the field <code>authorName</code>.
     * </p>
     *
     * @param authorName the authorName to set
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * <p>
     * Getter for the field <code>authorUrl</code>.
     * </p>
     *
     * @return the authorUrl
     */
    public String getAuthorUrl() {
        return authorUrl;
    }

    /**
     * <p>
     * Setter for the field <code>authorUrl</code>.
     * </p>
     *
     * @param authorUrl the authorUrl to set
     */
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    /**
     * <p>
     * Getter for the field <code>providerName</code>.
     * </p>
     *
     * @return the providerName
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * <p>
     * Setter for the field <code>providerName</code>.
     * </p>
     *
     * @param providerName the providerName to set
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * <p>
     * Getter for the field <code>providerUrl</code>.
     * </p>
     *
     * @return the providerUrl
     */
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * <p>
     * Setter for the field <code>providerUrl</code>.
     * </p>
     *
     * @param providerUrl the providerUrl to set
     */
    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    /**
     * <p>
     * Getter for the field <code>cacheAge</code>.
     * </p>
     *
     * @return the cacheAge
     */
    public Integer getCacheAge() {
        return cacheAge;
    }

    /**
     * <p>
     * Setter for the field <code>cacheAge</code>.
     * </p>
     *
     * @param cacheAge the cacheAge to set
     */
    public void setCacheAge(Integer cacheAge) {
        this.cacheAge = cacheAge;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * <p>
     * Setter for the field <code>thumbnailUrl</code>.
     * </p>
     *
     * @param thumbnailUrl the thumbnailUrl to set
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailWidth</code>.
     * </p>
     *
     * @return the thumbnailWidth
     */
    public Integer getThumbnailWidth() {
        return thumbnailWidth;
    }

    /**
     * <p>
     * Setter for the field <code>thumbnailWidth</code>.
     * </p>
     *
     * @param thumbnailWidth the thumbnailWidth to set
     */
    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * <p>
     * Getter for the field <code>thumbnailHeight</code>.
     * </p>
     *
     * @return the thumbnailHeight
     */
    public Integer getThumbnailHeight() {
        return thumbnailHeight;
    }

    /**
     * <p>
     * Setter for the field <code>thumbnailHeight</code>.
     * </p>
     *
     * @param thumbnailHeight the thumbnailHeight to set
     */
    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }
}
