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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.iiif.image.ImageInformation;
import io.goobi.viewer.AbstractDatabaseEnabledTest;

/**
 * @author Florian Alpers
 *
 */
class VuFindAuthenticationProviderTest extends AbstractDatabaseEnabledTest {

    VuFindProvider provider;

    private static String userActive_nickname = "admin";
    private static String userActive_email = "1@users.org";
    private static String userActive_pwHash = "abcdef1";

    private static String userSuspended_nickname = "nick 3";
    private static String userSuspended_email = "3@users.org";
    private static String userSuspended_pwHash = "abcdef3";

    //    private static ClientAndProxy proxy;
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

    @BeforeAll
    public static void startProxy() {
        mockServer = ClientAndServer.startClientAndServer(SERVERPORT);
        String requestBody_valid = REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname).replace("{password}", userActive_pwHash);
        String requestBody_invalid = REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname).replace("{password}", userSuspended_pwHash);
        String requestBody_unknown =
                REQUEST_BODY_TEMPLATE.replace("{username}", userActive_nickname + "test").replace("{password}", userActive_pwHash);
        String requestBody_suspended =
                REQUEST_BODY_TEMPLATE.replace("{username}", userSuspended_nickname).replace("{password}", userSuspended_pwHash);

        serverClient = new MockServerClient(SERVERURL, SERVERPORT);

        //active user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBody_valid))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_VALID));

        //wrong password
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBody_invalid))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_INVALID));

        //unknown user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBody_unknown))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_UNKNOWN));

        //suspended user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBody_suspended))
                .respond(
                        HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_SUSPENDED));

    }

    @AfterAll
    public static void stopProxy() throws Exception {
        serverClient.stop();
        mockServer.stop();
        Path logFile = Paths.get("mockserver.log");
        if (Files.isRegularFile(logFile)) {
            Files.delete(logFile);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        provider = new VuFindProvider("external", "", "http://" + SERVERURL + ":" + SERVERPORT + "/user/auth", "", 1000l);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testLogin_valid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname, userActive_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isActive());
        Assertions.assertFalse(future.get().getUser().get().isSuspended());
    }

    @Test
    void testLogin_invalid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname, userSuspended_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().isRefused());
    }

    @Test
    void testLogin_unknown() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActive_nickname + "test", userActive_pwHash);
        Assertions.assertFalse(future.get().getUser().isPresent());
    }

    @Test
    void testLogin_suspended() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userSuspended_nickname, userSuspended_pwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isActive());
        Assertions.assertTrue(future.get().getUser().get().isSuspended());
    }

}
