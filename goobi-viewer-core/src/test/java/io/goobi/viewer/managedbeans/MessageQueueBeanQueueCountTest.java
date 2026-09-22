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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.mq.MessageQueueManager;

/**
 * Unit tests for {@link MessageQueueBean}'s cheap, cached total-message count used by the admin topbar. These use a
 * mocked {@link MessageQueueManager} and do not start an embedded broker, so they run independently of the
 * broker-backed {@link MessageQueueBeanTest}.
 */
class MessageQueueBeanQueueCountTest extends AbstractTest {

    /**
     * @verifies return total queue size from broker
     */
    @Test
    void getTotalMessagesInQueueCount_shouldReturnTotalQueueSizeFromBroker() {
        MessageQueueManager broker = Mockito.mock(MessageQueueManager.class);
        Mockito.when(broker.getTotalQueueSize()).thenReturn(7L);
        MessageQueueBean bean = new MessageQueueBean(broker);
        assertEquals(7, bean.getTotalMessagesInQueueCount());
    }

    /**
     * @verifies cache count within ttl
     */
    @Test
    void getTotalMessagesInQueueCount_shouldCacheCountWithinTtl() {
        MessageQueueManager broker = Mockito.mock(MessageQueueManager.class);
        Mockito.when(broker.getTotalQueueSize()).thenReturn(3L);
        MessageQueueBean bean = new MessageQueueBean(broker);
        assertEquals(3, bean.getTotalMessagesInQueueCount());
        assertEquals(3, bean.getTotalMessagesInQueueCount());
        assertEquals(3, bean.getTotalMessagesInQueueCount());
        // within the cache TTL the broker is queried only once, not on every render
        Mockito.verify(broker, Mockito.times(1)).getTotalQueueSize();
    }
}
