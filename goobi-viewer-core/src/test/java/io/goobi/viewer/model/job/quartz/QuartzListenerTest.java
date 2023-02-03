package io.goobi.viewer.model.job.quartz;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

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

public class QuartzListenerTest extends AbstractDatabaseEnabledTest{

    IDAO dao;
    Configuration config;
    
    @Test
    public void testStartJobs() throws DAOException, SchedulerException {
        this.dao = DataManager.getInstance().getDao();
        this.config = DataManager.getInstance().getConfiguration();
        
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
