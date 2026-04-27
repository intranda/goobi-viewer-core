/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai.model;

import java.util.regex.Pattern;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import io.goobi.viewer.connector.oai.RequestHandler;

/**
 * <p>ResumptionToken class.</p>
 *
 */
@XStreamAlias("ResumptionToken")
public class ResumptionToken {

    /** Constant <code>TOKEN_NAME_PREFIX="oai_"</code> */
    public static final String TOKEN_NAME_PREFIX = "oai_";
    
    /** Constant <code>TOKEN_NAME_PATTERN</code> */
    public static final Pattern TOKEN_NAME_PATTERN = Pattern.compile("^" + TOKEN_NAME_PREFIX + "[0-9]{13}$");

    @XStreamAsAttribute
    @XStreamAlias("tokenName")
    private String tokenName;
    /** Total (virtual) hit number as communicated the client. */
    @XStreamAlias("hits")
    private long hits;
    /** Total actual record number in the index. */
    @XStreamAlias("rawHits")
    private long rawHits;
    @XStreamAlias("cursor")
    private int virtualCursor;
    @XStreamAlias("rawCursor")
    private int rawCursor;
    @XStreamAlias("expirationDate")
    private long expirationDate;
    @XStreamAlias("handler")
    private RequestHandler handler;

    /**
     * Creates a unique resumption token when number of hits is greater than list size.
     *
     * @param tokenName a {@link java.lang.String} object.
     * @param hits a long.
     * @param rawHits a long.
     * @param virtualCursor a int.
     * @param rawCursor a int.
     * @param expirationDate a long.
     * @param handler a {@link io.goobi.viewer.connector.oai.RequestHandler} object.
     */
    public ResumptionToken(String tokenName, long hits, long rawHits, int virtualCursor, int rawCursor, long expirationDate, RequestHandler handler) {
        this.tokenName = tokenName;
        this.hits = hits;
        this.rawHits = rawHits;
        this.virtualCursor = virtualCursor;
        this.rawCursor = rawCursor;
        this.expirationDate = expirationDate;
        this.handler = handler;
    }

    /**
     * <p>hasExpired.</p>
     *
     * @return a boolean.
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() > expirationDate;
    }

    /**
     * <p>Setter for the field <code>hits</code>.</p>
     *
     * @param hits the hits to set
     */
    public void setHits(long hits) {
        this.hits = hits;
    }

    /**
     * <p>Getter for the field <code>hits</code>.</p>
     *
     * @return the hits
     */
    public long getHits() {
        return hits;
    }

    /**
     * <p>Getter for the field <code>rawHits</code>.</p>
     *
     * @return the rawHits
     */
    public long getRawHits() {
        return rawHits;
    }

    /**
     * <p>Setter for the field <code>rawHits</code>.</p>
     *
     * @param rawHits the rawHits to set
     */
    public void setRawHits(long rawHits) {
        this.rawHits = rawHits;
    }

    /**
     * <p>Getter for the field <code>virtualCursor</code>.</p>
     *
     * @return the virtualCursor
     */
    public int getVirtualCursor() {
        return virtualCursor;
    }

    /**
     * <p>Setter for the field <code>virtualCursor</code>.</p>
     *
     * @param virtualCursor the virtualCursor to set
     */
    public void setVirtualCursor(int virtualCursor) {
        this.virtualCursor = virtualCursor;
    }

    /**
     * <p>Getter for the field <code>rawCursor</code>.</p>
     *
     * @return the rawCursor
     */
    public int getRawCursor() {
        return rawCursor;
    }

    /**
     * <p>Setter for the field <code>rawCursor</code>.</p>
     *
     * @param rawCursor the rawCursor to set
     */
    public void setRawCursor(int rawCursor) {
        this.rawCursor = rawCursor;
    }

    /**
     * <p>Setter for the field <code>expirationDate</code>.</p>
     *
     * @param expirationDate the expirationDate to set
     */
    public void setExpirationDate(long expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * <p>Getter for the field <code>expirationDate</code>.</p>
     *
     * @return the date
     */
    public long getExpirationDate() {
        return expirationDate;
    }

    /**
     * <p>Setter for the field <code>tokenName</code>.</p>
     *
     * @param tokenName the tokenName to set
     */
    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    /**
     * <p>Getter for the field <code>tokenName</code>.</p>
     *
     * @return the tokenName
     */
    public String getTokenName() {
        return tokenName;
    }

    /**
     * <p>Setter for the field <code>handler</code>.</p>
     *
     * @param handler the handler to set
     */
    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    /**
     * <p>Getter for the field <code>handler</code>.</p>
     *
     * @return the handler
     */
    public RequestHandler getHandler() {
        return handler;
    }

}
