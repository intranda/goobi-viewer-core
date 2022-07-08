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
 * Access permission check outcome. Apart from access granted true/false status, additional attributes can be defined here.
 */
public class AccessPermission implements Serializable {

    private static final long serialVersionUID = 7835995693629510107L;

    private boolean granted = false;
    private boolean ticketRequired = false;

    /**
     * @return {@link AccessPermission} with denied status
     */
    public static AccessPermission denied() {
        return new AccessPermission().setGranted(false);
    }

    /**
     * @return {@link AccessPermission} with granted status
     */
    public static AccessPermission granted() {
        return new AccessPermission().setGranted(true);
    }

    /**
     * @return the granted
     */
    public boolean isGranted() {
        return granted;
    }

    /**
     * @param granted the granted to set
     * @return this
     */
    public AccessPermission setGranted(boolean granted) {
        this.granted = granted;
        return this;
    }

    /**
     * @return the ticketRequired
     */
    public boolean isTicketRequired() {
        return ticketRequired;
    }

    /**
     * @param ticketRequired the ticketRequired to set
     * @return this
     */
    public AccessPermission setTicketRequired(boolean ticketRequired) {
        this.ticketRequired = ticketRequired;
        return this;
    }
}
