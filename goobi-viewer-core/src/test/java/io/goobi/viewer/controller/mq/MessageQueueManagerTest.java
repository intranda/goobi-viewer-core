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
package io.goobi.viewer.controller.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessageQueueManagerTest {

    /**
     * A normal ActiveMQ message ID must be embedded unchanged.
     * @verifies normal id no escaping
     * @see MessageQueueManager#buildMessageIdSelector
     */
    @Test
    void buildMessageIdSelector_shouldNormalIdNoEscaping() {
        String id = "ID:hostname-12345-1711670000000-1:1:1:1:1";
        assertEquals("JMSMessageID='ID:hostname-12345-1711670000000-1:1:1:1:1'",
                MessageQueueManager.buildMessageIdSelector(id));
    }

    /**
     * A single quote in the message ID must be doubled to produce valid JMS selector syntax.
     * Without escaping, JMSMessageID=''' is a syntax error; after escaping it becomes
     * JMSMessageID='''' which is a valid (non-matching) selector.
     * @verifies single quote is escaped
     * @see MessageQueueManager#buildMessageIdSelector
     */
    @Test
    void buildMessageIdSelector_shouldSingleQuoteIsEscaped() {
        assertEquals("JMSMessageID=''''", MessageQueueManager.buildMessageIdSelector("'"));
    }

    /**
     * Multiple quotes are all doubled.
     * @verifies multiple quotes all escaped
     * @see MessageQueueManager#buildMessageIdSelector
     */
    @Test
    void buildMessageIdSelector_shouldMultipleQuotesAllEscaped() {
        assertEquals("JMSMessageID='''it''s'''",
                MessageQueueManager.buildMessageIdSelector("'it's'"));
    }

    /**
     * Empty string is safe: produces an empty-string selector.
     * @verifies empty string produces empty selector
     * @see MessageQueueManager#buildMessageIdSelector
     */
    @Test
    void buildMessageIdSelector_shouldEmptyStringProducesEmptySelector() {
        assertEquals("JMSMessageID=''", MessageQueueManager.buildMessageIdSelector(""));
    }

    /**
     * A normal message type is embedded unchanged.
     * @verifies normal type no escaping
     * @see MessageQueueManager#buildJmsTypeSelector
     */
    @Test
    void buildJmsTypeSelector_shouldNormalTypeNoEscaping() {
        assertEquals("JMSType='PRERENDER_PDF'",
                MessageQueueManager.buildJmsTypeSelector("PRERENDER_PDF"));
    }

    /**
     * A single quote in the type must be doubled to produce valid JMS selector syntax.
     * @verifies single quote is escaped
     * @see MessageQueueManager#buildJmsTypeSelector
     */
    @Test
    void buildJmsTypeSelector_shouldSingleQuoteIsEscaped() {
        assertEquals("JMSType=''''", MessageQueueManager.buildJmsTypeSelector("'"));
    }
}
