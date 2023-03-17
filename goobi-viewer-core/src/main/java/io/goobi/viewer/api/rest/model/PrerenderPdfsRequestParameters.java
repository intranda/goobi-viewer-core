package io.goobi.viewer.api.rest.model;

import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name="PrerenderPdfsRequestParameters", description="additional parameters to prerender single page pdfs", requiredProperties= {"pi"})
public class PrerenderPdfsRequestParameters extends TaskParameter {

    @Schema(description = "Record persistent identifier", example="PPN12345")
    public String pi;
    @Schema(description = "ContentServer config variant to use when creating the pdfs", example="default")
    public String variant;
    @Schema(description = "Set to true if existing pdf files should be overwritten", example="false")
    public Boolean force;
    
}
