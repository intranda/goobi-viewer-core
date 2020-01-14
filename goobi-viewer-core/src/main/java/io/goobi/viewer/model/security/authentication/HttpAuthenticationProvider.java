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
package io.goobi.viewer.model.security.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationRequest;
import io.goobi.viewer.model.security.authentication.model.VuAuthenticationResponse;

/**
 * <p>Abstract HttpAuthenticationProvider class.</p>
 *
 * @author Florian Alpers
 */
public abstract class HttpAuthenticationProvider implements IAuthenticationProvider {

    /** Constant <code>connectionManager</code> */
    protected static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    protected final String name;
    protected final String label;
    protected final String type;
    protected final String url;
    protected final String image;
    protected final long timeoutMillis;
    protected List<String> addUserToGroups;

    /**
     * <p>Constructor for HttpAuthenticationProvider.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     */
    public HttpAuthenticationProvider(String name, String label, String type, String url, String image, long timeoutMillis) {
        super();
        this.name = name;
        this.label = label;
        this.url = url;
        this.image = image;
        this.type = type;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * <p>Getter for the field <code>timeoutMillis</code>.</p>
     *
     * @return the timeoutMillis
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getName()
     */
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>Getter for the field <code>label</code>.</p>
     *
     * @return the label
     */
    public String getLabel() {
        return (this.label == null || this.label.isEmpty()) ? this.name : this.label;
    }

    
    /**
     * <p>Getter for the field <code>url</code>.</p>
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>Getter for the field <code>image</code>.</p>
     *
     * @return the image
     */
    public String getImage() {
        return image;
    }

    /**
     * <p>getImageUrl.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl() {
        try {
            URI uri = new URI(image);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
        } catch (NullPointerException | URISyntaxException e) {
            //construct viewer path uri
        }
        StringBuilder url = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        url.append("/resources/themes/").append(BeanUtils.getNavigationHelper().getTheme()).append("/images/openid/");
        url.append(image);
        return url.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getAddUserToGroups()
     */
    /** {@inheritDoc} */
    @Override
    public List<String> getAddUserToGroups() {
        return addUserToGroups;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#setAddUserToGroups(java.util.List)
     */
    /** {@inheritDoc} */
    @Override
    public void setAddUserToGroups(List<String> addUserToGroups) {
        this.addUserToGroups = addUserToGroups;
    }
    
    /**
     * <p>post.</p>
     *
     * @param url a {@link java.net.URI} object.
     * @param requestEntity a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws javax.ws.rs.WebApplicationException if any.
     */
    protected String post(URI url, String requestEntity) throws WebApplicationException {
        //client from connectionManager is reused, so don't close it
        CloseableHttpClient client =
                HttpClientBuilder.create().setConnectionManager(connectionManager).setRedirectStrategy(new LaxRedirectStrategy()).build();
        try {
            HttpPost post = new HttpPost(url);
            RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(1000).setSocketTimeout(1000).setConnectTimeout(1000).build();
            post.setConfig(config);
            post.addHeader("Content-Type", "application/json");
            HttpEntity e = new StringEntity(requestEntity);
            post.setEntity(e);
            try(CloseableHttpResponse httpResponse = client.execute(post)) {
               try(InputStream input = httpResponse.getEntity().getContent(); final Reader reader = new InputStreamReader(input)) {
                       String jsonResponse = CharStreams.toString(reader);
                       return jsonResponse;
                   }
               }
        } catch (IOException e) {
            throw new WebApplicationException("Error posting " + requestEntity + " to " + url, e);
        }
    }
    
    /**
     * <p>get.</p>
     *
     * @param url a {@link java.net.URI} object.
     * @return a {@link java.lang.String} object.
     * @throws javax.ws.rs.WebApplicationException if any.
     */
    protected String get(URI url) throws WebApplicationException {
        //client from connectionManager is reused, so don't close it
        CloseableHttpClient client =
                HttpClientBuilder.create().setConnectionManager(connectionManager).setRedirectStrategy(new LaxRedirectStrategy()).build();
        try {
            HttpGet get = new HttpGet(url);
            RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(1000).setSocketTimeout(1000).setConnectTimeout(1000).build();
            get.setConfig(config);
            try(CloseableHttpResponse httpResponse = client.execute(get)) {
               try(InputStream input = httpResponse.getEntity().getContent(); final Reader reader = new InputStreamReader(input)) {
                       String jsonResponse = CharStreams.toString(reader);
                       return jsonResponse;
                   }
               }
        } catch (IOException e) {
            throw new WebApplicationException("Error getting url " + url, e);
        }
    }
    

}
