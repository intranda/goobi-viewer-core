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
package de.intranda.digiverso.presentation.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import de.intranda.digiverso.presentation.Version;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.AccessDeniedException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.ModuleMissingException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.modules.IModule;

/**
 * Helper methods.
 */
public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    public static final String REGEX_QUOTATION_MARKS = "\"[^()]*?\"";
    public static final String REGEX_PARENTHESES = "\\([^()]*\\)";
    public static final String REGEX_PARENTESES_DATES = "\\([\\w|\\s|\\-|\\.|\\?]+\\)";
    public static final String REGEX_BRACES = "\\{(\\w+)\\}";
    public static final String REGEX_WORDS = "[a-zäáàâöóòôüúùûëéèêßñ0123456789]+";
    public static final String ADDRESS_LOCALHOST_IPV4 = "127.0.0.1";
    public static final String ADDRESS_LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    public static final String DEFAULT_ENCODING = "UTF-8";

    // TODO remove constants
    public static final String SUFFIX_FULLTEXT_CROWDSOURCING = "_txtcrowd";
    public static final String SUFFIX_ALTO_CROWDSOURCING = "_altocrowd";
    public static final String SUFFIX_USER_GENERATED_CONTENT = "_ugc";

    private static final int HTTP_TIMEOUT = 10000;

    public static DecimalFormat dfTwoDecimals = new DecimalFormat("0.00");
    public static DecimalFormat dfTwoDigitInteger = new DecimalFormat("00");

    public static Namespace nsAlto = Namespace.getNamespace("alto", "http://www.loc.gov/standards/alto/ns-v2#");
    // TODO final namespaces
    public static Namespace nsIntrandaViewerOverviewPage =
            Namespace.getNamespace("iv_overviewpage", "http://www.intranda.com/digiverso/intrandaviewer/overviewpage");
    public static Namespace nsIntrandaViewerCrowdsourcing =
            Namespace.getNamespace("iv_crowdsourcing", "http://www.intranda.com/digiverso/intrandaviewer/crowdsourcing");

    /**
     * Translation method for Java code. (Re-)loads resource bundles if necessary.
     *
     * @param text Message key to translate.
     * @param locale
     * @return
     */
    public static String getTranslation(String text, Locale locale) {
        return ViewerResourceBundle.getTranslation(text, locale);
    }

    /**
     * Creates an MD5 hash of the given String.
     *
     * @param myString
     * @return MD5 hash
     * @should hash string correctly
     */
    public static String generateMD5(String myString) {
        String answer = "";
        try {
            byte[] defaultBytes = myString.getBytes("UTF-8");
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();

            StringBuffer hexString = new StringBuffer();
            for (byte element : messageDigest) {
                String hex = Integer.toHexString(0xFF & element);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            answer = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        return answer;
    }

    /**
     * Returns the remote IP address of the given HttpServletRequest. If multiple addresses are found in x-forwarded-for, the last in the list is
     * returned.
     *
     * @param request
     * @return
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
        return

        parseMultipleIpAddresses(address);
    }

    /**
     *
     * @param address
     * @return
     * @should filter multiple addresses correctly
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
     * Sends an email to with the given subject and body to the given recipient list.
     *
     * @param recipients
     * @param subject
     * @param body
     * @return
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    public static boolean postMail(List<String> recipients, String subject, String body) throws UnsupportedEncodingException, MessagingException {
        return Helper.postMail(recipients, subject, body, DataManager.getInstance().getConfiguration().getSmtpServer(),
                DataManager.getInstance().getConfiguration().getSmtpUser(), DataManager.getInstance().getConfiguration().getSmtpPassword(),
                DataManager.getInstance().getConfiguration().getSmtpSenderAddress(), DataManager.getInstance().getConfiguration().getSmtpSenderName(),
                DataManager.getInstance().getConfiguration().getSmtpSecurity());
    }

    /**
     * Sends an email to with the given subject and body to the given recipient list using the given SMTP parameters.
     *
     * @param recipients
     * @param subject
     * @param body
     * @param smtpServer
     * @param smtpUser
     * @param smtpPassword
     * @param smtpSenderAddress
     * @param smtpSenderName
     * @param smtpSecurity
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private static boolean postMail(List<String> recipients, String subject, String body, String smtpServer, final String smtpUser,
            final String smtpPassword, String smtpSenderAddress, String smtpSenderName, String smtpSecurity)
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
        String security = DataManager.getInstance().getConfiguration().getSmtpSecurity();
        Properties props = new Properties();
        switch (security.toUpperCase()) {
            case "STARTTLS":
                logger.debug("Using STARTTLS");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", "25");
                props.setProperty("mail.smtp.host", smtpServer);
                props.setProperty("mail.smtp.ssl.trust", "*");
                props.setProperty("mail.smtp.starttls.enable", "true");
                props.setProperty("mail.smtp.starttls.required", "true");
                break;
            case "SSL":
                logger.debug("Using SSL");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.host", smtpServer);
                props.setProperty("mail.smtp.port", "465");
                props.setProperty("mail.smtp.ssl.enable", "true");
                props.setProperty("mail.smtp.ssl.trust", "*");
                break;
            default:
                logger.debug("Using no SMTP security");
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.smtp.port", "25");
                props.setProperty("mail.smtp.host", smtpServer);
        }
        props.setProperty("mail.smtp.connectiontimeout", "15000");
        props.setProperty("mail.smtp.timeout", "15000");
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        // logger.trace(props.toString());

        Session session;
        if (auth) {
            // with authentication
            session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpUser);
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
        InternetAddress[] addressTo = new InternetAddress[recipients.size()];
        int i = 0;
        for (String recipient : recipients) {
            addressTo[i] = new InternetAddress(recipient);
            i++;
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
        // Optional : You can also set your custom headers in the Email if you
        // Want
        // msg.addHeader("MyHeaderName", "myHeaderValue");
        msg.setSubject(subject);
        {
            // Message body
            MimeBodyPart messagePart = new MimeBodyPart();
            messagePart.setText(body, "utf-8");
            // messagePart.setHeader("Content-Type", "text/plain; charset=\"utf-8\"");
            messagePart.setHeader("Content-Type", "text/html; charset=\"utf-8\"");
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
     * @param object Object to serialize.
     * @return
     */
    public static String serializeWithXStream(Object object, Converter converter) {
        XStream xStream = new XStream();
        if (converter != null) {
            xStream.registerConverter(converter);
        }
        xStream.autodetectAnnotations(true);
        HierarchicalStreamWriter xmlWriter = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(baos, DEFAULT_ENCODING)) {
            xmlWriter = new PrettyPrintWriter(writer);
            xStream.marshal(object, xmlWriter);
            xmlWriter.close();
            return new String(baos.toByteArray(), DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return "";
    }

    /**
     * @param data
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Object deSerializeWithXStream(String data, Class clazz, Converter converter) {
        XStream xstream = new XStream();
        if (converter != null) {
            xstream.registerConverter(converter);
        }
        xstream.processAnnotations(clazz);
        return xstream.fromXML(data);
    }

    /**
     * Re-index in background thread to significantly decrease saving times.
     * 
     * @param pageCompleted
     * @return
     * @throws ModuleMissingException
     */
    public static void triggerReIndexRecord(String pi, String recordType, OverviewPage overviewPage) {
        Thread backgroundThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (!Helper.reIndexRecord(pi, recordType, overviewPage)) {
                        logger.error("Failed to re-index  record {}", pi);
                        Messages.error("reIndexRecordFailure");
                    } else {
                        Messages.info("reIndexRecordSuccess");
                    }
                } catch (DAOException e) {
                    logger.error("Failed to reindex record " + pi + ": " + e.getMessage(), e);
                    Messages.error("reIndexRecordFailure");
                }
            }
        });

        logger.debug("Re-indexing record {}", pi);
        backgroundThread.start();
    }

    /**
     * Writes the record into the hotfolder for re-indexing. Modules can contribute data for re-indexing. Execution of method can take a while, so if
     * performance is of importance, use <code>triggerReIndexRecord</code> instead.
     * 
     * @param pi
     * @param recordType
     * @param overviewPage
     * @return
     * @throws DAOException
     * @should write overview page data
     */
    public static synchronized boolean reIndexRecord(String pi, String recordType, OverviewPage overviewPage) throws DAOException {
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }

        String dataRepository = null;
        try {
            dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            return false;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }

        String filePath = getDataFilePath(pi + ".xml", dataRepository, recordType);
        File recordXmlFile = new File(filePath);
        if (!recordXmlFile.isFile()) {
            logger.error("Cannot re-index '{}': record not found.", recordXmlFile.getAbsolutePath());
            return false;
        }
        logger.info("Preparing to re-index record: {}", recordXmlFile.getAbsolutePath());
        StringBuilder sbNamingScheme = new StringBuilder(pi);
        // TODO remove crowdsourcing constants
        File fulltextDir =
                new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_FULLTEXT_CROWDSOURCING);
        File altoDir = new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + SUFFIX_ALTO_CROWDSOURCING);

        // If the same record is already being indexed, use an alternative naming scheme
        File recordXmlFileInHotfolder = new File(DataManager.getInstance().getConfiguration().getHotfolder(), recordXmlFile.getName());
        if (recordXmlFileInHotfolder.exists() || fulltextDir.exists() || altoDir.exists()) {
            logger.info("'{}' is already being indexed, looking for an alternative naming scheme...", sbNamingScheme.toString());
            int iteration = 0;
            // Just checking for the presence of the record XML file at this
            // point, because this method is synchronized and no two
            // instances should be running at the same time.
            while ((recordXmlFileInHotfolder = new File(DataManager.getInstance().getConfiguration().getHotfolder(), pi + "#" + iteration + ".xml"))
                    .exists()) {
                iteration++;
            }
            sbNamingScheme.append('#').append(iteration);
            logger.info("Alternative naming scheme: {}", sbNamingScheme.toString());
        }

        // TODO Export overview page contents
        if (overviewPage == null) {
            overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(pi, null, null);
        }
        if (overviewPage != null) {
            try {
                overviewPage.exportTextData(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Module augmentations
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                module.augmentReIndexRecord(pi, dataRepository, sbNamingScheme.toString());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Finally, copy the record XML file to the hotfolder
        try {
            FileUtils.copyFile(recordXmlFile,
                    new File(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + ".xml"));
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return false;
    }

    /**
     * 
     * @param pi
     * @param page
     * @param recordType
     * @return
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IOException
     */
    public static synchronized boolean reIndexPage(String pi, int page, String recordType)
            throws DAOException, PresentationException, IndexUnreachableException, IOException {
        logger.trace("reIndexPage: {}/{}", pi, page);
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("Illegal page number: " + page);
        }

        String dataRepository = DataManager.getInstance().getSearchIndex().findDataRepository(pi);

        String query = new StringBuilder().append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(page)
                .append(" AND ")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE.name())
                .toString();
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Arrays
                .asList(new String[] { SolrConstants.IDDOC, SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.UGCTERMS }));

        if (doc == null) {
            logger.error("No Solr document found for {}/{}", pi, page);
            return false;
        }
        String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
        StringBuilder sbNamingScheme = new StringBuilder(pi).append('#').append(iddoc);

        // Module augmentations
        boolean writeTriggerFile = true;
        for (IModule module : DataManager.getInstance().getModules()) {
            try {
                if (!module.augmentReIndexPage(pi, page, doc, recordType, dataRepository, sbNamingScheme.toString())) {
                    writeTriggerFile = false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Create trigger file in hotfolder
        if (writeTriggerFile) {
            Path triggerFile = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder(), sbNamingScheme.toString() + ".docupdate");
            Files.createFile(triggerFile);
        }

        return true;
    }

    /**
     *
     * @param url Destination URL. Must contain all required GET parameters.
     * @param data String to send as a stream.
     * @return
     */
    public static synchronized boolean sendDataAsStream(String url, String data) {
        try (InputStream is = IOUtils.toInputStream(data, "UTF-8")) {
            HttpEntity entity = new InputStreamEntity(is, -1);
            int code = simplePOSTRequest(url, entity);
            switch (code) {
                case HttpStatus.SC_OK:
                    return true;
                default:
                    return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends the given HttpEntity to the given URL via HTTP POST. Only returns a status code.
     *
     * @param url
     * @param entity
     * @return
     */
    private static int simplePOSTRequest(String url, HttpEntity entity) {
        logger.debug(url);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpPost post = new HttpPost(url);
            Charset.forName(DEFAULT_ENCODING);
            post.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(post); StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code != HttpStatus.SC_OK) {
                    logger.error("{}: {}", code, response.getStatusLine().getReasonPhrase());
                }
                return code;
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return -1;
    }

    /**
     * Builds full-text document REST URL.
     * 
     * @param dataRepository
     * @param filePath
     * @return Full REST URL
     * @throws ViewerConfigurationException
     * @should build url correctly
     */
    public static String buildFullTextUrl(String dataRepository, String filePath) throws ViewerConfigurationException {
        return new StringBuilder(DataManager.getInstance().getConfiguration().getContentRestApiUrl()).append("document/")
                .append(StringUtils.isEmpty(dataRepository) ? '-' : dataRepository)
                .append('/')
                .append(filePath)
                .append('/')
                .toString();
    }

    /**
     * 
     * @param urlString
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     */
    public static String getWebContentGET(String urlString) throws ClientProtocolException, IOException, HTTPException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT)
                .setConnectTimeout(HTTP_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build()) {
            HttpGet get = new HttpGet(urlString);
            try (CloseableHttpResponse response = httpClient.execute(get); StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), DEFAULT_ENCODING);
                    // IOUtils.copy(response.getEntity().getContent(), writer);
                    // return writer.toString();
                }
                logger.trace("{}: {}\n{}", code, response.getStatusLine().getReasonPhrase(), IOUtils.toString(response.getEntity().getContent()));
                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * 
     * @param url
     * @param params
     * @param cookies
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws HTTPException
     */
    public static String getWebContentPOST(String url, Map<String, String> params, Map<String, String> cookies)
            throws ClientProtocolException, IOException, HTTPException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        logger.trace("url: {}", url);
        List<NameValuePair> nameValuePairs = null;
        if (params == null) {
            nameValuePairs = new ArrayList<>(0);
        } else {
            nameValuePairs = new ArrayList<>(params.size());
            for (String key : params.keySet()) {
                // logger.trace("param: {}:{}", key, params.get(key)); // TODO do not log passwords!
                nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        HttpClientContext context = null;
        CookieStore cookieStore = new BasicCookieStore();
        if (cookies != null && !cookies.isEmpty()) {
            context = HttpClientContext.create();
            for (String key : cookies.keySet()) {
                // logger.trace("cookie: {}:{}", key, cookies.get(key)); // TODO do not log passwords!
                BasicClientCookie cookie = new BasicClientCookie(key, cookies.get(key));
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
            HttpPost post = new HttpPost(url);
            Charset.forName(DEFAULT_ENCODING);
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            try (CloseableHttpResponse response = (context == null ? httpClient.execute(post) : httpClient.execute(post, context));
                    StringWriter writer = new StringWriter()) {
                int code = response.getStatusLine().getStatusCode();
                if (code == HttpStatus.SC_OK) {
                    logger.trace("{}: {}", code, response.getStatusLine().getReasonPhrase());
                    IOUtils.copy(response.getEntity().getContent(), writer);
                    return writer.toString();
                }
                logger.trace("{}: {}\n{}", code, response.getStatusLine().getReasonPhrase(), IOUtils.toString(response.getEntity().getContent()));
                throw new HTTPException(code, response.getStatusLine().getReasonPhrase());
            }
        }
    }

    /**
     * Returns the absolute path to the data repository with the given name (including a slash at the end).
     *
     * @param dataRepository
     * @return
     */
    public static String getRepositoryPath(String dataRepository) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(dataRepository)) {
            sb.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(dataRepository).append('/');
        } else {
            sb.append(DataManager.getInstance().getConfiguration().getViewerHome());
        }

        return sb.toString();
    }

    /**
     *
     * @param fileName
     * @param dataRepository
     * @param format
     * @return
     * @should construct METS file path correctly
     * @should construct LIDO file path correctly
     * @should throw IllegalArgumentException if fileName is null
     * @should throw IllegalArgumentException if format is unknown
     */
    public static String getDataFilePath(String fileName, String dataRepository, String format) {
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName may not be null or empty");
        }
        if (!SolrConstants._METS.equals(format) && !SolrConstants._LIDO.equals(format)) {
            throw new IllegalArgumentException("format must be METS or LIDO");
        }

        StringBuilder sb = new StringBuilder(getRepositoryPath(dataRepository));
        switch (format) {
            case SolrConstants._METS:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
                break;
            case SolrConstants._LIDO:
                sb.append(DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
                break;
        }
        sb.append('/').append(fileName);

        return sb.toString();
    }

    /**
     * 
     * @param pi
     * @param fileName
     * @param dataRepository
     * @param format
     * @return
     * @should return correct path
     */
    public static String getTextFilePath(String pi, String fileName, String dataRepository, String format) {
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("fileName may not be null or empty");
        }

        StringBuilder sb = new StringBuilder(getRepositoryPath(dataRepository));
        switch (format) {
            case SolrConstants.FILENAME_ALTO:
                sb.append(DataManager.getInstance().getConfiguration().getAltoFolder());
                break;
            case SolrConstants.FILENAME_FULLTEXT:
                sb.append(DataManager.getInstance().getConfiguration().getFulltextFolder());
                break;
            case SolrConstants.FILENAME_TEI:
                sb.append(DataManager.getInstance().getConfiguration().getTeiFolder());
                break;
        }
        sb.append('/').append(pi).append('/').append(fileName);

        return sb.toString();
    }

    /**
     * Returns the application version number.
     *
     * @return
     */
    public static String getVersion() {
        return Version.VERSION + "-" + Version.BUILDDATE + "-" + Version.BUILDVERSION;
    }

    public static void main(String[] args) throws DAOException {
        // FullText ft = new FullText();
        // ft.setPi("18979459-1830");
        // ft.setPage(1);
        // ft.setFileName("00375667.png");
        // ft.setText("Als Anhang zum Wohnungsanzeiger...");
        // ft.setUpdatedBy(DataManager.getInstance().getDao().getUser(1));
        // ft.setDateUpdated(new Date());
        // try {
        // DataManager.getInstance().getDao().addFullText(ft);
        // } catch (PresentationException e) {
        // e.printStackTrace();
        // }

        Helper.reIndexRecord("PPN517154005", "METS", null);
    }

    public static String intern(String string) {
        if (string == null) {
            return null;
        }
        return string.intern();
    }

    /**
     * Loads plain full-text via the REST service. ALTO is preferred (and converted to plain text, with a plain text fallback.
     * 
     * @param pi
     * @param dataRepository
     * @param altoFilePath ALTO file path relative to the repository root (e.g. "alto/PPN123/00000001.xml")
     * @param fulltextFilePath plain full-text file path relative to the repository root (e.g. "fulltext/PPN123/00000001.xml")
     * @param request
     * @return
     * @throws AccessDeniedException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @should load fulltext from alto correctly
     * @should load fulltext from plain text correctly
     */
    public static String loadFulltext(String dataRepository, String altoFilePath, String fulltextFilePath, HttpServletRequest request)
            throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (altoFilePath != null) {
            // ALTO file
            String alto = loadFulltext(dataRepository, altoFilePath, request);
            if (alto != null) {
                return ALTOTools.getFullText(alto, request);

            }
        }
        if (fulltextFilePath != null) {
            // Plain full-text file
            String fulltext = loadFulltext(dataRepository, fulltextFilePath, request);
            if (fulltext != null) {
                return fulltext;
            }
        }

        return null;
    }

    /**
     * Loads given text file path as a string, if the client has full-text access permission.
     * 
     * @param pi
     * @param dataRepository
     * @param filePath File path consisting of three party (datafolder/pi/filename); There must be two separators in the path!
     * @param request
     * @return
     * @throws AccessDeniedException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should return file content correctly
     */
    public static String loadFulltext(String dataRepository, String filePath, HttpServletRequest request)
            throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (filePath == null) {
            return null;
        }

        String url = Helper.buildFullTextUrl(dataRepository, filePath);
        try {
            return Helper.getWebContentGET(url);
        } catch (HTTPException e) {
            logger.error("Could not retrieve file from {}", url);
            logger.error(e.getMessage());
            if (e.getCode() == 403) {
                logger.debug("Access denied for text file {}", filePath);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
        }

        return null;
    }

    /**
     * 
     * @param pi
     * @param language
     * @return
     * @throws AccessDeniedException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ViewerConfigurationException
     */
    public static String loadTei(String pi, String language)
            throws AccessDeniedException, FileNotFoundException, IOException, ViewerConfigurationException {
        logger.trace("loadTei: {}/{}", pi, language);
        if (pi == null) {
            return null;
        }

        String url = new StringBuilder(DataManager.getInstance().getConfiguration().getContentRestApiUrl()).append("tei/")
                .append(pi)
                .append('/')
                .append(language)
                .append('/')
                .toString();
        try {
            return Helper.getWebContentGET(url);
        } catch (HTTPException e) {
            logger.error("Could not retrieve file from {}", url);
            logger.error(e.getMessage());
            if (e.getCode() == 403) {
                logger.debug("Access denied for TEI file {}/{}", pi, language);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
        }

        return null;
    }

    /**
     * @param childText
     * @param b
     * @return
     */
    public static boolean parseBoolean(String text, boolean defaultValue) {
        if("FALSE".equalsIgnoreCase(text)) {
            return false;
        } else if("TRUE".equalsIgnoreCase(text)) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * @param childText
     * @return
     */
    public static boolean parseBoolean(String text) {
        return parseBoolean(text, false);
    }
}
