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
package io.goobi.viewer.model.misc;

public class EmailRecipient {

    private final String id;
    private final String label;
    private final String emailAddress;
    private final boolean defaultRecipient;

    /**
     *
     * @param id unique identifier for this recipient
     * @param label display name of the recipient
     * @param emailAddress email address of the recipient
     * @param defaultRecipient true if this is the default recipient
     */
    public EmailRecipient(String id, String label, String emailAddress, boolean defaultRecipient) {
        this.id = id;
        this.label = label;
        this.emailAddress = emailAddress;
        this.defaultRecipient = defaultRecipient;
    }

    /**

     */
    public String getLabel() {
        return label;
    }

    /**

     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**

     */
    public boolean isDefaultRecipient() {
        return defaultRecipient;
    }

    /**

     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return emailAddress;
    }
}
