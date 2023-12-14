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
package io.goobi.viewer.model.administration.legal;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

/**
 * Describes the scope within which a declaration of consent (i.e. clicking the 'accept' button) is valid. Outside of that scope, consent must be
 * requested again. The scope consists of a duration in days during which it is valid, as well as a storage type which can either be 'local' or
 * 'session'. This type determines if a consent is stored in the local or session storage of a browser. If it is stored in session storage, the time
 * to live of that scope is determined by the duration of the session.
 *
 * @author florian
 *
 */
public class ConsentScope implements Serializable {

    private static final long serialVersionUID = -7933737886888841025L;

    private StorageMode storageMode = StorageMode.LOCAL;

    /**
     * The number of days after which the consent must be renewed at the latest
     */
    private int daysToLive = 14;

    /**
     * empty default constructor
     */
    public ConsentScope() {

    }

    /**
     * Create a new consent scope from a string which is created by the {@link ConsentScope#toString()} method of another ConsentScope, making this
     * effectively a cloning constructor Used when deserializing a consent scope from database
     * 
     * @param string representing a consent scope
     */
    public ConsentScope(String string) {
        if ("session".equalsIgnoreCase(string)) {
            this.storageMode = StorageMode.SESSION;
        } else if (StringUtils.isNotBlank(string) && string.matches("\\d+d")) {
            this.storageMode = StorageMode.LOCAL;
            this.daysToLive = Integer.parseInt(string.substring(0, string.length() - 1));
        } else {
            throw new IllegalArgumentException("String '" + string + "' is not a valid consent scope string");
        }
    }

    /*
     * String representation of the consentScope, used when serializing the scope to database.
     */
    @Override
    public String toString() {
        if (StorageMode.SESSION.equals(this.storageMode)) {
            return "session";
        }
        return Integer.toString(daysToLive) + "d";
    }

    /**
     * Get the storage mode, determining if the scope is limited to a http session or a span of time
     * 
     * @return the storageMode
     */
    public StorageMode getStorageMode() {
        return storageMode;
    }

    /**
     * Set the storageMode
     * 
     * @param storageMode the storageMode to set
     */
    public void setStorageMode(StorageMode storageMode) {
        this.storageMode = storageMode;
    }

    /**
     * get the number of days the consent is valid for
     * 
     * @return the daysToLive
     */
    public int getDaysToLive() {
        return daysToLive;
    }

    /**
     * set the number of days the consent is valid for
     * 
     * @param daysToLive the daysToLive to set
     */
    public void setDaysToLive(int daysToLive) {
        this.daysToLive = daysToLive;
    }

    /**
     * The scope within which the consent is valid
     * 
     * @author florian
     *
     */
    public enum StorageMode {
        /**
         * Consent is valid for a single browser and stored it its local storage
         */
        LOCAL,
        /**
         * Consent is valid for a single browser session and stored in the browser's session storage
         */
        SESSION;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * An object is equals to a consentScope if it is also a consentScope and has the same string representation
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return ((ConsentScope) obj).toString().equals(this.toString());
        }
        return false;
    }

}
