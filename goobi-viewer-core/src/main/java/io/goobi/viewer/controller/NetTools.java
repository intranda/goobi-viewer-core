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
package io.goobi.viewer.controller;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Utility methods for HTTP operations, mail, etc.
 */
public final class NetTools {

    private static final Logger logger = LogManager.getLogger(NetTools.class);

    private static final int HTTP_TIMEOUT = 30000;
    /** Constant <code>ADDRESS_LOCALHOST_IPV4="127.0.0.1"</code>. */
    public static final String ADDRESS_LOCALHOST_IPV4 = "127.0.0.1";
    /** Constant <code>ADDRESS_LOCALHOST_IPV6="0:0:0:0:0:0:0:1"</code>. */
    public static final String ADDRESS_LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment; filename=\"";

    private static final String HTTP_METHOD_DELETE = "DELETE";

    public static final String PARAM_CLEAR_CACHE_ALL = "all";
    public static final String PARAM_CLEAR_CACHE_CONTENT = "content";
    public static final String PARAM_CLEAR_CACHE_THUMBS = "thumbs";
    public static final String PARAM_CLEAR_CACHE_PDF = "pdf";

    /**
     * Hiding public constructor.
     */
    private NetTools() {
    }

    /**
     * Validates the given URL for outbound HTTP calls. Blocks SSRF attempts
     * against private/loopback/link-local ranges and cloud metadata services.
     * Hosts that appear in the application configuration (Solr, Workflow,
     * REST API, authentication providers) are implicitly allowed.
     *
     * @param url URL to validate
     * @throws IllegalArgumentException if the URL is null, malformed,
     *         uses a non-HTTP scheme, or resolves to a blocked address
     * @should reject null url
     * @should reject blank url
     * @should reject non-http scheme
     * @should reject url without host
     * @should reject loopback address when not allowed
     * @should allow loopback address when configured
     * @should reject private network address
     * @should reject link-local address
     * @should allow implicitly whitelisted host
     * @should allow public address
     */
    public static void validateOutboundUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url may not be blank");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Malformed URL: " + e.getMessage());
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "Only http/https schemes allowed, got: " + scheme);
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException(
                    "URL has no host: " + url);
        }

        if (buildImplicitAllowlist().contains(host.toLowerCase())) {
            return;
        }

        Configuration config =
                DataManager.getInstance().getConfiguration();
        boolean allowLoopback = config.isOutboundHttpAllowLoopback();

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Unresolvable host: " + host);
        }
        for (InetAddress addr : addresses) {
            if (addr.isLoopbackAddress() && allowLoopback) {
                continue;
            }
            if (isBlockedAddress(addr)) {
                throw new IllegalArgumentException(
                        "Host resolves to blocked address: "
                                + host + " -> "
                                + addr.getHostAddress());
            }
        }
    }

    /**
     * @param addr address to check
     * @return true if the address belongs to a blocked range
     */
    static boolean isBlockedAddress(InetAddress addr) {
        return addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress();
    }

    /**
     * Builds a set of hostnames that are implicitly allowed for outbound
     * HTTP calls because they appear in the application configuration.
     *
     * @return lower-cased hostnames from Solr, Workflow, REST API and
     *         authentication provider URLs
     */
    static Set<String> buildImplicitAllowlist() {
        Set<String> hosts = new HashSet<>();
        Configuration config =
                DataManager.getInstance().getConfiguration();
        addHostFromUrl(hosts, config.getSolrUrl());
        addHostFromUrl(hosts, config.getWorkflowRestUrl());
        addHostFromUrl(hosts, config.getRestApiUrl());
        for (IAuthenticationProvider provider
                : config.getAuthenticationProviders()) {
            if (provider instanceof HttpAuthenticationProvider) {
                addHostFromUrl(hosts,
                        ((HttpAuthenticationProvider) provider)
                                .getUrl());
            }
        }
        return hosts;
    }

    private static void addHostFromUrl(Set<String> hosts, String url) {
        if (StringUtils.isBlank(url)) {
            return;
        }
        try {
            String prefixed = url;
            if (!prefixed.contains("://")) {
                prefixed = "http://" + prefixed;
            }
            String host = new URI(prefixed).getHost();
            if (host != null) {
                hosts.add(host.toLowerCase());
            }
        } catch (URISyntaxException e) {
            logger.warn("Cannot extract host from configured URL: {}",
                    url);
        }
    }

    /**
     * Creates an HttpClient that pins DNS resolution to the addresses
     * obtained at validation time, preventing DNS rebinding attacks.
     *
     * @param validatedHost the already-validated hostname
     * @param pinnedAddresses the addresses resolved during validation
     * @param config optional RequestConfig (may be null)
     * @return a CloseableHttpClient with pinned DNS
     */
    static CloseableHttpClient createSsrfSafeHttpClient(
            String validatedHost, InetAddress[] pinnedAddresses,
            RequestConfig config) {
        DnsResolver pinnedResolver = host -> {
            if (host.equalsIgnoreCase(validatedHost)) {
                return pinnedAddresses;
            }
            return InetAddress.getAllByName(host);
        };
        org.apache.http.impl.client.HttpClientBuilder builder =
                HttpClients.custom().setDnsResolver(pinnedResolver);
        if (config != null) {
            builder.setDefaultRequestConfig(config);
        }
        return builder.build();
    }

    /**
     * Resolves and validates the host of the given URL, then returns
     * an SSRF-safe HttpClient with pinned DNS. For hosts on the
     * implicit allowlist, returns a regular HttpClient (no DNS pinning
     * needed since these are trusted).
     *
     * @param url the outbound URL (must have been validated already)
     * @param config optional RequestConfig (may be null)
     * @return a CloseableHttpClient safe against SSRF/DNS-rebinding
     */
    public static CloseableHttpClient buildHttpClientForUrl(
            String url, RequestConfig config) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Malformed URL: " + e.getMessage());
        }
        String host = uri.getHost();
        if (host != null
                && buildImplicitAllowlist().contains(
                        host.toLowerCase())) {
            org.apache.http.impl.client.HttpClientBuilder builder =
                    HttpClients.custom();
            if (config != null) {
                builder.setDefaultRequestConfig(config);
            }
            return builder.build();
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Unresolvable host: " + host);
        }
        return createSsrfSafeHttpClient(host, addresses, config);
    }

    /**
     * Checks whether the given redirect URL is allowed. A URL is allowed if it starts with the given application base URL
     * or if its host is in the configured redirect whitelist.
     *
     * @param redirectUrl URL to check
     * @param applicationUrl Application base URL (may be null)
     * @return true if allowed; false otherwise
     * @should return true if redirectUrl starts with application url
     * @should return true if redirectUrl host is whitelisted
     * @should return false if redirectUrl host is not whitelisted
     * @should return false if redirectUrl is malformed
     * @should return false if redirectUrl is null
     */
    public static boolean isRedirectUrlAllowed(String redirectUrl, String applicationUrl) {
        if (redirectUrl == null) {
            return false;
        }

        // Allow URLs that start with the application's own URL
        if (applicationUrl != null && redirectUrl.startsWith(applicationUrl)) {
            return true;
        }

        // Check against configured whitelist of allowed redirect hosts
        List<String> whitelist = DataManager.getInstance().getConfiguration().getHttpHeaderLoginRedirectWhitelist();
        if (!whitelist.isEmpty()) {
            try {
                String host = new URI(redirectUrl).toURL().getHost();
                return whitelist.contains(host);
            } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
                logger.warn("Could not parse redirect URL: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * callUrlGET.
     *
     * @param url URL to call via HTTP GET
     * @return A String array with two elements. The first contains the HTTP status code, the second either the requested data (if status code is 200)
     *         or the error message.
     */
    public static String[] callUrlGET(String url) {
        validateOutboundUrl(url);
        // logger.trace("callUrlGET: {}", url); //NOSONAR Debug
        String[] ret = new String[2];
        try (CloseableHttpClient httpClient =
                buildHttpClientForUrl(url, null)) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet); StringWriter writer = new StringWriter()) {
                ret[0] = String.valueOf(response.getStatusLine().getStatusCode());
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpServletResponse.SC_OK:
                        HttpEntity httpEntity = response.getEntity();
                        httpEntity.getContentLength();
                        IOUtils.copy(httpEntity.getContent(), writer, StringTools.DEFAULT_ENCODING);
                        ret[1] = writer.toString();
                        break;
                    case 401:
                        logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                    default:
                        // logger.warn("Error code: {}", response.getStatusLine().getStatusCode()); //NOSONAR Debug
                        ret[1] = response.getStatusLine().getReasonPhrase();
                        break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     *
     * @param url URL to call
     * @return {@link String} content fetched from the given url
     * @throws IOException
     * @throws HTTPException
     */
    public static String getWebContentGET(String url) throws IOException, HTTPException {
        return getWebContentGET(url, HTTP_TIMEOUT);
    }

    /**
     * getWebContentGET.
     *
     * @param url URL to call
     * @param timeout Custom timeout
     * @return the HTTP response body as a string
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static String getWebContentGET(String url, int timeout) throws IOException, HTTPException {
        validateOutboundUrl(url);
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();
        try (CloseableHttpClient httpClient =
                buildHttpClientForUrl(url, defaultRequestConfig)) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(get); StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), StringTools.DEFAULT_ENCODING);
                    // IOUtils.copy(response.getEntity().getContent(), writer);
                    // return writer.toString();
                }
                logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * getWebContentPOST.
     *
     * @param url URL to call via HTTP POST
     * @param headers HTTP request headers as name-value pairs
     * @param params form parameters sent in the request body
     * @param cookies cookies to include with the request
     * @param contentType MIME type for the request body
     * @param stringBody Optional entity content.
     * @param file optional file to upload as multipart body
     * @return the HTTP response body as a string
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     */
    public static String getWebContentPOST(String url, Map<String, String> headers, Map<String, String> params, Map<String, String> cookies,
            String contentType, String stringBody, File file) throws IOException {
        return getWebContent("POST", url, headers, params, cookies, contentType, stringBody, file);
    }

    /**
     * getWebContentDELETE.
     *
     * @param url URL to call via HTTP DELETE
     * @param headers HTTP request headers as name-value pairs
     * @param params form parameters sent in the request body
     * @param cookies cookies to include with the request
     * @param stringBody Optional entity content.
     * @return the HTTP response body as a string
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if return code is not 200
     */
    public static String getWebContentDELETE(String url, Map<String, String> headers, Map<String, String> params, Map<String, String> cookies,
            String stringBody) throws IOException {
        return getWebContent(HTTP_METHOD_DELETE, url, headers, params, cookies, null, stringBody, null);
    }

    /**
     * getWebContent.
     *
     * @param method POST | PUT | DELETE
     * @param url URL to call
     * @param headers HTTP request headers as name-value pairs
     * @param params form parameters sent in the request body
     * @param cookies cookies to include with the request
     * @param contentType Optional mime type.
     * @param stringBody Optional entity content.
     * @param file Optional file entity content.
     * @return the HTTP response body as a string
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if return code is not 200
     */
    static String getWebContent(String method, String url, Map<String, String> headers, Map<String, String> params, Map<String, String> cookies,
            String contentType, String stringBody, File file) throws IOException {
        if (method == null || !("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || HTTP_METHOD_DELETE.equalsIgnoreCase(method))) {
            throw new IllegalArgumentException("Illegal method: " + method);
        }
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }
        validateOutboundUrl(url);

        logger.debug("url: {}", url);
        List<NameValuePair> nameValuePairs = null;
        if (params == null) {
            nameValuePairs = new ArrayList<>(0);
        } else {
            nameValuePairs = new ArrayList<>(params.size());
            for (Entry<String, String> entry : params.entrySet()) {
                nameValuePairs.add(
                        new BasicNameValuePair(
                                entry.getKey(), entry.getValue()));
            }
        }
        HttpClientContext context = null;
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null && !cookies.isEmpty()) {
            context = HttpClientContext.create();
            for (Entry<String, String> entry : cookies.entrySet()) {
                BasicClientCookie cookie =
                        new BasicClientCookie(
                                entry.getKey(), entry.getValue());
                cookie.setPath("/");
                cookie.setDomain("0.0.0.0");
                cookieStore.addCookie(cookie);
            }
            context.setCookieStore(cookieStore);
        }

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient =
                buildHttpClientForUrl(
                        url, defaultRequestConfig)) {

            HttpRequestBase requestBase;
            switch (method.toUpperCase()) {
                case "POST":
                    requestBase = new HttpPost(url);
                    break;
                case "PUT":
                    requestBase = new HttpPut(url);
                    break;
                case HTTP_METHOD_DELETE:
                    requestBase = new HttpDelete(url);
                    break;
                default:
                    return "";
            }
            if (headers != null && !headers.isEmpty()) {
                for (String key : headers.keySet()) {
                    requestBase.addHeader(key, headers.get(key));
                }
            }
            Charset.forName(StringTools.DEFAULT_ENCODING);
            // TODO allow combinations of params + body
            if (requestBase instanceof HttpPost || requestBase instanceof HttpPut) {
                if (file != null) {
                    // Multipart
                    ((HttpEntityEnclosingRequestBase) requestBase).setEntity(
                            MultipartEntityBuilder.create()
                                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                    .addBinaryBody("file", file)
                                    .addTextBody("filename", file.getName())
                                    .build());
                } else if (StringUtils.isNotEmpty(stringBody)) {
                    ByteArrayEntity entity = new ByteArrayEntity(stringBody.getBytes(StringTools.DEFAULT_ENCODING));
                    entity.setContentEncoding(StringTools.DEFAULT_ENCODING);
                    if (contentType != null) {
                        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, contentType));
                    }
                    ((HttpEntityEnclosingRequestBase) requestBase).setEntity(entity);
                } else {
                    ((HttpEntityEnclosingRequestBase) requestBase).setEntity(new UrlEncodedFormEntity(nameValuePairs));
                }
            }
            try (CloseableHttpResponse response = (context == null ? httpClient.execute(requestBase) : httpClient.execute(requestBase, context));
                    StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
                if (code == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), StringTools.DEFAULT_ENCODING);
                }
                return EntityUtils.toString(response.getEntity(), StringTools.DEFAULT_ENCODING);
            }
        }
    }

    /**
     * Sends an email to with the given subject and body to the given recipient list.
     *
     * @param recipients list of primary recipient email addresses
     * @param cc list of CC recipient email addresses
     * @param bcc list of BCC recipient email addresses
     * @param subject email subject line
     * @param body email body text (HTML)
     * @return true if mail sent successfully; false otherwise
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws jakarta.mail.MessagingException if any.
     */
    public static boolean postMail(List<String> recipients, List<String> cc, List<String> bcc, String subject, String body)
            throws UnsupportedEncodingException, MessagingException {
        return postMail(recipients, cc, bcc, subject, body, DataManager.getInstance().getConfiguration().getSmtpServer(),
                DataManager.getInstance().getConfiguration().getSmtpUser(), DataManager.getInstance().getConfiguration().getSmtpPassword(),
                DataManager.getInstance().getConfiguration().getSmtpSenderAddress(), DataManager.getInstance().getConfiguration().getSmtpSenderName(),
                DataManager.getInstance().getConfiguration().getSmtpSecurity(), DataManager.getInstance().getConfiguration().getSmtpPort());
    }

    /**
     * Sends an email to with the given subject and body to the given recipient list using the given SMTP parameters.
     *
     * @param recipients list of primary recipient email addresses
     * @param cc list of CC recipient email addresses
     * @param bcc list of BCC recipient email addresses
     * @param subject email subject line
     * @param body email body text (HTML)
     * @param smtpServer SMTP server hostname
     * @param smtpUser SMTP authentication username
     * @param smtpPassword SMTP authentication password
     * @param smtpSenderAddress sender email address
     * @param smtpSenderName sender display name
     * @param smtpSecurity security protocol (STARTTLS, SSL, or none)
     * @param inSmtpPort SMTP server port number
     * @return true if mail sent successfully; false otherwise
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private static boolean postMail(List<String> recipients, List<String> cc, List<String> bcc, String subject, String body, String smtpServer,
            final String smtpUser, final String smtpPassword, String smtpSenderAddress, String smtpSenderName, String smtpSecurity,
            final Integer inSmtpPort) throws MessagingException, UnsupportedEncodingException {
        if (recipients == null) {
            throw new IllegalArgumentException("recipients may not be null");
        }
        if (subject == null) {
            throw new IllegalArgumentException("subject may not be null");
        }
        if (body == null) {
            throw new IllegalArgumentException("body may not be null");
        }
        if (smtpServer == null) {
            throw new IllegalArgumentException("smtpServer may not be null");
        }
        if (smtpSenderAddress == null) {
            throw new IllegalArgumentException("smtpSenderAddress may not be null");
        }
        if (smtpSenderName == null) {
            throw new IllegalArgumentException("smtpSenderName may not be null");
        }
        if (smtpSecurity == null) {
            throw new IllegalArgumentException("smtpSecurity may not be null");
        }

        if (StringUtils.isNotEmpty(smtpPassword) && StringUtils.isEmpty(smtpUser)) {
            logger.warn("stmpPassword is configured but smtpUser is not, ignoring smtpPassword.");
        }

        boolean debug = false;
        boolean auth = true;
        if (StringUtils.isEmpty(smtpUser)) {
            auth = false;
        }
        Properties props = new Properties();
        int smtpPort = inSmtpPort;
        switch (smtpSecurity.toUpperCase()) {
            case "STARTTLS":
                logger.debug("Using STARTTLS");
                if (smtpPort == -1) {
                    smtpPort = 25;
                }
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
                props.setProperty("mail.smtp.host", smtpServer);
                //                props.setProperty("mail.smtp.ssl.trust", "*");
                //                props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
                props.setProperty("mail.smtp.starttls.enable", "true");
                props.setProperty("mail.smtp.starttls.required", "true");
                break;
            case "SSL":
                logger.debug("Using SSL");
                if (smtpPort == -1) {
                    smtpPort = 465;
                }
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.host", smtpServer);
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
                props.setProperty("mail.smtp.ssl.enable", "true");
                props.setProperty("mail.smtp.ssl.trust", "*");
                break;
            default:
                logger.debug("Using no SMTP security");
                if (smtpPort == -1) {
                    smtpPort = 25;
                }
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", String.valueOf(smtpPort));
                props.setProperty("mail.smtp.host", smtpServer);
        }
        props.setProperty("mail.smtp.connectiontimeout", "30000");
        props.setProperty("mail.smtp.timeout", "30000");
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        logger.debug("Connecting to email server {} on port {} via SMTP security {}", smtpServer, String.valueOf(smtpPort),
                smtpSecurity.toUpperCase());
        // logger.trace(props.toString()); //NOSONAR Debug

        Session session;
        if (auth) {
            //            props.setProperty("mail.smtp.user", smtpUser);
            //            props.setProperty("mail.smtp.password", smtpPassword);
            // with authentication
            session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });
        } else {
            // w/o authentication
            session = Session.getInstance(props, null);
        }
        session.setDebug(debug);

        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(smtpSenderAddress, smtpSenderName);
        msg.setFrom(addressFrom);

        // Recipients
        msg.setRecipients(Message.RecipientType.TO, prepareRecipients(recipients));

        // CC
        if (cc != null && !cc.isEmpty()) {
            msg.setRecipients(Message.RecipientType.CC, prepareRecipients(cc));
        }

        // BCC

        if (bcc != null && !bcc.isEmpty()) {
            msg.setRecipients(Message.RecipientType.BCC, prepareRecipients(bcc));
        }
        // Optional : You can also set your custom headers in the Email if you want
        // msg.addHeader("MyHeaderName", "myHeaderValue");
        msg.setSubject(subject);

        // Message body
        MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(body, "utf-8");
        messagePart.setHeader(HTTP_HEADER_CONTENT_TYPE, "text/html; charset=\"utf-8\"");
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        msg.setContent(multipart);

        msg.setSentDate(new Date());
        Transport.send(msg);

        return true;
    }

    /**
     *
     * @param recipients list of email address strings to convert
     * @return Given recipients as a InternetAddress[]
     * @throws AddressException
     */
    static InternetAddress[] prepareRecipients(List<String> recipients) throws AddressException {
        if (recipients == null) {
            return new InternetAddress[0];
        }

        InternetAddress[] ret = new InternetAddress[recipients.size()];
        int i = 0;
        for (String recipient : recipients) {
            ret[i] = new InternetAddress(recipient);
            i++;
        }

        return ret;
    }

    /**
     * Returns the remote IP address of the given HttpServletRequest. If multiple addresses are found in x-forwarded-for, the first in the list is
     * returned.
     *
     * @param request incoming HTTP servlet request to inspect
     * @return the resolved remote IP address of the client
     * @should parse ip address
     */
    public static String getIpAddress(HttpServletRequest request) {
        String address = ADDRESS_LOCALHOST_IPV4;
        if (request != null) {

            // Prefer address from x-forwarded-for
            address = request.getHeader("x-forwarded-for");
            if (address == null) {
                address = request.getHeader("X-Forwarded-For");
            }
            if (address == null) {
                address = request.getRemoteAddr();
            }
        }

        if (address == null) {
            address = ADDRESS_LOCALHOST_IPV4;
            logger.warn("Could not extract remote IP address, using localhost.");
        }

        // logger.trace("Pre-parsed IP address(es): {}", address); //NOSONAR Debug
        return parseMultipleIpAddresses(address); //NOSONAR address cannot be null here
    }

    /**
     * parseMultipleIpAddresses. If the given string contains more than one address, return the first one, otherwise the entire string
     *
     * @param address IP address
     * @return the first IP address from a comma-separated list, or the entire string if it contains only one address
     * @should return only the first IP address from a comma-separated list
     */
    protected static String parseMultipleIpAddresses(final String address) {
        if (address == null) {
            throw new IllegalArgumentException("address may not be null");
        }

        String ret = address;
        if (ret.contains(",")) {
            String[] addressSplit = ret.split(",");
            if (addressSplit.length > 0) {
                //Use the first address. According to specification, this should be the client ip
                ret = addressSplit[0].trim();
            }
        }

        // logger.trace("Parsed IP address: {}", ret); //NOSONAR Debug
        return ret;
    }

    /**
     * Replaces most of the given email address with asterisks.
     *
     * @param email email address to scramble
     * @return Scrambled email address
     * @should replace domain middle part with asterisks keeping first three and last three characters
     */
    public static String scrambleEmailAddress(String email) {
        if (StringUtils.isEmpty(email)) {
            return email;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < email.length(); ++i) {
            if (i > 2 && i < email.length() - 3) {
                sb.append('*');
            } else {
                sb.append(email.charAt(i));
            }
        }
        String ret = sb.toString();
        ret = ret.replaceAll("[*]+", "***");

        return ret;
    }

    /**
     * Replaces most the last two segments of the given IPv4 address with placeholders.
     *
     * @param address IP address
     * @return Scrambled IP address
     * @should replace last two octets of IP address with X
     */
    public static String scrambleIpAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return address;
        }

        String[] addressSplit = address.split("[.]");
        if (addressSplit.length == 4) {
            return addressSplit[0] + "." + addressSplit[1] + ".X.X";
        }

        return address;
    }

    /**
     *
     * @param address IP address
     * @return true if given address is a localhost address; false otherwise
     */
    public static boolean isIpAddressLocalhost(String address) {
        // logger.trace("isIpAddressLocalhost: {}", address); //NOSONAR Debug
        if (address == null) {
            return false;
        }

        return ADDRESS_LOCALHOST_IPV6.equals(address) || ADDRESS_LOCALHOST_IPV4.equals(address);
    }

    /**
     *
     * @param mode cache clear mode (all, content, thumbs, pdf)
     * @param pi persistent identifier of the record
     * @param rootUrl base URL of the viewer application
     * @param webApiToken authentication token for the web API
     * @return Generated URL
     * @should compose cache API URL with correct query params for each cache type
     */
    public static String buildClearCacheUrl(String mode, String pi, String rootUrl, String webApiToken) {
        if (mode == null) {
            throw new IllegalArgumentException("mode may not be null");
        }

        StringBuilder sbUrl =
                new StringBuilder(rootUrl).append("api/v1")
                        .append(ApiUrls.CACHE)
                        .append('/')
                        .append(pi)
                        .append("/?token=")
                        .append(webApiToken);
        switch (mode) {
            case NetTools.PARAM_CLEAR_CACHE_ALL:
                sbUrl.append("&content=true&thumbs=true&pdf=true");
                break;
            case NetTools.PARAM_CLEAR_CACHE_CONTENT:
            case NetTools.PARAM_CLEAR_CACHE_THUMBS:
            case NetTools.PARAM_CLEAR_CACHE_PDF:
                sbUrl.append("&").append(mode).append("=true");
                break;
            default:
                return "";
        }

        return sbUrl.toString();
    }

    /**
     * Return true if the given string is a whole number between 200 and 399 (inclusive).
     *
     * @param string HTTP status as {@link String}
     * @return true if HTTP code is in the 200-399 range; false otherwise
     */
    public static boolean isStatusOk(String string) {
        try {
            int code = Integer.parseInt(string);
            return 200 <= code && code < 400;
        } catch (NullPointerException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * 
     * @param subnetMask subnet mask in CIDR notation to validate
     * @return true if subnetMask valid; false otherwise
     */
    public static boolean isValidSubnetMask(String subnetMask) {
        try {
            new SubnetUtils(subnetMask);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if the request Contains a 'User-Agent' header matching the regex configured in {@link Configuration#getCrawlerDetectionRegex()}. If it
     * matches, the request is assumed be be from a web-crawler bot and not from a human. Identifying a web-crawler request via the
     * CrawlerSessionManagerValve session attribute does not work for this purpose since it is only applied to the session after the first request
     * 
     * @param request incoming HTTP servlet request to inspect
     * @return true if the request is made by a web crawler
     */
    public static boolean isCrawlerBotRequest(HttpServletRequest request) {
        String userAgent = request != null ? request.getHeader("User-Agent") : "";
        return StringUtils.isNotBlank(userAgent)
                && userAgent.matches(DataManager.getInstance().getConfiguration().getCrawlerDetectionRegex());

    }

    /**
     * Appends one or more query parameters to an existing URI.
     * 
     * @param uriString the URI as a string
     * @param queryParams A list of parameters. Each element of the list is assumed to be a list of size 2, whith the first element being the
     *            parameter name and the second the parameter value. If the list has only one item, it is assumed to be a parameter name without
     *            value, any elements after the second will be ignored
     * @return The URI with query params appended
     * @throws URISyntaxException
     */
    public static String addQueryParameters(String uriString, List<List<String>> queryParams) throws URISyntaxException {
        return addQueryParameters(new URI(uriString), queryParams).toString();
    }

    /**
     * Appends one or more query parameters to an existing URI.
     *
     * @param uri the base URI to which query parameters are appended
     * @param queryParams A list of parameters. Each element of the list is assumed to be a list of size 2, whith the first element being the
     *            parameter name and the second the parameter value. If the list has only one item, it is assumed to be a parameter name without
     *            value, any elements after the second will be ignored
     * @return The URI with query params appended
     * @throws URISyntaxException
     */
    public static URI addQueryParameters(final URI uri, List<List<String>> queryParams) {
        URI ret = uri;
        if (queryParams != null) {
            UriBuilder builder = UriBuilder.fromUri(uri);
            for (List<String> param : queryParams) {
                builder.queryParam(param.stream().findFirst().orElse(""), param.stream().skip(1).findFirst().orElse(""));
            }
            ret = builder.build();
        }

        return ret;
    }

}