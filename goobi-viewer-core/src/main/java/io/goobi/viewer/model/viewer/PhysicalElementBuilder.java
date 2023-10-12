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

/**
 *
 */
public class PhysicalElementBuilder {

    /** Persistent identifier. */
    private String pi;

    private String filePath;
    /** Physical page number of this element in the list of all pages (this value is always 1 below the ORDER value in the METS file). */
    private int order;
    /** Logical page number (label) of this element. */
    private String orderLabel;
    /** Physical ID from the METS file. */
    private String physId;
    /** URN granular. */
    private String urn;

    private String purlPart;
    /** Media mime type. */
    private String mimeType = BaseMimeType.IMAGE.getName();
    /** Data repository name for the record to which this page belongs. */
    private String dataRepository;

    public PhysicalElement build() {
        return new PhysicalElement(physId, filePath, order, orderLabel, urn, purlPart, pi, mimeType, dataRepository);
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     * @return this
     */
    public PhysicalElementBuilder setPi(String pi) {
        this.pi = pi;
        return this;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath the filePath to set
     * @return this
     */
    public PhysicalElementBuilder setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     * @return this
     */
    public PhysicalElementBuilder setOrder(int order) {
        this.order = order;
        return this;
    }

    /**
     * @return the orderLabel
     */
    public String getOrderLabel() {
        return orderLabel;
    }

    /**
     * @param orderLabel the orderLabel to set
     * @return this
     */
    public PhysicalElementBuilder setOrderLabel(String orderLabel) {
        this.orderLabel = orderLabel;
        return this;
    }

    /**
     * @return the physId
     */
    public String getPhysId() {
        return physId;
    }

    /**
     * @param physId the physId to set
     * @return this
     */
    public PhysicalElementBuilder setPhysId(String physId) {
        this.physId = physId;
        return this;
    }

    /**
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * @param urn the urn to set
     * @return this
     */
    public PhysicalElementBuilder setUrn(String urn) {
        this.urn = urn;
        return this;
    }

    /**
     * @return the purlPart
     */
    public String getPurlPart() {
        return purlPart;
    }

    /**
     * @param purlPart the purlPart to set
     * @return this
     */
    public PhysicalElementBuilder setPurlPart(String purlPart) {
        this.purlPart = purlPart;
        return this;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     * @return this
     */
    public PhysicalElementBuilder setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * @param dataRepository the dataRepository to set
     * @return this
     */
    public PhysicalElementBuilder setDataRepository(String dataRepository) {
        this.dataRepository = dataRepository;
        return this;
    }

}
