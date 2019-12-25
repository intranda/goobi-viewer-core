package io.goobi.viewer.servlets.rest;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

/**
 * <p>CORSHeaderFilter class.</p>
 *
 */
@Provider
@CORSBinding
public class CORSHeaderFilter implements ContainerResponseFilter {

    private static final Configuration config = DataManager.getInstance().getConfiguration();

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (config.isAddCORSHeader()) {
            addAccessControlHeader(responseContext, config.getCORSHeaderValue());
        }
    }

    private static void addAccessControlHeader(ContainerResponseContext response, String content) {
        response.getHeaders().add("Access-Control-Allow-Origin", content);
    }

}
