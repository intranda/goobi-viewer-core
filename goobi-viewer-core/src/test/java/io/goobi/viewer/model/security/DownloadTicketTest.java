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


import org.junit.Assert;
import org.junit.Test;

public class DownloadTicketTest {
    
    /**
    * @see DownloadTicket#checkPassword(String)
    * @verifies check password correctly
    */
    @Test
    public void checkPassword_shouldCheckPasswordCorrectly() throws Exception {
        DownloadTicket ticket = new DownloadTicket();
        ticket.setPasswordHash("$2a$10$H580saN37o2P03A5myUCm.V0ac/lO.79AfkiNjVhDzljqS3RGojzO");
        
        Assert.assertFalse(ticket.checkPassword("foo"));
        Assert.assertTrue(ticket.checkPassword("halbgeviertstrich"));
    }
}