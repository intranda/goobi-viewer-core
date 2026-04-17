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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.activemq.ActiveMQConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;

class DefaultQueueListenerShutdownTest {

    /**
     * Verifies that startListener exits the message loop when isTransportFailed() returns true.
     * Before the fix, the while loop only checked shouldStop, so a broker-side disconnect would
     * cause the listener to spin indefinitely rather than exit cleanly.
     * @verifies exit message loop when transport fails
     */
    @Test
    void startListener_shouldExitMessageLoopWhenTransportFails() throws JMSException {
        MessageQueueManager broker = Mockito.mock(MessageQueueManager.class);
        DefaultQueueListener listener = new DefaultQueueListener(broker, "testQueue");

        ActiveMQConnection conn = Mockito.mock(ActiveMQConnection.class);
        // isTransportFailed() returns false twice, then true — loop should exit after 2 iterations
        Mockito.when(conn.isTransportFailed()).thenReturn(false, false, true);

        MessageConsumer consumer = Mockito.mock(MessageConsumer.class);
        Mockito.when(consumer.receive(1000)).thenReturn(null);

        Session sess = Mockito.mock(Session.class);
        Queue queue = Mockito.mock(Queue.class);
        Mockito.when(conn.createSession(false, Session.CLIENT_ACKNOWLEDGE)).thenReturn(sess);
        Mockito.when(sess.createQueue("testQueue")).thenReturn(queue);
        Mockito.when(sess.createConsumer(queue)).thenReturn(consumer);

        listener.startListener("testQueue", conn);

        // The loop ran exactly 2 iterations before isTransportFailed() turned true
        Mockito.verify(consumer, Mockito.times(2)).receive(1000);
    }

    /**
     * Verifies that waitForMessage does not sleep for 3 seconds when shouldStop is true.
     * The JMSException catch block must skip both the sleep and the error log when the
     * listener is shutting down, so Tomcat does not have to wait.
     * @verifies not sleep when shutting down
     */
    @Test
    void waitForMessage_shouldNotSleepWhenShuttingDown() throws JMSException {
        MessageQueueManager broker = Mockito.mock(MessageQueueManager.class);
        DefaultQueueListener listener = new DefaultQueueListener(broker, "testQueue");
        listener.close(); // sets shouldStop = true; thread is null so join is skipped

        Session sess = Mockito.mock(Session.class);
        MessageConsumer consumer = Mockito.mock(MessageConsumer.class);
        Mockito.when(consumer.receive(1000)).thenThrow(new JMSException("transport failed"));

        long start = System.nanoTime();
        listener.waitForMessage(sess, consumer);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // Without the fix this would block for ~3000ms; with the fix it returns immediately
        assertTrue(elapsedMs < 500, "waitForMessage should not sleep during shutdown, took " + elapsedMs + "ms");
    }
}
