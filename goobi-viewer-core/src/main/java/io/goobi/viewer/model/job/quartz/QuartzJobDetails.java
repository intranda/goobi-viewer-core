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

package io.goobi.viewer.model.job.quartz;

import java.time.LocalDateTime;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobKey;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import it.burning.cron.CronExpressionDescriptor;
import it.burning.cron.CronExpressionParser.Options;

public class QuartzJobDetails {

    private String jobName;
    private String jobGroup;

    private LocalDateTime previousFireTime;
    private LocalDateTime nextFireTime;

    private JobKey jobKey;

    private boolean paused;

    private String cronExpression;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public LocalDateTime getPreviousFireTime() {
        return previousFireTime;
    }

    public void setPreviousFireTime(LocalDateTime previousFireTime) {
        this.previousFireTime = previousFireTime;
    }

    public LocalDateTime getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(LocalDateTime nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public void setJobKey(JobKey jobKey) {
        this.jobKey = jobKey;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getHumanReadableCronTime() {
        if (StringUtils.isBlank(cronExpression)) {
            return "";
        }
        Options options = new Options(false, true, true, false, BeanUtils.getLocale());
        return CronExpressionDescriptor.getDescription(cronExpression, options);
    }

}
