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
package io.goobi.viewer.model.job.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.security.tickets.AccessTicket;
import io.goobi.viewer.model.security.tickets.AccessTicket.AccessTicketType;

class PurgeExpiredDownloadsHandlerTest extends AbstractDatabaseEnabledTest {

    /**
     * @see PurgeExpiredDownloadsHandler#call(ViewerMessage,io.goobi.viewer.controller.mq.MessageQueueManager)
     * @verifies delete expired tickets of every type
     */
    @Test
    void call_shouldDeleteExpiredTicketsOfEveryType() throws Exception {
        IDAO dao = DataManager.getInstance().getDao();

        AccessTicket recordTicket = createTicket(AccessTicketType.RECORD, DateTools.now().minusDays(1));
        assertTrue(dao.addTicket(recordTicket));

        assertEquals(MessageStatus.FINISH,
                new PurgeExpiredDownloadsHandler().call(new ViewerMessage(TaskType.PURGE_EXPIRED_DOWNLOAD_TICKETS.name()), null));

        assertNull(dao.getTicket(recordTicket.getId()));
        // The expired download ticket from the test data set is gone as well
        assertNull(dao.getTicket(1L));
    }

    /**
     * @see PurgeExpiredDownloadsHandler#call(ViewerMessage,io.goobi.viewer.controller.mq.MessageQueueManager)
     * @verifies keep tickets that have not expired
     */
    @Test
    void call_shouldKeepTicketsThatHaveNotExpired() throws Exception {
        IDAO dao = DataManager.getInstance().getDao();

        AccessTicket recordTicket = createTicket(AccessTicketType.RECORD, DateTools.now().plusDays(1));
        assertTrue(dao.addTicket(recordTicket));

        assertEquals(MessageStatus.FINISH,
                new PurgeExpiredDownloadsHandler().call(new ViewerMessage(TaskType.PURGE_EXPIRED_DOWNLOAD_TICKETS.name()), null));

        assertNotNull(dao.getTicket(recordTicket.getId()));
        // Ticket 3 is a pending request without an expiration date
        assertNotNull(dao.getTicket(3L));
    }

    private static AccessTicket createTicket(AccessTicketType type, LocalDateTime expirationDate) {
        AccessTicket ticket = new AccessTicket();
        ticket.setType(type);
        ticket.setEmail("user3@example.com");
        ticket.setPi("PPN789");
        ticket.setPasswordHash("$2a$10$H580saN37o2P03A5myUCm.V0ac/lO.79AfkiNjVhDzljqS3RGojzO");
        ticket.setExpirationDate(expirationDate);
        return ticket;
    }
}
