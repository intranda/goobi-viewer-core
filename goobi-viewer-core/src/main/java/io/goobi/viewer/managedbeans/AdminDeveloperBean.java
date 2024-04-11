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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.controller.variablereplacer.VariableReplacer;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.PullThemeHandler;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.quartz.TaskTriggerStatus;

@Named
@ApplicationScoped
public class AdminDeveloperBean implements Serializable {

    private static final long serialVersionUID = 9068383748390523908L;

    private static final Logger logger = LogManager.getLogger(AdminDeveloperBean.class);

    private static final long CREATE_DEVELOPER_PACKAGE_TIMEOUT = 120_000; //2 min

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

    public AdminDeveloperBean() {
        this(DataManager.getInstance().getConfiguration(), "viewer");
    }

    public AdminDeveloperBean(Configuration config, String persistenceUnitName) {
        viewerThemeName = config.getTheme();
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("Error getting quartz scheduler", e);
        }
    }

    public void downloadDeveloperArchive() {
        Path zipPath;
        try {
            sendDownloadProgressUpdate(0);
            zipPath = createZipFile(DataManager.getInstance().getConfiguration().getCreateDeveloperPackageScriptPath());
            if (Files.exists(zipPath)) {
                sendDownloadProgressUpdate(1);
            } else {
                throw new IOException("Failed to create file " + zipPath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Bean thread interrupted while waiting for bash call to finish");
            sendDownloadError("Backing thread interrupted");
            return;
        } catch (IOException e) {
            logger.error("Error creating zip archive: {}", e.toString());
            sendDownloadError("Error creating zip archive: " + e.getMessage());
            return;
        }
        try {
            String uploadFilename = this.viewerThemeName + "_developer.zip";
            logger.debug("Sending file {} as {}", zipPath, uploadFilename);
            Faces.sendFile(zipPath, uploadFilename, true);
            logger.debug("File {} sent successfully", zipPath);
            sendDownloadFinished();
        } catch (IOException e) {
            logger.error("Error creating zip archive: {}", e.toString());
            sendDownloadError("Error creating zip archive: " + e.getMessage());
        } finally {
            try {
                logger.debug("Deleting file {}", zipPath);
                Files.deleteIfExists(zipPath);
                logger.debug("File {} deleted successfully", zipPath);
            } catch (IOException e) {
                logger.error("Failed to delete developer zip archive {}. Please delete manually", zipPath);
                sendDownloadError("Failed to delete developer zip archive " + zipPath + ". Please delete manually");
            }
        }
    }

    private static Path createZipFile(String createDeveloperPackageScriptPath) throws IOException, InterruptedException {
        String commandString = new VariableReplacer(DataManager.getInstance().getConfiguration()).replace(createDeveloperPackageScriptPath);
        ShellCommand command = new ShellCommand(commandString.split("\\s+"));
        int ret = command.exec(CREATE_DEVELOPER_PACKAGE_TIMEOUT);
        String out = command.getOutput().trim();
        String error = command.getErrorOutput().trim();
        if (ret > 0) {
            throw new IOException(error);
        } else {
            return Path.of(out);
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
        message.setMaxRetries(1);
        queueManager.addToQueue(message);
    }

    public boolean isAutopullActive() throws DAOException {
        List<ViewerMessage> messages = DataManager.getInstance()
                .getDao()
                .getViewerMessages(0, 1, "lastUpdateTime", true,
                        Map.of("taskName", TaskType.PULL_THEME.name()));
        if (!messages.isEmpty()) {
            return messages.get(0).getMessageStatus() == MessageStatus.FINISH;
        }
        return false;
    }

    public boolean isAutopullError() throws DAOException {
        List<ViewerMessage> messages = DataManager.getInstance()
                .getDao()
                .getViewerMessages(0, 1, "lastUpdateTime", true,
                        Map.of("taskName", TaskType.PULL_THEME.name()));
        if (!messages.isEmpty()) {
            return messages.get(0).getMessageStatus() == MessageStatus.ERROR;
        }
        return false;
    }

    public LocalDateTime getLastAutopull() throws DAOException {

        return getLastSuccessfullTask().map(ViewerMessage::getLastUpdateTime).orElse(null);

    }

    public VersionInfo getLastVersionInfo() throws DAOException, IOException {
        return getLastSuccessfullTask()
                .map(m -> {
                    try {
                        return PullThemeHandler.getVersionInfo(m.getProperties().get(ViewerMessage.MESSAGE_PROPERTY_INFO),
                                m.getLastUpdateTime().format(getDateTimeFormatter()));
                    } catch (JDOMException e) {
                        logger.error("Error reading version info from message {}: {}", m, e.toString());
                        return null;
                    }
                })
                .orElse(this.getThemeVersion());
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(BeanUtils.getNavigationHelper().getDateTimePattern());
    }

    private Optional<ViewerMessage> getLastSuccessfullTask() throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getViewerMessages(0, 1, "lastUpdateTime", true,
                        Map.of("taskName", TaskType.PULL_THEME.name(), "messageStatus", MessageStatus.FINISH.name()))
                .stream()
                .findAny();
    }

    public String getThemeName() {
        return this.viewerThemeName;
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

    public void sendPullThemeFinished(String message) {
        updatePullThemeProgress("finished", Optional.ofNullable(message).filter(StringUtils::isNotBlank), 1.0f);
    }

    public void sendPullThemeError(String message) {
        updatePullThemeProgress("error", Optional.of(message).filter(StringUtils::isNotBlank), 0);
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
            if ("?".equals(info.buildDate)) {
                info = getVersionFromTomcatDirectory();
            }
            return info;
        }
        return getVersionFromTomcatDirectory();
    }

    public VersionInfo getVersionFromTomcatDirectory() throws IOException {
        String tomcatBase = System.getProperty("catalina.base");
        String relPath = String.format("wtpwebapps/goobi-viewer-theme-%s/META-INF/MANIFEST.MF", this.getThemeName());
        Path path = Path.of(tomcatBase, relPath);
        if (Files.exists(path)) {
            return VersionInfo.getFromManifest(Files.readString(path));
        }
        return new VersionInfo("goobi-viewer-theme-" + this.viewerThemeName, "unknown", "unknown", "unknown", "");
    }

    public static class VersionInfo {
        private final String applicationName;
        private final String buildDate;
        private final String gitRevision;
        private final String releaseVersion;
        private final String commitMessage;

        public VersionInfo(String applicationName, String buildDate, String gitRevision, String releaseVersion, String commitMessage) {
            this.applicationName = applicationName;
            this.buildDate = buildDate;
            this.gitRevision = gitRevision;
            this.releaseVersion = releaseVersion;
            this.commitMessage = commitMessage;
        }

        public static VersionInfo getFromManifest(String manifest) {
            return new VersionInfo(
                    getInfo("ApplicationName", manifest),
                    getInfo("Implementation-Build-Date", manifest),
                    getInfo("Implementation-Version", manifest),
                    getInfo("version", manifest), "");
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

        public String getCommitMessage() {
            return commitMessage;
        }

    }

}
