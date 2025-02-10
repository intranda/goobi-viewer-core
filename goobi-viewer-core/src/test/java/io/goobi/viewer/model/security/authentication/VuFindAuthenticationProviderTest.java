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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.FileTools;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Florian Alpers
 *
 */
class VuFindAuthenticationProviderTest extends AbstractDatabaseEnabledTest {

    VuFindProvider provider;

    private static String userActiveNickname = "admin";
    private static String userActivePwHash = "abcdef1";

    private static String userSuspendedNickname = "nick 3";
    private static String userSuspendedPwHash = "abcdef3";

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
        String requestBodyValid = REQUEST_BODY_TEMPLATE.replace("{username}", userActiveNickname + "foo").replace("{password}", userActivePwHash);
        String requestBodyInvalid = REQUEST_BODY_TEMPLATE.replace("{username}", userActiveNickname).replace("{password}", userSuspendedPwHash);
        String requestBodyUnknown =
                REQUEST_BODY_TEMPLATE.replace("{username}", userActiveNickname + "test").replace("{password}", userActivePwHash);
        String requestBodySuspended =
                REQUEST_BODY_TEMPLATE.replace("{username}", userSuspendedNickname).replace("{password}", userSuspendedPwHash);

        serverClient = new MockServerClient(SERVERURL, SERVERPORT);

        //active user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBodyValid))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_VALID));

        //wrong password
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBodyInvalid))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_INVALID));

        //unknown user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBodyUnknown))
                .respond(HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_UNKNOWN));

        //suspended user
        serverClient.when(HttpRequest.request().withPath("/user/auth").withBody(requestBodySuspended))
                .respond(
                        HttpResponse.response().withHeader(new Header("Content-Type", MediaType.APPLICATION_JSON)).withBody(RESPONSE_USER_SUSPENDED));

    }

    @AfterAll
    public static void stopProxy() throws Exception {
        serverClient.stop();
        mockServer.stop();
        Path logFile = Paths.get("mockserver.log");
        if (Files.isRegularFile(logFile)) {
            System.out.println(FileTools.getStringFromFile(logFile.toFile(), StandardCharsets.UTF_8.toString()));
            Files.delete(logFile);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        provider = new VuFindProvider("external", "", "http://" + SERVERURL + ":" + SERVERPORT + "/user/auth", "", 1000l);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    void testLogin_unknown() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActiveNickname + "test", userActivePwHash);
        Assertions.assertFalse(future.get().getUser().isPresent());
        System.out.println(REQUEST_BODY_TEMPLATE.replace("{username}", userActiveNickname + "test").replace("{password}", userActivePwHash));
        System.out.println(RESPONSE_USER_UNKNOWN);
    }

    @Test
    void testLogin_valid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActiveNickname, userActivePwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isActive());
        Assertions.assertFalse(future.get().getUser().get().isSuspended());
    }

    @Test
    void testLogin_invalid() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userActiveNickname, userSuspendedPwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().isRefused());
    }

    @Test
    void testLogin_suspended() throws AuthenticationProviderException, InterruptedException, ExecutionException {
        CompletableFuture<LoginResult> future = provider.login(userSuspendedNickname, userSuspendedPwHash);
        Assertions.assertTrue(future.get().getUser().isPresent());
        Assertions.assertTrue(future.get().getUser().get().isActive());
        Assertions.assertTrue(future.get().getUser().get().isSuspended());
    }

}
