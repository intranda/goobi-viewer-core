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
package io.goobi.viewer.api.rest.v1;

import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

/**
 * <p>
 * ViewerApplication class.
 * </p>
 */
@ApplicationPath(ApiUrls.API)
@ViewerRestServiceBinding
public class Application extends ResourceConfig {

    private static final Logger logger = LogManager.getLogger(Application.class);

    /**
     * <p>
     * Constructor for ViewerApplication.
     * </p>
     * 
     * @param servletConfig
     */
    public Application(@Context ServletConfig servletConfig) {
        super();
        PersistentStorageBean applicationBean = (PersistentStorageBean) BeanUtils.getBeanByName("applicationBean", PersistentStorageBean.class);
        AbstractBinder binder = new AbstractBinder() {

            @Override
            protected void configure() {
                String apiUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
                apiUrl = apiUrl.replace("/rest", "/api/v1");
                bind(new ApiUrls(apiUrl)).to(ApiUrls.class);
                CMSTemplateManager templateManager = applicationBean.getTemplateManager();
                MessageQueueManager messageBroker = applicationBean.getMessageBroker();
                bind(templateManager).to(CMSTemplateManager.class);
                bind(messageBroker).to(MessageQueueManager.class);
                bind(DataManager.getInstance().getConfiguration()).to(Configuration.class);
                bind(ContentServerCacheManager.getInstance()).to(ContentServerCacheManager.class);
                try {
                    bind(DataManager.getInstance().getDao()).to(IDAO.class);
                } catch (DAOException e) {
                    logger.fatal("Unable to instantiate DAO for use in rest api", e);
                }
            }
        };
        this.init(binder);
    }

    /**
     * Constructor with custom injection binder for tests.
     *
     * @param binder
     */
    public Application(AbstractBinder binder) {
        super();
        this.init(binder);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(ContentServerCacheManager.getInstance()).to(ContentServerCacheManager.class);
            }
        });
    }

    private void init(AbstractBinder injectionBinder) {
        //Allow receiving multi-part POST requests
        register(MultiPartFeature.class);
        //inject properties into Resources classes
        register(injectionBinder);
        //define Java packages to observe
        packages(true, "io.goobi.viewer.api.rest.v1");
        packages(true, "io.goobi.viewer.api.rest.filters");
        packages(true, "io.goobi.viewer.api.rest.exceptions");
        packages(true, "io.swagger");

    }

}
