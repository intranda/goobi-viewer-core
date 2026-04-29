package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnifaces.cdi.PushContext;

class SocketBeanTest {

    private static final String UPDATE = "update";
    private static final String TEST_MESSAGE = "Test Message";

    /**
     * @verifies forward update message via push context
     */
    @Test
    void send_shouldForwardUpdateMessageViaPushContext() throws InterruptedException {
        PushContext push = Mockito.spy(PushContext.class);
        SocketBean bean = new SocketBean(0, push);
        bean.send(TEST_MESSAGE);
        Thread.sleep(2);//sleep to avoid race condition, because message is sent asynchronously
        Mockito.verify(push, Mockito.times(1)).send(UPDATE);

    }

    /**
     * Verifies that close() terminates the executor promptly even when a long idle period is configured.
     * With shutdownNow() the sleeping scheduled thread is interrupted immediately. With the old
     * service.close() / shutdown() approach the call would block until the next scheduled run.
     * @verifies terminate executor promptly
     */
    @Test
    void close_shouldTerminateExecutorPromptly() {
        PushContext push = Mockito.spy(PushContext.class);
        SocketBean bean = new SocketBean(30, push); // 30-second schedule

        long start = System.nanoTime();
        bean.close();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs < 2000, "close() should return promptly, took " + elapsedMs + "ms");
    }

}
