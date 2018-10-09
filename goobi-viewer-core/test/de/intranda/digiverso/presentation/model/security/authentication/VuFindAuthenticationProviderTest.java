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
package de.intranda.digiverso.presentation.model.security.authentication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;

/**
 * @author Florian Alpers
 *
 */
public class VuFindAuthenticationProviderTest extends AbstractDatabaseEnabledTest {

    VuFindProvider provider;

    private static String userActive_nickname = "nick 1";
    private static String userActive_email = "1@users.org";
    private static String userActive_pwHash = "abcdef1";

    private static String userSuspended_nickname = "nick 3";
    private static String userSuspended_email = "3@users.org";
    private static String userSuspended_pwHash = "abcdef3";

    private static ClientAndProxy proxy;
    private static ClientAndServer mockServer;
    private static MockServerClient serverClient;

    private static final int SERVERPORT = 1080;
    private static final String SERVERURL = "127.0.0.1";
    private static final String REQUEST_BODY_TEMPLATE = "{\"username\":\"{username}\",\"password\":\"{password}\"}";
    private static final String RESPONSE_USER_VALID = "{ " + "\"user\": {" + "\"isValid\": \"Y\"," + "\"exists\": \"Y\"" + "}," + "\"expired\": {"
            + "\"isExpired\": \"N\"" + "}," + "\"blocks\": {" + "\"isBlocked\": \"N\"" + "}" + "}";
    private static final String RESPONSE_USER_INVALID = "{ " + "\"user\": {" + "\"isValid\": \"N\"," + "\"exists\": \"Y\"" + "}," + "\"expired\": {"
            + "\"isExpired\": \"N\"" + "}," + "\"blocks\": {" + "\"isBlocked\": \"Y\"" + "}" + "}";
    private static final String RESPONSE_USER_UNKNOWN = "{ " + "\"user\": {" + "\"isValid\": \"N\"," + "\"exists\": \"N\"" + "}," + "\"expired\": {"
            + "\"isExpired\": \"U\"" + "}," + "\"blocks\": {" + "\"isBlocked\": \"U\"" + "}" + "}";
    private static final String RESPONSE_USER_SUSPENDED = "{ " + "\"user\": {" + "\"isValid\": \"N\"," + "\"exists\": \"Y\"" + "}," + "\"expired\": {"
            + "\"isExpired\": \"Y\"" + "}," + "\"blocks\": {" + "\"isBlocked\": \"N\"" + "}" + "}";

    @BeforeClass
    public static void startProxy() {
        mockServer = ClientAndServer.startClientAndServer(SERVERPORT);
        String requestBody_valid = REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname).replace("{password}", userActive_pwHash);
        String requestBody_invalid = REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname).replace("{password}", userSuspended_pwHash);
        String requestBody_unknown = REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname + "test").replace("{password}", userActive_pwHash);
        String requestBody_suspended = REQUEST_BODY_TEMPLATE.replace("{username}", userSuspended_nickname).replace("{password}", userSuspended_pwHash);

        serverClient = new MockServerClient(SERVERURL, SERVERPORT);
        
        //active user
        serverClient
                .when(HttpRequest.request()
                        .withPath("/user/auth")
                        .withBody(requestBody_valid)
                     )
                .respond(HttpResponse.response()
                        .withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON))
                        .withBody(RESPONSE_USER_VALID));
        
        //wrong password
        serverClient
        .when(HttpRequest.request()
                .withPath("/user/auth")
                .withBody(requestBody_invalid)
             )
        .respond(HttpResponse.response()
                .withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody(RESPONSE_USER_INVALID));
        
      //unknown user
        serverClient
        .when(HttpRequest.request()
                .withPath("/user/auth")
                .withBody(requestBody_unknown)
             )
        .respond(HttpResponse.response()
                .withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody(RESPONSE_USER_UNKNOWN));
        
      //suspended user
        serverClient
        .when(HttpRequest.request()
                .withPath("/user/auth")
                .withBody(requestBody_suspended)
             )
        .respond(HttpResponse.response()
                .withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody(RESPONSE_USER_SUSPENDED));

    }

    @AfterClass
    public static void stopProxy() throws Exception {
        serverClient.stop();
        mockServer.stop();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        provider = new VuFindProvider("external", "http://" + SERVERURL + ":" + SERVERPORT + "/user/auth", "", 1000l);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLogin_valid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname, userActive_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().getUser().get().isActive());
        Assert.assertFalse(future.get().getUser().get().isSuspended());
    }

    @Test
    public void testLogin_invalid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname, userSuspended_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().isRefused());
    }

    @Test
    public void testLogin_unknown() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname + "test", userActive_pwHash);
        Assert.assertFalse(future.get().getUser().isPresent());
    }

    @Test
    public void testLogin_suspended() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userSuspended_nickname, userSuspended_pwHash);
        Assert.assertTrue(future.get().getUser().isPresent());
        Assert.assertTrue(future.get().getUser().get().isActive());
        Assert.assertTrue(future.get().getUser().get().isSuspended());
    }

}
