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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.utils.XmlConstants;

class ErrorCodeTest extends AbstractTest {

    /**
     * @see ErrorCode#getBadArgument()
     * @verifies construct element correctly
     */
    @Test
    void getBadArgument_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadArgument();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("badArgument", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_BAD_ARGUMENT, ele.getText());
    }

    /**
     * @see ErrorCode#getBadResumptionToken()
     * @verifies construct element correctly
     */
    @Test
    void getBadResumptionToken_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadResumptionToken();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("badResumptionToken", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_BAD_RESUMPTION_TOKEN, ele.getText());
    }

    /**
     * @see ErrorCode#getBadVerb()
     * @verifies construct element correctly
     */
    @Test
    void getBadVerb_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getBadVerb();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("badVerb", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_BAD_VERB, ele.getText());
    }

    /**
     * @see ErrorCode#getCannotDisseminateFormat()
     * @verifies construct element correctly
     */
    @Test
    void getCannotDisseminateFormat_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getCannotDisseminateFormat();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("cannotDisseminateFormat", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_CANNOT_DISSEMINATE_FORMAT, ele.getText());
    }

    /**
     * @see ErrorCode#getIdDoesNotExist()
     * @verifies construct element correctly
     */
    @Test
    void getIdDoesNotExist_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getIdDoesNotExist();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("idDoesNotExist", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_ID_DOES_NOT_EXIST, ele.getText());
    }

    /**
     * @see ErrorCode#getNoMetadataFormats()
     * @verifies construct element correctly
     */
    @Test
    void getNoMetadataFormats_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoMetadataFormats();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("noMetadataFormats", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_NO_METADATA_FORMATS, ele.getText());
    }

    /**
     * @see ErrorCode#getNoRecordsMatch()
     * @verifies construct element correctly
     */
    @Test
    void getNoRecordsMatch_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoRecordsMatch();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("noRecordsMatch", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_NO_RECORDS_MATCH, ele.getText());
    }

    /**
     * @see ErrorCode#getNoSetHierarchy()
     * @verifies construct element correctly
     */
    @Test
    void getNoSetHierarchy_shouldConstructElementCorrectly() throws Exception {
        Element ele = new ErrorCode().getNoSetHierarchy();
        Assertions.assertNotNull(ele);
        Assertions.assertEquals(XmlConstants.ELE_NAME_ERROR, ele.getName());
        Assertions.assertEquals("noSetHierarchy", ele.getAttributeValue("code"));
        Assertions.assertEquals(ErrorCode.BODY_NO_SET_HIERARCHY, ele.getText());
    }
}