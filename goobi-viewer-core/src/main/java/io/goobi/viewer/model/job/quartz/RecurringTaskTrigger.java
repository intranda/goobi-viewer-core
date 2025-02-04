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

import io.goobi.viewer.model.job.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recurring_task_trigger")
public class RecurringTaskTrigger {

    public RecurringTaskTrigger() {
        //empty
    }

    public RecurringTaskTrigger(TaskType type, String scheduleExpression) {
        this.taskType = type.name();
        this.scheduleExpression = scheduleExpression;
        status = TaskTriggerStatus.RUNNING;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "task_type")
    private String taskType;
    @Column(name = "schedule_expression")
    private String scheduleExpression;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskTriggerStatus status;
    @Column(name = "laste_time_triggered")
    private LocalDateTime lastTimeTriggered;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }

    public TaskTriggerStatus getStatus() {
        return status;
    }

    public void setStatus(TaskTriggerStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastTimeTriggered() {
        return lastTimeTriggered;
    }

    public void setLastTimeTriggered(LocalDateTime lastTimeTriggered) {
        this.lastTimeTriggered = lastTimeTriggered;
    }

    @Override
    public String toString() {
        return "Recurring Task " + getTaskType() + " (" + getScheduleExpression() + ")";
    }

}
