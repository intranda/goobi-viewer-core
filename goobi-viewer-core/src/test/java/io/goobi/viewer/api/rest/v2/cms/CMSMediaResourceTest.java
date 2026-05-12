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
package io.goobi.viewer.api.rest.v2.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import jakarta.ws.rs.BadRequestException;

class CMSMediaResourceTest extends AbstractDatabaseEnabledTest {

    CMSMediaResource resource;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // v2 CMSMediaResource has only a default constructor with @Context fields;
        // we don't need to wire the IDAO for the helper-static tests below.
        resource = new CMSMediaResource();
    }

    /**
     * @see CMSMediaResource#listMediaFilenames(Path, int, int)
     * @verifies respect first and count parameters
     */
    @Test
    void listMediaFilenames_shouldRespectFirstAndCountParameters(@TempDir Path folder) throws Exception {
        for (int i = 0; i < 5; i++) {
            Files.createFile(folder.resolve("file" + i + ".jpg"));
        }
        List<String> slice = CMSMediaResource.listMediaFilenames(folder, 1, 2);
        assertEquals(2, slice.size(), "first=1,count=2 must return two filenames");
    }

    /**
     * @see CMSMediaResource#listMediaFilenames(Path, int, int)
     * @verifies return empty list when count is zero
     */
    @Test
    void listMediaFilenames_shouldReturnEmptyListWhenCountIsZero(@TempDir Path folder) throws Exception {
        Files.createFile(folder.resolve("only.jpg"));
        List<String> slice = CMSMediaResource.listMediaFilenames(folder, 0, 0);
        assertEquals(0, slice.size());
    }

    /**
     * @see CMSMediaResource#getAllFiles(int, int)
     * @verifies reject negative first
     */
    @Test
    void getAllFiles_shouldRejectNegativeFirst() {
        assertThrows(BadRequestException.class, () -> resource.getAllFiles(-1, 10));
    }

    /**
     * @see CMSMediaResource#getAllFiles(int, int)
     * @verifies reject negative count
     */
    @Test
    void getAllFiles_shouldRejectNegativeCount() {
        assertThrows(BadRequestException.class, () -> resource.getAllFiles(0, -1));
    }
}
