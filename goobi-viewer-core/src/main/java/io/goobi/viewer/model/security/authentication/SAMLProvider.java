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
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.openid.SAMLAssertionServlet;

/**
 * @author Oliver Paetzel
 *
 */
public class SAMLProvider implements IAuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(SAMLProvider.class);

    private final String name;
    private final String idpMetadataUrl;
    private final String relyingPartyIdentifier;
    protected final long timeoutMillis;

    private volatile SamlClient client;
    private volatile LoginResult loginResult = null;
    private Object responseLock = new Object();

    public SAMLProvider(String name, String idpMetadataUrl, String relyingPartyIdentifier, long timeoutMillis) {
        super();
        this.name = name;
        this.idpMetadataUrl = idpMetadataUrl;
        this.relyingPartyIdentifier = relyingPartyIdentifier;
        this.timeoutMillis = timeoutMillis;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#login(java.lang.String, java.lang.String)
     */
    @Override
    public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
        try (InputStream in = new URL(idpMetadataUrl).openConnection().getInputStream(); Reader idpMetaReader = new InputStreamReader(in)) {
            String redirectUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + SAMLAssertionServlet.URL;
            SamlClient client = SamlClient.fromMetadata(this.relyingPartyIdentifier, redirectUrl, idpMetaReader);
            this.client = client;
            // this needs to be done, so the SAMLServlet is able to find this instance when the IdP redirects back to the viewer
            BeanUtils.getRequest().getSession().setAttribute("SAMLProvider", this);
            client.redirectToIdentityProvider(BeanUtils.getResponse(), null);
            return CompletableFuture.supplyAsync(() -> {
                synchronized (responseLock) {
                    try {
                        // this wait will be stopped by a notify() when the login was successful. This happens in the completeLogin() method.
                        responseLock.wait(this.timeoutMillis);
                        return this.loginResult;
                    } catch (InterruptedException e) {
                        return new LoginResult(BeanUtils.getRequest(), BeanUtils.getResponse(), new AuthenticationProviderException(e));
                    }
                }
            });
        } catch (IOException | SamlException e) {
            throw new AuthenticationProviderException(e);
        }
    }

    /**
     * Completes the authentication flow and wakes up the CompletableFuture returned by the login() method.
     * 
     * @param encodedResponse the base64-encoded SAML-response
     * @param request The callback request
     * @param response The callback response
     * @return a future that will be completed when the userBean has redirected the user.
     */
    public Future<Boolean> completeLogin(String encodedResponse, HttpServletRequest request, HttpServletResponse response) {
        try {
            SamlResponse samlResponse = client.decodeAndValidateSamlResponse(encodedResponse);
            String id = samlResponse.getNameID();

            User user = null;
            if (id != null) {
                String comboSub = getType().toLowerCase() + ":" + getName().toLowerCase() + ":" + id;
                // Retrieve user by sub
                user = DataManager.getInstance().getDao().getUserByOpenId(comboSub);
                if (user != null) {
                    logger.debug("Found user {} via OAuth sub '{}'.", user.getId(), comboSub);
                }
                // If not found, create a new user
                if (user == null) {
                    user = new User();
                    user.setActive(true);
                    //user.setEmail(email);
                    if (id != null) {
                        user.getOpenIdAccounts().add(id);
                    }
                    logger.debug("Created new user.");
                }
                // Add to bean and persist
                if (user.getId() == null) {
                    if (!DataManager.getInstance().getDao().addUser(user)) {
                        logger.error("Could not add user to DB.");
                    }
                } else {
                    if (!DataManager.getInstance().getDao().updateUser(user)) {
                        logger.error("Could not update user in DB.");
                    }
                }
            }
            this.loginResult = new LoginResult(request, response, Optional.ofNullable(user), false);
        } catch (SamlException | DAOException e) {
            this.loginResult = new LoginResult(request, response, new AuthenticationProviderException(e));
        } finally {
            synchronized (responseLock) {
                responseLock.notifyAll();
            }
        }
        return this.loginResult.isRedirected(timeoutMillis);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#logout()
     */
    @Override
    public void logout() throws AuthenticationProviderException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsPasswordChange()
     */
    @Override
    public boolean allowsPasswordChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getType()
     */
    @Override
    public String getType() {
        return "SAML";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsNicknameChange()
     */
    @Override
    public boolean allowsNicknameChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#allowsEmailChange()
     */
    @Override
    public boolean allowsEmailChange() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#getAddUserToGroups()
     */
    @Override
    public List<String> getAddUserToGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.authentication.IAuthenticationProvider#setAddUserToGroups(java.util.List)
     */
    @Override
    public void setAddUserToGroups(List<String> addUserToGroups) {
        // TODO Auto-generated method stub

    }

}
