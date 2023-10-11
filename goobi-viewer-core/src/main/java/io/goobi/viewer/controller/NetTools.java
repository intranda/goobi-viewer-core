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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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

/**
 * Utility methods for HTTP operations, mail, etc.
 *
 */
public class NetTools {

    private static final Logger logger = LogManager.getLogger(NetTools.class);

    private static final int HTTP_TIMEOUT = 30000;
    /** Constant <code>ADDRESS_LOCALHOST_IPV4="127.0.0.1"</code> */
    public static final String ADDRESS_LOCALHOST_IPV4 = "127.0.0.1";
    /** Constant <code>ADDRESS_LOCALHOST_IPV6="0:0:0:0:0:0:0:1"</code> */
    public static final String ADDRESS_LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
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
     * <p>
     * callUrlGET.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @return A String array with two elements. The first contains the HTTP status code, the second either the requested data (if status code is 200)
     *         or the error message.
     */
    public static String[] callUrlGET(String url) {
        // logger.trace("callUrlGET: {}", url);
        String[] ret = new String[2];
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
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
                        // logger.warn("Error code: {}", response.getStatusLine().getStatusCode());
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
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     */
    public static String getWebContentGET(String url) throws IOException, HTTPException {
        return getWebContentGET(url, HTTP_TIMEOUT);
    }

    /**
     * <p>
     * getWebContentGET.
     * </p>
     *
     * @param url URL to call
     * @param timeout Custom timeout
     * @return a {@link java.lang.String} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if any.
     */
    public static String getWebContentGET(String url, int timeout) throws IOException, HTTPException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
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
     * <p>
     * getWebContentPOST.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param headers
     * @param params a {@link java.util.Map} object.
     * @param cookies a {@link java.util.Map} object.
     * @param stringBody Optional entity content.
     * @param file
     * @return a {@link java.lang.String} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if return code is not 200
     */
    public static String getWebContentPOST(String url, Map<String, String> headers, Map<String, String> params, Map<String, String> cookies,
            String contentType, String stringBody, File file) throws IOException, HTTPException {
        return getWebContent("POST", url, headers, params, cookies, contentType, stringBody, file);
    }

    /**
     * <p>
     * getWebContentDELETE.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @param headers
     * @param params a {@link java.util.Map} object.
     * @param cookies a {@link java.util.Map} object.
     * @param stringBody Optional entity content.
     * @return a {@link java.lang.String} object.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.HTTPException if return code is not 200
     */
    public static String getWebContentDELETE(String url, Map<String, String> headers, Map<String, String> params, Map<String, String> cookies,
            String stringBody) throws IOException {
        return getWebContent(HTTP_METHOD_DELETE, url, headers, params, cookies, null, stringBody, null);
    }

    /**
     * <p>
     * getWebContent.
     * </p>
     *
     * @param method POST | PUT | DELETE
     * @param url a {@link java.lang.String} object.
     * @param headers
     * @param params a {@link java.util.Map} object.
     * @param cookies a {@link java.util.Map} object.
     * @param contentType Optional mime type.
     * @param stringBody Optional entity content.
     * @param file Optional file entity content.
     * @return a {@link java.lang.String} object.
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

        logger.debug("url: {}", url);
        List<NameValuePair> nameValuePairs = null;
        if (params == null) {
            nameValuePairs = new ArrayList<>(0);
        } else {
            nameValuePairs = new ArrayList<>(params.size());
            for (Entry<String, String> entry : params.entrySet()) {
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        HttpClientContext context = null;
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null && !cookies.isEmpty()) {
            context = HttpClientContext.create();
            for (Entry<String, String> entry : cookies.entrySet()) {
                BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), entry.getValue());
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
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {

            HttpRequestBase requestBase;
            switch (method.toUpperCase()) {
                case "POST":
                    requestBase = new HttpPost(url);
                    break;
                case "PUT":
                    requestBase = new HttpPut(url);
                    break;
                case "DELETE":
                    requestBase = new HttpDelete(url);
                    break;
                default:
                    return "";
            }
            //            if (StringUtils.isNotEmpty(contentType)) {
            //                requestBase.setHeader("Content-Type", contentType);
            //            }
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
                //                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
                return EntityUtils.toString(response.getEntity(), StringTools.DEFAULT_ENCODING);
            }
        }
    }

    /**
     * Sends an email to with the given subject and body to the given recipient list.
     *
     * @param recipients a {@link java.util.List} object.
     * @param cc
     * @param bcc
     * @param subject a {@link java.lang.String} object.
     * @param body a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws javax.mail.MessagingException if any.
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
     * @param recipients
     * @param cc
     * @param bcc
     * @param subject
     * @param body
     * @param smtpServer
     * @param smtpUser
     * @param smtpPassword
     * @param smtpSenderAddress
     * @param smtpSenderName
     * @param smtpSecurity
     * @param smtpPort
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private static boolean postMail(List<String> recipients, List<String> cc, List<String> bcc, String subject, String body, String smtpServer,
            final String smtpUser, final String smtpPassword, String smtpSenderAddress, String smtpSenderName, String smtpSecurity, Integer smtpPort)
            throws MessagingException, UnsupportedEncodingException {
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
        // logger.trace(props.toString());

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
        {
            // Message body
            MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(body, "utf-8");
            messagePart.setHeader(HTTP_HEADER_CONTENT_TYPE, "text/html; charset=\"utf-8\"");
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);
            msg.setContent(multipart);
        }
        msg.setSentDate(new Date());
        Transport.send(msg);

        return true;
    }

    /**
     *
     * @param recipients
     * @return
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
     * Returns the remote IP address of the given HttpServletRequest. If multiple addresses are found in x-forwarded-for, the last in the list is
     * returned.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getIpAddress(HttpServletRequest request) {
        String address = ADDRESS_LOCALHOST_IPV4;
        if (request != null) {
            //            if (logger.isTraceEnabled()) {
            //                Enumeration<String> headerNames = request.getHeaderNames();
            //                while (headerNames.hasMoreElements()) {
            //                    String headerName = headerNames.nextElement();
            //                    logger.trace("request header '{}':'{}'", headerName, request.getHeader(headerName));
            //                }
            //            }

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

        // logger.trace("Pre-parsed IP address(es): {}", address);
        return parseMultipleIpAddresses(address);
    }

    /**
     * <p>
     * parseMultipleIpAddresses.
     * </p>
     *
     * @param address a {@link java.lang.String} object.
     * @should filter multiple addresses correctly
     * @return a {@link java.lang.String} object.
     */
    protected static String parseMultipleIpAddresses(String address) {
        if (address == null) {
            throw new IllegalArgumentException("address may not be null");
        }

        if (address.contains(",")) {
            String[] addressSplit = address.split(",");
            if (addressSplit.length > 0) {
                address = addressSplit[addressSplit.length - 1].trim();
            }
        }

        // logger.trace("Parsed IP address: {}", address);
        return address;
    }

    /**
     * Replaces most of the given email address with asterisks.
     *
     * @param email
     * @return Scrambled email address
     * @should modify string correctly
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
     * @param address
     * @return Scrambled IP address
     * @should modify string correctly
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
     * @param address
     * @return
     */
    public static boolean isIpAddressLocalhost(String address) {
        // logger.trace("isIpAddressLocalhost: {}", address);
        if (address == null) {
            return false;
        }

        return ADDRESS_LOCALHOST_IPV6.equals(address) || ADDRESS_LOCALHOST_IPV4.equals(address);
    }

    /**
     *
     * @param mode
     * @param pi
     * @param rootUrl
     * @param webApiToken
     * @return
     * @should build url correctly
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
     * return true if the given string is a whole number between 200 and 399 (inclusive)
     *
     * @param string
     * @return
     */
    public static boolean isStatusOk(String string) {
        try {
            int code = Integer.parseInt(string);
            return 200 <= code && code < 400;
        } catch (NullPointerException | NumberFormatException e) {
            return false;
        }
    }

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
     * {@link org.apache.catalina.valves.CrawlerSessionManagerValve} session attribute does not work for this purpose since it is only applied to the
     * session after the first request
     * 
     * @param request
     * @return true if the request is made by a web crawler
     */
    public static boolean isCrawlerBotRequest(HttpServletRequest request) {
        String userAgent = request != null ? request.getHeader("User-Agent") : "";
        return StringUtils.isNotBlank(userAgent) &&
                userAgent.matches(DataManager.getInstance().getConfiguration().getCrawlerDetectionRegex());

    }

    /**
     * Append one or more query parameters to an existing URI
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
     * Append one or more query parameters to an existing URI
     * 
     * @param uriString the URI
     * @param queryParams A list of parameters. Each element of the list is assumed to be a list of size 2, whith the first element being the
     *            parameter name and the second the parameter value. If the list has only one item, it is assumed to be a parameter name without
     *            value, any elements after the second will be ignored
     * @return The URI with query params appended
     * @throws URISyntaxException
     */
    public static URI addQueryParameters(URI uri, List<List<String>> queryParams) {
        if (queryParams != null) {
            UriBuilder builder = UriBuilder.fromUri(uri);
            for (List<String> param : queryParams) {
                builder.queryParam(param.stream().findFirst().orElse(""), param.stream().skip(1).findFirst().orElse(""));
            }
            uri = builder.build();
        }
        return uri;
    }

}
