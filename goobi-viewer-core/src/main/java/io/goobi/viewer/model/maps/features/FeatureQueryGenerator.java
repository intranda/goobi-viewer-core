package io.goobi.viewer.model.maps.features;

import java.net.URI;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.ws.rs.core.UriBuilder;

public class FeatureQueryGenerator {

    public URI createSearchLink(MetadataContainer metadata, String template, LabelCreator labelCreator) {

        UriBuilder searchUrl = UriBuilder.fromUri(PrettyUrlTools.getAbsolutePageUrl("newSearch1", "-"));

        String longLat = metadata.getFirstValue("WKT_COORDS");
        if (StringUtils.isNotBlank(longLat)) {
            String query = StringTools.encodeUrl(createSearchTerm(metadata, template, labelCreator));
            String locationQuery = StringTools.encodeUrl("WKT_COORDS:\"Intersects(POINT(%s)) distErrPct=0\"".formatted(longLat));
            searchUrl.queryParam("filterQuery", "(%s) AND (%s)".formatted(query, locationQuery));
        }
        return searchUrl.build();
    }

    private String createSearchTerm(MetadataContainer metadata, String template, LabelCreator labelCreator) {

        String docType = metadata.getFirstValue(SolrConstants.DOCTYPE);

        switch (docType) {
            case "METADATA":
                if (!template.equals(StringConstants.DEFAULT_NAME)) {
                    return "LABEL:(%s %s_LANG_*)".formatted(template, template);
                } else {
                    String definedTemplates =
                            labelCreator.getTemplateNames().stream().map(t -> "%s %s_LANG_*".formatted(t, t)).collect(Collectors.joining(" "));
                    return "-LABEL:(%s)".formatted(definedTemplates);
                }
            case "DOCSTRCT":
            default:
                if (!template.equals(StringConstants.DEFAULT_NAME)) {
                    return "DOCSTRCT:%s".formatted(template);
                } else {
                    return "-DOCSTRCT:%s".formatted(labelCreator.getTemplateNames().stream().collect(Collectors.joining(" ")));
                }
        }
    }

}
