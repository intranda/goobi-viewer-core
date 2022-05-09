package io.goobi.viewer.model.job.upload;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "title", "value" })
public class PropertyResponse {
    private String title;
    private String value;

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     * @return this;
     */
    public PropertyResponse setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     * @return this
     */
    public PropertyResponse setValue(String value) {
        this.value = value;
        return this;
    }
}
