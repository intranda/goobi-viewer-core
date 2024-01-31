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
package io.goobi.viewer.model.security;

import java.io.Serializable;

/**
 * 
 */
public class CopyrightIndicatorStatus implements Serializable {

    private static final long serialVersionUID = -9046091644794397616L;

    public enum Status {
        OPEN,
        PARTIAL,
        LOCKED;

        /**
         * 
         * @param name Status name to match
         * @return {@link Status}
         * @should return correct value
         */
        public static Status getByName(String name) {
            for (Status status : values()) {
                if (status.name().equals(name)) {
                    return status;
                }
            }

            return null;
        }
    }

    private final Status status;
    private final String description;

    public CopyrightIndicatorStatus(Status status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
