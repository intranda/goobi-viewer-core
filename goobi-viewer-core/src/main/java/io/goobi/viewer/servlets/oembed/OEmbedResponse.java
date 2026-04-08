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

/**
 * Base class for oEmbed response objects, containing the common fields defined by the oEmbed specification.
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
     * Getter for the field <code>version</code>.
     *

     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter for the field <code>version</code>.
     *
     * @param version the oEmbed spec version string (e.g. "1.0")
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Getter for the field <code>type</code>.
     *

     */
    public String getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the oEmbed response type (e.g. "photo", "rich", "video", "link")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Getter for the field <code>width</code>.
     *

     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Setter for the field <code>width</code>.
     *
     * @param width the width in pixels of the embedded content
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Getter for the field <code>height</code>.
     *

     */
    public Integer getHeight() {
        return height;
    }

    /**
     * Setter for the field <code>height</code>.
     *
     * @param height the height in pixels of the embedded content
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Getter for the field <code>title</code>.
     *

     */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the field <code>title</code>.
     *
     * @param title the title of the embedded resource
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for the field <code>authorName</code>.
     *

     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Setter for the field <code>authorName</code>.
     *
     * @param authorName the name of the author or creator of the embedded resource
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * Getter for the field <code>authorUrl</code>.
     *

     */
    public String getAuthorUrl() {
        return authorUrl;
    }

    /**
     * Setter for the field <code>authorUrl</code>.
     *
     * @param authorUrl the URL of the author's profile or homepage
     */
    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    /**
     * Getter for the field <code>providerName</code>.
     *

     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Setter for the field <code>providerName</code>.
     *
     * @param providerName the name of the oEmbed content provider
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Getter for the field <code>providerUrl</code>.
     *

     */
    public String getProviderUrl() {
        return providerUrl;
    }

    /**
     * Setter for the field <code>providerUrl</code>.
     *
     * @param providerUrl the homepage URL of the oEmbed content provider
     */
    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    /**
     * Getter for the field <code>cacheAge</code>.
     *

     */
    public Integer getCacheAge() {
        return cacheAge;
    }

    /**
     * Setter for the field <code>cacheAge</code>.
     *
     * @param cacheAge the suggested cache lifetime of this response in seconds
     */
    public void setCacheAge(Integer cacheAge) {
        this.cacheAge = cacheAge;
    }

    /**
     * Getter for the field <code>thumbnailUrl</code>.
     *

     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * Setter for the field <code>thumbnailUrl</code>.
     *
     * @param thumbnailUrl the URL of the thumbnail image for the embedded resource
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * Getter for the field <code>thumbnailWidth</code>.
     *

     */
    public Integer getThumbnailWidth() {
        return thumbnailWidth;
    }

    /**
     * Setter for the field <code>thumbnailWidth</code>.
     *
     * @param thumbnailWidth the width in pixels of the thumbnail image
     */
    public void setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    /**
     * Getter for the field <code>thumbnailHeight</code>.
     *

     */
    public Integer getThumbnailHeight() {
        return thumbnailHeight;
    }

    /**
     * Setter for the field <code>thumbnailHeight</code>.
     *
     * @param thumbnailHeight the height in pixels of the thumbnail image
     */
    public void setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }
}
