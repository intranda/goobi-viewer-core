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
package io.goobi.viewer.api.rest.model.tasks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;

class TaskManagerTest extends AbstractDatabaseEnabledTest {

    /**
     * @see TaskManager#deleteExpiredDownloadTickets()
     * @verifies delete all expired tickets
     */
    @Test
    void deleteExpiredDownloadTickets_shouldDeleteAllExpiredTickets() throws Exception {
        Assertions.assertNotNull(DataManager.getInstance().getDao().getTicket(1L));
        Assertions.assertEquals(1, TaskManager.deleteExpiredDownloadTickets());
        Assertions.assertNull(DataManager.getInstance().getDao().getTicket(1L));
    }
}