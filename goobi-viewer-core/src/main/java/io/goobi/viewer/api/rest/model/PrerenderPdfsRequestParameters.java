package io.goobi.viewer.api.rest.model;

import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrerenderPdfsRequestParameters", description = "additional parameters to prerender single page pdfs", requiredProperties = { "pi" })
public class PrerenderPdfsRequestParameters extends TaskParameter {

    @Schema(description = "Record persistent identifier", example = "PPN12345")
    private String pi;
    @Schema(description = "ContentServer config variant to use when creating the pdfs", example = "default")
    private String variant;
    @Schema(description = "Set to true if existing pdf files should be overwritten", example = "false")
    private Boolean force;

    public String getPi() {
        return pi;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

}
