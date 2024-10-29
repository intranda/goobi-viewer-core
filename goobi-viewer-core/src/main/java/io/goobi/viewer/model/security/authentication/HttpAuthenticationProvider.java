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
package io.goobi.viewer.model.security.authentication;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;

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
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.StringTools;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * <p>
 * Abstract HttpAuthenticationProvider class.
 * </p>
 *
 * @author Florian Alpers
 */
public abstract class HttpAuthenticationProvider implements IAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(HttpAuthenticationProvider.class);

    /** Constant <code>DEFAULT_EMAIL="{username}@nomail.com"</code> */
    protected static final String DEFAULT_EMAIL = "{username}@nomail.com";
    /** Constant <code>TYPE_USER_PASSWORD="userPassword"</code> */
    protected static final String TYPE_USER_PASSWORD = "userPassword";

    /** Constant <code>connectionManager</code> */
    protected static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    protected final String name;
    protected final String label;
    protected final String type;
    protected final String url;
    protected final String image;
    protected final long timeoutMillis;
    protected List<String> addUserToGroups;
    /** URL to redirect to after successful login. */
    protected String redirectUrl;

    /**
     * <p>
     * Constructor for HttpAuthenticationProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param type a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     */
    protected HttpAuthenticationProvider(String name, String label, String type, String url, String image, long timeoutMillis) {
        super();
        this.name = name;
        this.label = label;
        this.url = url;
        this.image = image;
        this.type = type;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * <p>
     * Getter for the field <code>timeoutMillis</code>.
     * </p>
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
     * <p>
     * Getter for the field <code>label</code>.
     * </p>
     *
     * @return the label
     */
    public String getLabel() {
        return (this.label == null || this.label.isEmpty()) ? this.name : this.label;
    }

    /**
     * <p>
     * Getter for the field <code>url</code>.
     * </p>
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Getter for the field <code>image</code>.
     * </p>
     *
     * @return the image
     */
    public String getImage() {
        return image;
    }

    /**
     * <p>
     * getImageUrl.
     * </p>
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
        StringBuilder sbUrl = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        sbUrl.append("/resources/themes/").append(BeanUtils.getNavigationHelper().getTheme()).append("/images/openid/");
        sbUrl.append(image);
        return sbUrl.toString();
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
     * @return the redirectUrl
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl) {
        logger.trace("setRedirectUrl: {}", redirectUrl);
        this.redirectUrl = redirectUrl;
    }

    /**
     * <p>
     * post.
     * </p>
     *
     * @param url a {@link java.net.URI} object.
     * @param requestEntity a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws jakarta.ws.rs.WebApplicationException if any.
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
            try (CloseableHttpResponse httpResponse = client.execute(post)) {
                try (StringWriter writer = new StringWriter()) {
                    //                    IOUtils.copy(httpResponse.getEntity().getContent(), writer, StringTools.DEFAULT_ENCODING);
                    //                    return writer.toString();
                    return EntityUtils.toString(httpResponse.getEntity(), StringTools.DEFAULT_ENCODING);
                }
            }
        } catch (IOException e) {
            throw new WebApplicationException("Error posting " + requestEntity + " to " + url, e);
        }
    }

    /**
     * <p>
     * get.
     * </p>
     *
     * @param url a {@link java.net.URI} object.
     * @return a {@link java.lang.String} object.
     * @throws jakarta.ws.rs.WebApplicationException if any.
     */
    protected String get(URI url) throws WebApplicationException {
        //client from connectionManager is reused, so don't close it
        CloseableHttpClient client =
                HttpClientBuilder.create().setConnectionManager(connectionManager).setRedirectStrategy(new LaxRedirectStrategy()).build();
        try {
            HttpGet get = new HttpGet(url);
            RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(1000).setSocketTimeout(1000).setConnectTimeout(1000).build();
            get.setConfig(config);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                return EntityUtils.toString(httpResponse.getEntity(), StringTools.DEFAULT_ENCODING);
            }
        } catch (IOException e) {
            throw new WebApplicationException("Error getting url " + url, e);
        }
    }

}
