package io.goobi.viewer.model.job.quartz;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageBroker;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.QuartzBean;
import io.goobi.viewer.model.job.TaskType;

public class QuartzListenerTest extends AbstractDatabaseEnabledTest{

    IDAO dao;
    Configuration config;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        this.dao = DataManager.getInstance().getDao();
        this.config = DataManager.getInstance().getConfiguration();
        clearDatabase(dao);
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clearDatabase(dao);
    }
    
    private void clearDatabase(IDAO dao) throws DAOException {
        List<RecurringTaskTrigger> triggers = dao.getRecurringTaskTriggers();
        for (RecurringTaskTrigger trigger : triggers) {
            dao.deleteRecurringTaskTrigger(trigger.getId());
        }
    }
    
    @Test
    public void testStartJobs() throws DAOException, SchedulerException {
        ServletContext context = Mockito.mock(ServletContext.class);
        ServletContextEvent contextEvt = Mockito.mock(ServletContextEvent.class);
        Mockito.when(contextEvt.getServletContext()).thenReturn(context);
        Mockito.when(context.getRealPath(Mockito.anyString())).thenReturn("/opt/digiverso/viewer/app");
        MessageBroker tempBroker = new MessageBroker(dao, MessageBroker.generateTicketHandlers());
        MessageBroker broker = Mockito.spy(tempBroker);
        QuartzListener listener = new QuartzListener(dao, config, broker);
        
        listener.contextInitialized(contextEvt);
        
        QuartzBean bean = new QuartzBean();
        assertEquals(5, bean.getActiveJobs().size());
    }


}
