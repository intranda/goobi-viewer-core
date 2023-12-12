package io.goobi.viewer.managedbeans;

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
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
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
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.quartz.TaskTriggerStatus;

@Named("developerBean")
@SessionScoped
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
    
    private static final String SQL_STATEMENT_ADD_SUPERUSER = "INSERT INTO users (active,email,password_hash,score,superuser) VALUES (1,\"goobi@intranda.com\",\"$2a$10$Z5GTNKND9ZbuHt0ayDh0Remblc7pKUNlqbcoCxaNgKza05fLtkuYO\",0,1);";
    
    private static final String BASH_STATEMENT_CREATE_SQL_DUMP = "mysqldump $VIEWERDBNAME --ignore-table=viewer.crowdsourcing_fulltexts --ignore-table=viewer.users";
    
    private static final String[] FILES_TO_INCLUDE = new String[] {"config_viewer-module-crowdsourcing.xml", "messages_*.properties"};
    
    @Inject
    private MessageQueueManager queueManager;
    private Scheduler scheduler = null;
    
    private final Configuration config;
    private final String viewerDatabaseName;
    private final String viewerConfigDirectory;
    
    public AdminDeveloperBean() {
        this(DataManager.getInstance().getConfiguration());
    }

    public AdminDeveloperBean(Configuration config) {
        this.config = config;
        viewerDatabaseName = config.getTheme();
        viewerConfigDirectory = config.getConfigLocalPath();
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("Error getting quartz scheduler", e);
        }
    }
    
    public Path createDeveloperArchive() throws IOException, InterruptedException, JDOMException  {
        return createDeveloperArchive(Files.createTempDirectory("viewer_developer_"));
    }
    
    protected Path createDeveloperArchive(Path tempDir) throws IOException, InterruptedException, JDOMException  {
        
        Map<Path, String> zipEntryMap = new HashMap<>();
        FilenameFilter filter = WildcardFileFilter.builder().setWildcards(FILES_TO_INCLUDE).get();
        
        zipEntryMap.put(Path.of("viewer/config/config_viewer.xml"), XmlTools.getStringFromElement(createDeveloperViewerConfig(Path.of(viewerConfigDirectory, "config_viewer.xml")).getRootElement(), StringTools.DEFAULT_ENCODING));
        try {            
            zipEntryMap.put(Path.of("viewer/config/viewer.sql"), createSqlDump());
        } catch(IOException e) {
            logger.error("Error creating sql dump of viewer database: {}", e.toString());
        }
        for(File file : Path.of(viewerConfigDirectory).toFile().listFiles(filter)) {
            Path zipEntryPath = Path.of("viewer/config", file.getName().toString());
            zipEntryMap.put(zipEntryPath, FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING));
        }
        
        File zipFile = tempDir.resolve("developer.zip").toFile();
        FileTools.compressZipFile(zipEntryMap, zipFile, 9);
        return zipFile.toPath();
    }


    protected String createSqlDump() throws IOException, InterruptedException {
        String createSqlDumpStatement = BASH_STATEMENT_CREATE_SQL_DUMP.replace("$VIEWERDBNAME", this.viewerDatabaseName);
        ShellCommand command = new ShellCommand(createSqlDumpStatement.split("\\s+"));
        int ret = command.exec();
        if(ret < 1) {
            return new StringBuilder(command.getOutput()).append(SQL_STATEMENT_CREATE_USERS).append(SQL_STATEMENT_ADD_SUPERUSER).toString();
        } else {
            throw new IOException("Error executing command '" + createSqlDumpStatement + "':\t" + command.getErrorOutput());
        }
    }
    
    protected Document createDeveloperViewerConfig(Path viewerConfigPath) throws IOException, JDOMException {
        Document configDoc = XmlTools.readXmlFile(viewerConfigPath);
        replaceSolrUrl(configDoc);
        renameElement(configDoc, "//config/urls/rest", "iiif");
        XmlTools.evaluateToFirstElement("//config/urls/iiif", configDoc.getRootElement(), Collections.emptyList()).ifPresent(ele -> ele.setAttribute("useForCmsMedia", "true"));
        addElement(configDoc, "//config/urls", "rest", "http://localhost:8080/viewer/api/v1/");
        return configDoc;
    }

    private void replaceSolrUrl(Document configDoc) {
        Optional<String> restUrl = XmlTools.evaluateToFirstString("//config/urls/rest", configDoc.getRootElement(), Collections.emptyList());
        Optional<Element> solrElement = XmlTools.evaluateToFirstElement("//config/urls/solr", configDoc.getRootElement(), Collections.emptyList());
        restUrl.ifPresent(rest -> {
            String solrPath = solrElement.map(Element::getText).map(URI::create).map(URI::getPath).orElse("/solr/collection1");
            URI modifiedSolr = UriBuilder.fromUri(rest).replacePath(solrPath).build();
            solrElement.ifPresent(solrEle -> solrEle.setText(modifiedSolr.toString()));
        });
    }
    
    private void renameElement(Document configDoc, String path, String newName) {
        Optional<Element> restUrl = XmlTools.evaluateToFirstElement(path, configDoc.getRootElement(), Collections.emptyList());
        restUrl.ifPresent(rest -> rest.setName(newName));
    }
    
    private void addElement(Document configDoc, String parentPath, String name, String value) {
        Optional<Element> urlElement = XmlTools.evaluateToFirstElement(parentPath, configDoc.getRootElement(), Collections.emptyList());
        urlElement.ifPresent(urls -> {
            Element ele = new Element(name);
            ele.setText(value);
            urls.addContent(ele); 
        });
    }
    
    public void activateAutopull() throws DAOException {
        if(!isAutopullActive()) {
            pauseJob(TaskType.PULL_THEME);
        }
    }
    
    public void triggerPullTheme() throws MessageQueueException {
        ViewerMessage message = new ViewerMessage(TaskType.PULL_THEME.name());
        queueManager.addToQueue(message);
    }
    
    public boolean isAutopullActive() throws DAOException {
        RecurringTaskTrigger trigger = DataManager.getInstance().getDao().getRecurringTaskTriggerForTask(TaskType.PULL_THEME);
        return trigger != null && trigger.getStatus() == TaskTriggerStatus.RUNNING;
    }
    
    public LocalDateTime getLastAutopull() throws DAOException {
        RecurringTaskTrigger trigger = DataManager.getInstance().getDao().getRecurringTaskTriggerForTask(TaskType.PULL_THEME);
        return Optional.ofNullable(trigger).map(t -> t.getLastTimeTriggered()).orElse(null);

    }

    
    private void pauseJob(TaskType taskType) {
        try {
            scheduler.pauseJob(new JobKey(taskType.name(), taskType.name()));
            persistTriggerStatus(taskType.name(), TaskTriggerStatus.PAUSED);
        } catch (SchedulerException e) {
            logger.error(e);
        }
    }

    private void persistTriggerStatus(String jobName, TaskTriggerStatus status) {
        try {
            IDAO dao = DataManager.getInstance().getDao();
            RecurringTaskTrigger trigger = dao.getRecurringTaskTriggerForTask(TaskType.valueOf(jobName));
            trigger.setStatus(status);
            dao.updateRecurringTaskTrigger(trigger);
        } catch (DAOException e) {
            logger.error(e);
        }
    }
    
}
