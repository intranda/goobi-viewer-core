/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.controller.DataManager;

/**
 * <p>
 * ViewerApplication class.
 * </p>
 */
@ApplicationPath("/api/v1")
@ViewerRestServiceBinding
public class Application extends ResourceConfig {

    /**
     * <p>
     * Constructor for ViewerApplication.
     * </p>
     */
    public Application() {
        super();
        AbstractBinder binder = new AbstractBinder() {
            
            @Override
            protected void configure() {
                String apiUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
                apiUrl = apiUrl.replace("/rest", "/api/v1");
                bind(new ApiUrls(apiUrl)).to(AbstractApiUrlManager.class);
            }
        };
        this.init(binder);
    }

    /**
     * Constructor with custom injection binder for tests
     * 
     * @param binder
     */
    public Application(AbstractBinder binder) {
        super();
        this.init(binder);
    }
    
    private void init(AbstractBinder injectionBinder) {
        register(MultiPartFeature.class);
        register(injectionBinder);
        packages(true, "io.goobi.viewer.api.rest.v1");
        packages(true, "io.goobi.viewer.api.rest.filters");
        packages(true, "io.goobi.viewer.api.rest.exceptions");
        packages(true, "de.unigoettingen.sub.commons.contentlib.servlet.rest");
        packages(true, "io.swagger");
    }

}
