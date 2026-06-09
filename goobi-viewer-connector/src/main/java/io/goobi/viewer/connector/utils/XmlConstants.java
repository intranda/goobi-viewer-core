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
package io.goobi.viewer.connector.utils;

/**
 * 
 */
public final class XmlConstants {

    private XmlConstants() {
    }

    public static final String ATT_NAME_OBJID = "OBJID";
    public static final String ATT_NAME_SCHEME = "scheme";

    public static final String ELE_NAME_ERROR = "error";
    public static final String ELE_NAME_HEADER = "header";
    public static final String ELE_NAME_IDENTIFIER = "identifier";
    public static final String ELE_NAME_METADATA = "metadata";
    public static final String ELE_NAME_RECORD = "record";
    public static final String ELE_NAME_SETSPEC = "setSpec";
}
