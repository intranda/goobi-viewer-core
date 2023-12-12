package io.goobi.viewer.managedbeans;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.quartz.CronExpression;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.controller.shell.ShellCommand;

@Named
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

    private static final String BASH_STATEMENT_PULL_THEME_REPOSITORY = "cd $VIEWERTHEMEPATH; git pull | grep -v -e \"Already up-to-date.\" -e \"Bereits aktuell.\"";
    
    private final String[] FILES_TO_INCLUDE = new String[] {"config_viewer.xml", "messages_*.properties"};
    
    private final String viewerDatabaseName = "viewer";
    private final String viewerConfigDirectory = DataManager.getInstance().getConfiguration().getConfigLocalPath();
    
    public boolean isAutopullActive() {
        //TODO: implement
        return true;
    }
    
    public Instant getNextRunAutopull() throws ParseException {
        return getNextRunAutopull(Instant.now(), getAutpullCronExpression());
    }
    
    private String getAutpullCronExpression() {
        // TODO Auto-generated method stub
        return null;
    }
  
    public Instant getNextRunAutopull(Instant time, String expression) throws ParseException {
        CronExpression expr = new CronExpression(expression);
        Date lastDate = expr.getNextValidTimeAfter(Date.from(time));
        return lastDate.toInstant();
    }
    
    public static String convertCronExpression(String expr) {
        if(expr.endsWith("*")) {
            expr = expr.substring(0, expr.length()-1) + "?";
        }
        if(expr.split("\\s+").length < 6) {
            expr = "* " + expr; 
        }
        return expr;
    }
    
    public Path createDeveloperArchive() throws IOException, InterruptedException  {
        return createDeveloperArchive(Files.createTempDirectory("viewer_developer_"));
    }
    
    public Path createDeveloperArchive(Path tempDir) throws IOException, InterruptedException  {
        FilenameFilter filter = WildcardFileFilter.builder().setWildcards(FILES_TO_INCLUDE).get();
        File[] files = Path.of(this.viewerConfigDirectory).toFile().listFiles(filter);
        List<File> filesToZip = Stream.concat(Stream.of(files), Stream.of(createSqlDump(tempDir))).collect(Collectors.toList());
        File zipFile = tempDir.resolve("developer.zip").toFile();
        FileTools.compressZipFile(filesToZip, zipFile, 9);
        return zipFile.toPath();
    }
    
    public File createSqlDump(Path tempDir) throws IOException, InterruptedException {
        String createSqlDumpStatement = BASH_STATEMENT_CREATE_SQL_DUMP.replace("$VIEWERDBNAME", this.viewerDatabaseName);
        ShellCommand command = new ShellCommand(createSqlDumpStatement.split("\\s+"));
        int ret = command.exec();
        if(ret < 1) {
            Path dumpFile = tempDir.resolve("viewer.sql").toAbsolutePath();
            if(Files.exists(dumpFile)) {
                Files.delete(dumpFile);
            }
            Files.writeString(dumpFile, command.getOutput(), StandardOpenOption.CREATE);
            return dumpFile.toFile();
        } else {
            System.out.println("Output: " + command.getOutput());
            throw new IOException("Error executing command '" + createSqlDumpStatement + "':\t" + command.getErrorOutput());
        }
    }
    
    public Document createDeveloperViewerConfig(Path viewerConfigPath) throws IOException, JDOMException {
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
    
}
