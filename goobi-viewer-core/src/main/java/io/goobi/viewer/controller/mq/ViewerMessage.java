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
package io.goobi.viewer.controller.mq;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "mq_messages")
@JsonInclude(Include.NON_EMPTY)
public class ViewerMessage {

    public static final String MESSAGE_PROPERTY_ERROR = "error";
    public static final String MESSAGE_PROPERTY_INFO = "result";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "message_type", nullable = false)
    private String taskName;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mq_message_properties",
            joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "property_name")
    @Column(name = "property_value", nullable = true)
    @PrivateOwned
    private Map<String, String> properties = new HashMap<>();

    @Column(name = "message_status")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus = MessageStatus.NEW;

    @Column(name = "queue")
    private String queue;

    @Column(name = "retry_count")
    private int retryCount = 1;

    @Column(name = "max_retries")
    private int maxRetries = 10;

    @Column(name = "last_update_time")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime lastUpdateTime = LocalDateTime.now();

    @Column(name = "delay_millis")
    private long delay = 0L;

    public ViewerMessage() {

    }

    public ViewerMessage(String taskName) {
        this.taskName = taskName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public boolean isProcessing() {
        return MessageStatus.PROCESSING.equals(getMessageStatus());
    }

    public static ViewerMessage parseJSON(String json) throws JsonMappingException, JsonProcessingException {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .readValue(json, ViewerMessage.class);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Viewer Message of type ")
                .append(this.taskName)
                .append("\t")
                .append("Message id: ")
                .append(getMessageId())
                .append("\t")
                .append("Message status: ")
                .append(getMessageStatus())
                .toString();

    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Call this method to set the max number of allowed retries to the number of retries already done, effectively preventing any further retries.
     * This is useful if the ticket runs into an error which it cannot reasonably recover from, so further retries are futile
     */
    public void setDoNotRetry() {
        this.maxRetries = this.retryCount;
    }

    public boolean shouldRetry() {
        return this.retryCount < this.maxRetries;
    }

}
