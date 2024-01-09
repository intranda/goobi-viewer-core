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
package io.goobi.viewer.model.job.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.ActiveMQConfig;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.QuartzBean;

public class QuartzListenerTest extends AbstractDatabaseEnabledTest{

    IDAO dao;
    Configuration config;
    private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";
    QuartzListener listener;
    MessageQueueManager broker;
    Path schedulerDirectory;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.dao = DataManager.getInstance().getDao();
        this.config = DataManager.getInstance().getConfiguration();
        ActiveMQConfig activeMQConfig = new ActiveMQConfig(Paths.get(activeMqConfigPath));        
        MessageQueueManager tempBroker = new MessageQueueManager(activeMQConfig, dao);
        broker = Mockito.spy(tempBroker);
        listener = new QuartzListener(dao, config, broker);
        clearDatabase(dao);
        schedulerDirectory = Paths.get(activeMQConfig.getSchedulerDirectory());
        if(Files.exists(schedulerDirectory)) {
            FileUtils.deleteDirectory(schedulerDirectory.toFile());
        }
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        broker.closeMessageServer();
        clearDatabase(dao);
        if(schedulerDirectory != null && Files.exists(schedulerDirectory)) {
            FileUtils.deleteDirectory(schedulerDirectory.toFile());
        }
    }
    
    private void clearDatabase(IDAO dao) throws DAOException {
        List<RecurringTaskTrigger> triggers = dao.getRecurringTaskTriggers();
        for (RecurringTaskTrigger trigger : triggers) {
            dao.deleteRecurringTaskTrigger(trigger.getId());
        }
    }
    
    @Test
    void testStartJobs() throws DAOException, SchedulerException, IOException {
        ServletContext context = Mockito.mock(ServletContext.class);
        ServletContextEvent contextEvt = Mockito.mock(ServletContextEvent.class);
        Mockito.when(contextEvt.getServletContext()).thenReturn(context);
        Mockito.when(context.getRealPath(Mockito.anyString())).thenReturn("/opt/digiverso/viewer/app");
        
        listener.contextInitialized(contextEvt);
        
        QuartzBean bean = new QuartzBean();
        assertEquals(5, bean.getActiveJobs().size());
    }


}
