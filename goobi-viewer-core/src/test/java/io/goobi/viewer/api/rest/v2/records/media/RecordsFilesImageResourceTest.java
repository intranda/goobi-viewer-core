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
package io.goobi.viewer.api.rest.v2.records.media;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;

class RecordsFilesImageResourceTest {

    /**
     * @verifies reject illegal chars
     * @see RecordsFilesImageResource#requireValidPi
     */
    @Test
    void requireValidPi_shouldRejectIllegalChars() {
        // Characters that cause ImageResource to throw ContentLibException (HTTP 500)
        // when building file:// URIs from the PI as folder name
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi(" "));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("|"));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("<xml>"));
        assertThrows(BadRequestException.class, () -> RecordsFilesImageResource.requireValidPi("%00"));
    }
}
