package io.goobi.viewer.managedbeans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.json.JSONObject;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Faces;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.quartz.TaskTriggerStatus;

@Named
@ApplicationScoped
public class AdminDeveloperBean implements Serializable {

    private static final long serialVersionUID = 9068383748390523908L;

    private static final Logger logger = LogManager.getLogger(AdminDeveloperBean.class);

    private static final String SQL_STATEMENT_CREATE_USERS = "DROP TABLE IF EXISTS `users`;\n"
            + "CREATE TABLE `users` (\n"
            + "  `user_id` bigint(20) NOT NULL AUTO_INCREMENT,\n"
            + "  `activation_key` varchar(255) DEFAULT NULL,\n"
            + "  `active` tinyint(1) NOT NULL DEFAULT 0,\n"
            + "  `comments` varchar(255) DEFAULT NULL,\n"
            + "  `email` varchar(255) NOT NULL,\n"
            + "  `first_name` varchar(255) DEFAULT NULL,\n"
            + "  `last_login` datetime DEFAULT NULL,\n"
            + "  `last_name` varchar(255) DEFAULT NULL,\n"
            + "  `nickname` varchar(255) DEFAULT NULL,\n"
            + "  `password_hash` varchar(255) DEFAULT NULL,\n"
            + "  `score` bigint(20) DEFAULT NULL,\n"
            + "  `superuser` tinyint(1) NOT NULL DEFAULT 0,\n"
            + "  `suspended` tinyint(1) NOT NULL DEFAULT 0,\n"
            + "  `agreed_to_terms_of_use` tinyint(1) DEFAULT 0,\n"
            + "  `avatar_type` varchar(255) DEFAULT NULL,\n"
            + "  `local_avatar_updated` bigint(20) DEFAULT NULL,\n"
            + "  PRIMARY KEY (`user_id`),\n"
            + "  KEY `index_users_email` (`email`)\n"
            + ") ENGINE=InnoDB AUTO_INCREMENT=191 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;\n"
            + "";

    private static final String SQL_STATEMENT_ADD_SUPERUSER =
            "INSERT INTO users (active,email,password_hash,score,superuser) VALUES (1,\"goobi@intranda.com\","
                    + "\"$2a$10$Z5GTNKND9ZbuHt0ayDh0Remblc7pKUNlqbcoCxaNgKza05fLtkuYO\",0,1);";

    private static final String BASH_STATEMENT_CREATE_SQL_DUMP =
            "mysqldump $VIEWERDBNAME --ignore-table=viewer.crowdsourcing_fulltexts --ignore-table=viewer.users";

    private static final String[] FILES_TO_INCLUDE = new String[] { "config_viewer-module-crowdsourcing.xml", "messages_*.properties" };

    @Inject
    @Push
    private PushContext downloadContext;
    @Inject
    @Push
    private PushContext pullThemeContext;
    @Inject
    private ServletContext context;
    @Inject
    private transient MessageQueueManager queueManager;
    private transient Scheduler scheduler = null;

    private final String viewerThemeName;
    private final String viewerDatabaseName;
    private final String viewerConfigDirectory;

    public AdminDeveloperBean() {
        this(DataManager.getInstance().getConfiguration(), "viewer");
    }

    public AdminDeveloperBean(Configuration config, String persistenceUnitName) {
        viewerThemeName = config.getTheme();
        viewerDatabaseName = persistenceUnitName;
        viewerConfigDirectory = config.getConfigLocalPath();
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("Error getting quartz scheduler", e);
        }
    }

    public void downloadDeveloperArchive() {
        try {
            sendDownloadProgressUpdate(0);
            byte[] zip = createDeveloperArchive(p -> sendDownloadProgressUpdate(0.1f + p * 0.8f));
            logger.debug("Sending file...");
            Faces.sendFile(zip, this.viewerThemeName + "_developer.zip", true);
            logger.debug("Done sending file");
            sendDownloadFinished();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Bean thread interrupted while waiting for bash call to finish");
            sendDownloadError("Backing thread interrupted");
        } catch (IOException | JDOMException e) {
            logger.error("Error creating zip archive: {}", e.toString());
            sendDownloadError("Error creating zip archive: " + e.toString());
        }
    }

    public void activateAutopull() throws DAOException {
        if (!isAutopullActive()) {
            pauseJob(TaskType.PULL_THEME);
        }
    }

    public void triggerPullTheme() throws MessageQueueException {
        sendPullThemeUpdate(0f);
        ViewerMessage message = new ViewerMessage(TaskType.PULL_THEME.name());
        queueManager.addToQueue(message);
    }

    public boolean isAutopullActive() throws DAOException {
        RecurringTaskTrigger trigger = DataManager.getInstance().getDao().getRecurringTaskTriggerForTask(TaskType.PULL_THEME);
        return trigger != null && trigger.getStatus() == TaskTriggerStatus.RUNNING;
    }
    
    public boolean isAutopullError() throws DAOException {
        List<ViewerMessage> messages = DataManager.getInstance()
                .getDao()
                .getViewerMessages(0, 1, "lastUpdateTime", true,
                        Map.of("taskName", TaskType.PULL_THEME.name()));
        if (!messages.isEmpty()) {
            return messages.get(0).getMessageStatus() == MessageStatus.ERROR;
        } else {
            return false;
        }
    }

    public LocalDateTime getLastAutopull() throws DAOException {
        List<ViewerMessage> messages = DataManager.getInstance()
                .getDao()
                .getViewerMessages(0, 1, "lastUpdateTime", true,
                        Map.of("taskName", TaskType.PULL_THEME.name(), "messageStatus", MessageStatus.FINISH.name()));
        if (!messages.isEmpty()) {
            return messages.get(0).getLastUpdateTime();
        }
        return null;
    }

    public String getThemeName() {
        return this.viewerThemeName;
    }

    protected byte[] createDeveloperArchive(Consumer<Float> progressMonitor) throws InterruptedException, JDOMException, IOException {

        Map<Path, String> zipEntryMap = new HashMap<>();
        FilenameFilter filter = WildcardFileFilter.builder().setWildcards(FILES_TO_INCLUDE).get();

        zipEntryMap.put(Path.of("viewer/config/config_viewer.xml"), XmlTools.getStringFromElement(
                createDeveloperViewerConfig(Path.of(viewerConfigDirectory, "config_viewer.xml")).getRootElement(), StringTools.DEFAULT_ENCODING));
        progressMonitor.accept(0.2f);
        try {
            zipEntryMap.put(Path.of("viewer/config/viewer.sql"), createSqlDump());
            progressMonitor.accept(0.5f);
        } catch (IOException e) {
            logger.error("Error creating sql dump of viewer database: {}", e.toString());
        }
        for (File file : Path.of(viewerConfigDirectory).toFile().listFiles(filter)) {
            Path zipEntryPath = Path.of("viewer/config", file.getName());
            zipEntryMap.put(zipEntryPath, FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING));
        }
        progressMonitor.accept(0.7f);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FileTools.compressZip(out, zipEntryMap, 9);
            return out.toByteArray();
        } finally {
            progressMonitor.accept(1f);
        }
    }

    protected String createSqlDump() throws IOException, InterruptedException {
        String createSqlDumpStatement = BASH_STATEMENT_CREATE_SQL_DUMP.replace("$VIEWERDBNAME", this.viewerDatabaseName);
        ShellCommand command = new ShellCommand(createSqlDumpStatement.split("\\s+"));
        int ret = command.exec();
        if (ret < 1) {
            String output = command.getOutput();
            return new StringBuilder(output).append(SQL_STATEMENT_CREATE_USERS).append(SQL_STATEMENT_ADD_SUPERUSER).toString();
        }
        throw new IOException("Error executing command '" + createSqlDumpStatement + "':\t" + command.getErrorOutput());
    }

    protected Document createDeveloperViewerConfig(Path viewerConfigPath) throws IOException, JDOMException {
        Document configDoc = XmlTools.readXmlFile(viewerConfigPath);
        replaceSolrUrl(configDoc);
        renameElement(configDoc, "//config/urls/rest", "iiif");
        XmlTools.evaluateToFirstElement("//config/urls/iiif", configDoc.getRootElement(), Collections.emptyList())
                .ifPresent(ele -> ele.setAttribute("useForCmsMedia", "true"));
        addElement(configDoc, "//config/urls", "rest", "http://localhost:8080/viewer/api/v1/");
        return configDoc;
    }

    private static void replaceSolrUrl(Document configDoc) {
        Optional<String> restUrl = XmlTools.evaluateToFirstString("//config/urls/rest", configDoc.getRootElement(), Collections.emptyList());
        Optional<Element> solrElement = XmlTools.evaluateToFirstElement("//config/urls/solr", configDoc.getRootElement(), Collections.emptyList());
        restUrl.ifPresent(rest -> {
            String solrPath = solrElement.map(Element::getText).map(URI::create).map(URI::getPath).orElse("/solr/collection1");
            URI modifiedSolr = UriBuilder.fromUri(rest).replacePath(solrPath).build();
            solrElement.ifPresent(solrEle -> solrEle.setText(modifiedSolr.toString()));
        });
    }

    private static void renameElement(Document configDoc, String path, String newName) {
        Optional<Element> restUrl = XmlTools.evaluateToFirstElement(path, configDoc.getRootElement(), Collections.emptyList());
        restUrl.ifPresent(rest -> rest.setName(newName));
    }

    private static void addElement(Document configDoc, String parentPath, String name, String value) {
        Optional<Element> urlElement = XmlTools.evaluateToFirstElement(parentPath, configDoc.getRootElement(), Collections.emptyList());
        urlElement.ifPresent(urls -> {
            Element ele = new Element(name);
            ele.setText(value);
            urls.addContent(ele);
        });
    }

    private void pauseJob(TaskType taskType) {
        try {
            scheduler.pauseJob(new JobKey(taskType.name(), taskType.name()));
            persistTriggerStatus(taskType.name(), TaskTriggerStatus.PAUSED);
        } catch (SchedulerException e) {
            logger.error(e);
        }
    }

    private static void persistTriggerStatus(String jobName, TaskTriggerStatus status) {
        try {
            IDAO dao = DataManager.getInstance().getDao();
            RecurringTaskTrigger trigger = dao.getRecurringTaskTriggerForTask(TaskType.valueOf(jobName));
            trigger.setStatus(status);
            dao.updateRecurringTaskTrigger(trigger);
        } catch (DAOException e) {
            logger.error(e);
        }
    }

    private void sendDownloadFinished() {
        updateDownlaodProgress("finished", Optional.empty(), 1.0f);
    }

    private void sendDownloadError(String message) {
        updateDownlaodProgress("error", Optional.of(message), 0);
    }

    private void sendDownloadProgressUpdate(float progress) {
        updateDownlaodProgress("processing", Optional.empty(), progress);
    }
    
    

    private void updateDownlaodProgress(String status, Optional<String> message, float progress) {
        JSONObject json = new JSONObject();
        json.put("progress", progress);
        json.put("status", status);
        message.ifPresent(m -> json.put("message", m));
        downloadContext.send(json.toString());
    }
    
    
    public void sendPullThemeFinished() {
        updatePullThemeProgress("finished", Optional.empty(), 1.0f);
    }

    public void sendPullThemeError(String message) {
        updatePullThemeProgress("error", Optional.of(message), 0);
    }

    public void sendPullThemeUpdate(float progress) {
        updatePullThemeProgress("processing", Optional.empty(), progress);
    }
    
    private void updatePullThemeProgress(String status, Optional<String> message, float progress) {
        JSONObject json = new JSONObject();
        json.put("progress", progress);
        json.put("status", status);
        message.ifPresent(m -> json.put("message", m));
        pullThemeContext.send(json.toString());
    }

    public VersionInfo getThemeVersion() throws IOException {

        Path path = Path.of(context.getRealPath("META-INF/MANIFEST.MF"));
        if (Files.exists(path)) {
            VersionInfo info = VersionInfo.getFromManifest(Files.readString(path));
            if (info.buildDate == "?") {
                info = getVersionFromTomcatDirectory();
            }
            return info;
        } else {
            return getVersionFromTomcatDirectory();
        }

    }

    public VersionInfo getVersionFromTomcatDirectory() throws IOException {
        String tomcatBase = System.getProperty("catalina.base");
        String relPath = String.format("wtpwebapps/goobi-viewer-theme-%s/META-INF/MANIFEST.MF", this.getThemeName());
        Path path = Path.of(tomcatBase, relPath);
        if (Files.exists(path)) {
            return VersionInfo.getFromManifest(Files.readString(path));
        } else {
            return new VersionInfo("goobi-viewer-theme-" + this.viewerThemeName, "unknown", "unknown", "unknown");
        }
    }

    public static class VersionInfo {
        private final String applicationName;
        private final String buildDate;
        private final String gitRevision;
        private final String releaseVersion;

        public VersionInfo(String applicationName, String buildDate, String gitRevision, String releaseVersion) {
            this.applicationName = applicationName;
            this.buildDate = buildDate;
            this.gitRevision = gitRevision;
            this.releaseVersion = releaseVersion;
        }

        public static VersionInfo getFromManifest(String manifest) {
            return new VersionInfo(
                    getInfo("ApplicationName", manifest),
                    getInfo("Implementation-Build-Date", manifest),
                    getInfo("Implementation-Version", manifest),
                    getInfo("version", manifest));
        }

        private static String getInfo(String label, String infoText) {
            String regex = label + ": *(.*)";
            Matcher matcher = Pattern.compile(regex).matcher(infoText);
            if (matcher.find()) {
                return matcher.group(1);
            }

            return "?";
        }

        public String getApplicationName() {
            return applicationName;
        }

        public String getBuildDate() {
            return buildDate;
        }

        public String getGitRevision() {
            return gitRevision;
        }

        public String getReleaseVersion() {
            return releaseVersion;
        }

    }

}
