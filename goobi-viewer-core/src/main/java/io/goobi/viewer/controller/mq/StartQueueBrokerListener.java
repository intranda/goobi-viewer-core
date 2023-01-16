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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;

@WebListener
public class StartQueueBrokerListener implements ServletContextListener {

    private static final Logger log = LogManager.getLogger(StartQueueBrokerListener.class);

    private RMIConnectorServer rmiServer;
    private BrokerService broker;
    private List<DefaultQueueListener> listeners = new ArrayList<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            // JMX/RMI part taken from: https://vafer.org/blog/20061010091658/
            String address = "localhost";
            int namingPort = 1099;
            int protocolPort = 0;
            try {
                RMIServerSocketFactory serverFactory = new RMIServerSocketFactoryImpl(InetAddress.getByName(address));

                LocateRegistry.createRegistry(namingPort, null, serverFactory);

                StringBuilder url = new StringBuilder();
                url.append("service:jmx:");
                url.append("rmi://").append(address).append(':').append(protocolPort).append("/jndi/");
                url.append("rmi://").append(address).append(':').append(namingPort).append("/connector");

                Map<String, Object> env = new HashMap<>();
                env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverFactory);

                rmiServer = new RMIConnectorServer(
                        new JMXServiceURL(url.toString()),
                        env,
                        ManagementFactory.getPlatformMBeanServer());

                rmiServer.start();

            } catch (IOException e1) {
                log.error("error starting JMX connector. Will not start internal MessageBroker. Exception: {}", e1);
                return;
            }
            String activeMqConfig = DataManager.getInstance().getConfiguration().getActiveMQConfigPath();
            try {
                broker = BrokerFactory.createBroker("xbean:file:" + activeMqConfig, false);
                broker.setUseJmx(true);
                broker.start();

            } catch (Exception e) {
                log.error(e);
            }

            try {
                for (int i = 0; i < DataManager.getInstance().getConfiguration().getNumberOfParallelMessages(); i++) {
                    DefaultQueueListener listener = new DefaultQueueListener();
                    listener.register(DataManager.getInstance().getConfiguration().getActiveMQUsername(),
                            DataManager.getInstance().getConfiguration().getActiveMQPassword(), "viewer");
                    listeners.add(listener);
                }

            } catch (JMSException e) {
                log.error(e);
            }
        }
        sce.getServletContext().setAttribute("BrokerService", this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (rmiServer != null) {
                rmiServer.stop();
            }
            for (DefaultQueueListener l : listeners) {
                l.close();
            }

            if (broker != null) {
                broker.stop();
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    public BrokerService getBroker() {
        return broker;
    }

}
