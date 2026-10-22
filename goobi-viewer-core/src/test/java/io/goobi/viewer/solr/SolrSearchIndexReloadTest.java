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
package io.goobi.viewer.solr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;

/**
 * Tests for the Solr client replacement in {@link SolrSearchIndex}, using a stalling mock client instead of a live index.
 */
class SolrSearchIndexReloadTest extends AbstractTest {

    private static final long TIMEOUT_SECONDS = 10;

    /** Signals that the stale client's close() has been entered. */
    private CountDownLatch closeEntered;
    /** Keeps the stale client's close() blocked until the test releases it. */
    private CountDownLatch releaseClose;
    /** Name of the thread that ran the stale client's close(). */
    private AtomicReference<String> closingThreadName;

    private Http2SolrClient staleClient;
    private SolrSearchIndex index;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        closeEntered = new CountDownLatch(1);
        releaseClose = new CountDownLatch(1);
        closingThreadName = new AtomicReference<>();

        staleClient = Mockito.mock(Http2SolrClient.class);
        // A base URL differing from the configured one makes checkReloadNeeded() take the replacement branch
        Mockito.when(staleClient.getBaseURL()).thenReturn(DataManager.getInstance().getConfiguration().getSolrUrl() + "/stale");
        Mockito.doAnswer(invocation -> {
            closingThreadName.set(Thread.currentThread().getName());
            closeEntered.countDown();
            releaseClose.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return null;
        }).when(staleClient).close();

        index = new SolrSearchIndex(staleClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        releaseClose.countDown();
        index.close();
    }

    /**
     * @see SolrSearchIndex#checkReloadNeeded()
     * @verifies return while the old client is still closing
     */
    @Test
    void checkReloadNeeded_shouldReturnWhileTheOldClientIsStillClosing() throws Exception {
        Thread caller = new Thread(index::checkReloadNeeded, "checkReloadNeeded-caller");
        caller.setDaemon(true);
        caller.start();
        caller.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertFalse(caller.isAlive(), "checkReloadNeeded() must return while the old client is still being closed");
    }

    /**
     * @see SolrSearchIndex#checkReloadNeeded()
     * @verifies close the replaced client off the calling thread
     */
    @Test
    void checkReloadNeeded_shouldCloseTheReplacedClientOffTheCallingThread() throws Exception {
        Thread caller = new Thread(index::checkReloadNeeded, "checkReloadNeeded-caller");
        caller.setDaemon(true);
        caller.start();

        assertTrue(closeEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the replaced client must still be closed");
        assertNotEquals(caller.getName(), closingThreadName.get(), "the replaced client must not be closed on the calling thread");
    }

    /**
     * @see SolrSearchIndex#replaceClient(org.apache.solr.client.solrj.SolrClient)
     * @verifies replace the client only once for the same stale client
     */
    @Test
    void replaceClient_shouldReplaceTheClientOnlyOnceForTheSameStaleClient() throws Exception {
        index.replaceClient(staleClient);
        index.replaceClient(staleClient);

        assertTrue(closeEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the stale client must be closed");
        awaitSettled();
        Mockito.verify(staleClient, Mockito.times(1)).close();
    }

    /**
     * @see SolrSearchIndex#replaceClient(org.apache.solr.client.solrj.SolrClient)
     * @verifies replace the client only once when called concurrently
     */
    @Test
    void replaceClient_shouldReplaceTheClientOnlyOnceWhenCalledConcurrently() throws Exception {
        int callerCount = 16;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(callerCount);
        for (int i = 0; i < callerCount; i++) {
            Thread caller = new Thread(() -> {
                try {
                    startGate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    index.replaceClient(staleClient);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "replaceClient-caller-" + i);
            caller.setDaemon(true);
            caller.start();
        }
        startGate.countDown();

        assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "all callers must return");
        assertTrue(closeEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the stale client must be closed");
        awaitSettled();
        Mockito.verify(staleClient, Mockito.times(1)).close();
    }

    /**
     * Gives a superfluous second close() the chance to occur before the absence of one is asserted.
     */
    private static void awaitSettled() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }
}
