package io.goobi.viewer.managedbeans;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.omnifaces.cdi.PushContext;

class SocketBeanTest {

    private static final String UPDATE = "update";
    private static final String TEST_MESSAGE = "Test Message";

    @Test
    void test() throws InterruptedException {
        PushContext push = Mockito.spy(PushContext.class);
        SocketBean bean = new SocketBean(0, push);
        bean.send(TEST_MESSAGE);
        Thread.sleep(1);//sleep to avoid race condition, because message is sent asynchronously
        Mockito.verify(push, Mockito.times(1)).send(UPDATE);

    }

}
