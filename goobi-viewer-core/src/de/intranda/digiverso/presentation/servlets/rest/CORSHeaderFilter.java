package de.intranda.digiverso.presentation.servlets.rest;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;

@Provider
@CORSBinding
public class CORSHeaderFilter implements ContainerResponseFilter {

    private static final Configuration config = DataManager.getInstance().getConfiguration();
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if(config.isAddCORSHeader()) {            
            addAccessControlHeader(responseContext, config.getCORSHeaderValue());
        }
    }

    private void addAccessControlHeader(ContainerResponseContext response, String content) {
        response.getHeaders().add("Access-Control-Allow-Origin", content);
    }

}
