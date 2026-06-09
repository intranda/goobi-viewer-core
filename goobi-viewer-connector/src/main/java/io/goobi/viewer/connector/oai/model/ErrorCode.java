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

import org.jdom2.Element;
import org.jdom2.Namespace;

import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.utils.XmlConstants;

/**
 * This class provides all error codes for the OAI protocol.
 */
public class ErrorCode {

    static final String BODY_BAD_ARGUMENT =
            "The request includes illegal arguments, is missing required arguments, includes a repeated argument,"
                    + " or values for arguments have an illegal syntax.";
    static final String BODY_BAD_RESUMPTION_TOKEN = "The value of the resumptionToken argument is invalid or expired.";
    static final String BODY_BAD_VERB =
            "Value of the verb argument is not a legal OAI-PMH verb, the verb argument is missing, or the verb argument is repeated.";
    static final String BODY_CANNOT_DISSEMINATE_FORMAT =
            "The metadata format identified by the value given for the metadataPrefix argument is not supported by the item or by the repository.";
    static final String BODY_ID_DOES_NOT_EXIST = "The value of the identifier argument is unknown or illegal in this repository.";
    static final String BODY_NO_METADATA_FORMATS = "There are no metadata formats available for the specified item.";
    static final String BODY_NO_RECORDS_MATCH =
            "The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.";
    static final String BODY_NO_SET_HIERARCHY = "The repository does not support sets.";

    private Namespace xmlns = null;

    /**
     * <p>
     * Constructor for ErrorCode.
     * </p>
     */
    public ErrorCode() {
        xmlns = DataManager.getInstance().getConfiguration().getStandardNameSpace();
    }

    /**
     * Required argument is missing, argument has wrong syntax or argument has invalid value.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getBadArgument() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badArgument");
        error.setText(BODY_BAD_ARGUMENT);
        return error;
    }

    /**
     * resumptionToken is expired, invalid or not found.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getBadResumptionToken() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badResumptionToken");
        error.setText(BODY_BAD_RESUMPTION_TOKEN);
        return error;
    }

    /**
     * The verb argument is missing or has invalid value.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getBadVerb() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "badVerb");
        error.setText(BODY_BAD_VERB);
        return error;
    }

    /**
     * The metadataPrefix argument has invalid value.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getCannotDisseminateFormat() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "cannotDisseminateFormat");
        error.setText(
                BODY_CANNOT_DISSEMINATE_FORMAT);
        return error;
    }

    /**
     * The identifier does not exist.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getIdDoesNotExist() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "idDoesNotExist");
        error.setText(BODY_ID_DOES_NOT_EXIST);
        return error;
    }

    /**
     * For the given identifier the metadata format used in metadataPrefix is not provided.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getNoMetadataFormats() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noMetadataFormats");
        error.setText(BODY_NO_METADATA_FORMATS);
        return error;
    }

    /**
     * The query returns no hits.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getNoRecordsMatch() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noRecordsMatch");
        error.setText(BODY_NO_RECORDS_MATCH);
        return error;
    }

    /**
     * There are no sets in repository.
     *
     * @return a {@link org.jdom2.Element} object.
     * @should construct element correctly
     */
    public Element getNoSetHierarchy() {
        Element error = new Element(XmlConstants.ELE_NAME_ERROR, xmlns);
        error.setAttribute("code", "noSetHierarchy");
        error.setText(BODY_NO_SET_HIERARCHY);
        return error;
    }
}
