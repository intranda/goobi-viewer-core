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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.authentication.model.LitteraAuthenticationResponse;
import io.goobi.viewer.model.security.user.User;

/**
 * External authentication provider for the LITTERA reader authentication api (www.littera.eu). This provider requests requests authentication from
 * the configured url and an 'id' and 'pw' provided as query parameters. The response is a text/xml document containing a root element <Response> with
 * an attribute "authenticationSuccessful" which is either true or false depending on the validity of the passed query params. If the authentication
 * is successful, an existing viewer user is newly created is required with the nickname of the login id and an email of {id}@nomail.com. The user may
 * still be suspended, given admin rights ect. as any other viewer user
 *
 * @author Florian Alpers
 */
public class LitteraProvider extends HttpAuthenticationProvider {

    private static final Logger logger = LogManager.getLogger(LitteraProvider.class);
    /** Constant <code>DEFAULT_EMAIL="{username}@nomail.com"</code> */
    protected static final String DEFAULT_EMAIL = "{username}@nomail.com";
    /** Constant <code>TYPE_USER_PASSWORD="userPassword"</code> */
    protected static final String TYPE_USER_PASSWORD = "userPassword";
    private static final String QUERY_PARAMETER_ID = "id";
    private static final String QUERY_PARAMETER_PW = "pw";

    /**
     * <p>
     * Constructor for LitteraProvider.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param image a {@link java.lang.String} object.
     * @param timeoutMillis a long.
     */
    public LitteraProvider(String name, String label, String url, String image, long timeoutMillis) {
        super(name, label, TYPE_USER_PASSWORD, url, image, timeoutMillis);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    /** {@inheritDoc} */
    @Override
    public void logout() throws AuthenticationProviderException {
        //noop
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try {
            LitteraAuthenticationResponse response = get(new URI(getUrl()), loginName, password);
            Optional<User> user = getUser(loginName, response);
            LoginResult result = new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), user, !response.isAuthenticationSuccessful());
            return CompletableFuture.completedFuture(result);
        } catch (URISyntaxException e) {
            throw new AuthenticationProviderException("Cannot resolve authentication api url " + getUrl(), e);
        } catch (IOException e) {
            throw new AuthenticationProviderException("Error requesting authorisation for user " + loginName, e);
        }
    }

    /**
     * <p>
     * get.
     * </p>
     *
     * @param url a {@link java.net.URI} object.
     * @param username a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.authentication.model.LitteraAuthenticationResponse} object.
     * @throws java.io.IOException if any.
     */
    protected LitteraAuthenticationResponse get(final URI url, String username, String password) throws IOException {
        URI uri = UriBuilder.fromUri(url).queryParam(QUERY_PARAMETER_ID, username).queryParam(QUERY_PARAMETER_PW, password).build();
        String xml = get(uri);
        return deserialize(xml);
    }

    /**
     * @param xml
     * @return {@link LitteraAuthenticationResponse}
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    private static LitteraAuthenticationResponse deserialize(String xml) throws IOException {
        return new XmlMapper().readValue(xml, LitteraAuthenticationResponse.class);
    }

    /**
     * @param loginName
     * @param response
     * @return Optional<User>
     * @throws AuthenticationProviderException
     */
    private static Optional<User> getUser(String loginName, LitteraAuthenticationResponse response) throws AuthenticationProviderException {

        if (StringUtils.isBlank(loginName) || !response.isAuthenticationSuccessful()) {
            return Optional.empty();
        }

        User user = null;
        try {
            user = DataManager.getInstance().getDao().getUserByNickname(loginName);
            if (user != null) {
                logger.debug("Found user {} via vuFind username '{}'.", user.getId(), loginName);
            }
            // If not found, try email
            if (user == null) {
                user = DataManager.getInstance().getDao().getUserByEmail(loginName);
                if (user != null) {
                    logger.debug("Found user {} via vuFind username '{}'.", user.getId(), loginName);
                }
            }

            // If still not found, create a new user
            if (user == null) {
                user = new User();
                user.setNickName(loginName);
                user.setActive(true);
                user.setEmail(DEFAULT_EMAIL.replace("{username}", loginName));
                logger.debug("Created new user with nickname {}", loginName);
            }

            // Add to bean and persist
            if (user.getId() == null) {
                if (!DataManager.getInstance().getDao().addUser(user)) {
                    throw new AuthenticationProviderException("Could not add user to DB.");
                }
            } else {
                if (!DataManager.getInstance().getDao().updateUser(user)) {
                    throw new AuthenticationProviderException("Could not update user in DB.");
                }
            }

        } catch (DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        return Optional.ofNullable(user);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    /** {@inheritDoc} */
    @Override
    public boolean allowsEmailChange() {
        return true;
    }

}
