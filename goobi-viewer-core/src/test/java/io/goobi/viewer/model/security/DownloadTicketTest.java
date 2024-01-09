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


import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

public class DownloadTicketTest extends AbstractTest {
    
    /**
    * @see DownloadTicket#checkPassword(String)
    * @verifies check password correctly
    */
    @Test
    public void checkPassword_shouldCheckPasswordCorrectly() throws Exception {
        DownloadTicket ticket = new DownloadTicket();
        ticket.setPasswordHash("$2a$10$H580saN37o2P03A5myUCm.V0ac/lO.79AfkiNjVhDzljqS3RGojzO");
        
        Assertions.assertFalse(ticket.checkPassword("foo"));
        Assertions.assertTrue(ticket.checkPassword("halbgeviertstrich"));
    }

    /**
     * @see DownloadTicket#isActive()
     * @verifies return true if ticket active
     */
    @Test
    public void isActive_shouldReturnTrueIfTicketActive() throws Exception {
        DownloadTicket ticket = new DownloadTicket();
        ticket.setPasswordHash("abcde");
        ticket.setExpirationDate(LocalDateTime.now().plusDays(1));
        Assertions.assertFalse(ticket.isExpired());
    }

    /**
     * @see DownloadTicket#isExpired()
     * @verifies return true if expiration date before now
     */
    @Test
    public void isExpired_shouldReturnTrueIfExpirationDateBeforeNow() throws Exception {
        DownloadTicket ticket = new DownloadTicket();
        ticket.setExpirationDate(LocalDateTime.now().minusDays(1));
        Assertions.assertTrue(ticket.isExpired());
    }

    /**
     * @see DownloadTicket#isExpired()
     * @verifies return false if expiration date after now
     */
    @Test
    public void isExpired_shouldReturnFalseIfExpirationDateAfterNow() throws Exception {
        DownloadTicket ticket = new DownloadTicket();
        ticket.setExpirationDate(LocalDateTime.now().plusDays(1));
        Assertions.assertFalse(ticket.isExpired());
    }
}