package io.goobi.viewer.model.job.quartz;

import java.util.Date;

import org.quartz.Job;

public class QuartzJobDetails {

    private String jobName;
    private String jobGroup;

    private Date previousFireTime;
    private Date nextFireTime;

    private Class<? extends Job> clazz;


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
    public Date getPreviousFireTime() {
        return previousFireTime;
    }
    public void setPreviousFireTime(Date previousFireTime) {
        this.previousFireTime = previousFireTime;
    }
    public Date getNextFireTime() {
        return nextFireTime;
    }
    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }
    public Class<? extends Job> getClazz() {
        return clazz;
    }
    public void setClazz(Class<? extends Job> clazz) {
        this.clazz = clazz;
    }




}
