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
package io.goobi.viewer.model.email;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.export.RISExport;
import jakarta.mail.Message;

class EMailSenderTest extends AbstractTest {

    /**
     * @see RISExport#RISExport()
     * @verifies create message correctly
     */
    @Test
    void createMessage_shouldCreateMessageCorrectly() throws Exception {
        EMailSender sender = new EMailSender(DataManager.getInstance().getConfiguration());
        Message msg = sender.createMessage(Collections.singletonList("recipient@example.com"), Collections.singletonList("cc@example.com"),
                Collections.singletonList("bcc@example.com"), Collections.singletonList("reply-to@example.com"), "subject", "body",
                sender.createSession(false, false));
        Assertions.assertNotNull(msg);
        Assertions.assertEquals(3, msg.getAllRecipients().length);
        String[] replyTo = msg.getHeader("reply-to");
        Assertions.assertNotNull(replyTo);
        Assertions.assertEquals(1, replyTo.length);
        Assertions.assertEquals("reply-to@example.com", replyTo[0]);
    }
}