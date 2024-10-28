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
package io.goobi.viewer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import org.mockito.Mockito;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.ContextMocker;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LoginResult;
import io.goobi.viewer.model.security.user.User;

public class TestUtils {
    
    public static final String APPLICATION_ROOT_URL = "https://viewer.goobi.io/";
    public static final int NUM_ALL_SEARCH_SORTING_OPTIONS = 12;

    /**
     * Creates a Mockito-created FacesContext with an ExternalContext, ServletContext and session map. It can then be extended by tests to return
     * beans, etc.
     *
     * @return Mock FacesContext
     */
    public static FacesContext mockFacesContext() {
        FacesContext facesContext = ContextMocker.mockFacesContext();

        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(externalContext.getContext()).thenReturn(servletContext);

        Map<String, Object> sessionMap = new HashMap<>();
        Mockito.when(externalContext.getSessionMap()).thenReturn(sessionMap);

        return facesContext;
    }

    public static IAuthenticationProvider testAuthenticationProvider = new IAuthenticationProvider() {

        private User user = null;

        @Override
        public void logout() throws AuthenticationProviderException {
            //F
        }

        @Override
        public CompletableFuture<LoginResult> login(String loginName, String password) throws AuthenticationProviderException {
            LoginResult result;
            try {
                user = DataManager.getInstance().getDao().getUserByEmail(loginName);
                if (user != null && user.getPasswordHash().equals(password)) {
                    result = new LoginResult(null, null, Optional.ofNullable(user), false);
                } else {
                    result = new LoginResult(null, null, Optional.empty(), true);
                }
            } catch (DAOException e) {
                throw new AuthenticationProviderException(e);
            }
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public String getType() {
            return "test";
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean allowsPasswordChange() {
            return false;
        }

        @Override
        public boolean allowsNicknameChange() {
            return true;
        }

        @Override
        public boolean allowsEmailChange() {
            return false;
        }

        @Override
        public List<String> getAddUserToGroups() {
            return Collections.emptyList();
        }

        @Override
        public void setAddUserToGroups(List<String> addUserToGroups) {
            //
        }

        @Override
        public String getRedirectUrl() {
            return null;
        }

        @Override
        public void setRedirectUrl(String redirectUrl) {
            //            
        }
    };

}
