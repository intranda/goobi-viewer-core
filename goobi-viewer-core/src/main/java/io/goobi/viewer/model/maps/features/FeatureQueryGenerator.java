package io.goobi.viewer.model.maps.features;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.solr.SolrConstants;

public class FeatureQueryGenerator {

    public String createSearchFilterQuery(MetadataContainer metadata, String template, LabelCreator labelCreator) {

        String longLat = metadata.getFirstValue("WKT_COORDS");
        if (StringUtils.isNotBlank(longLat)) {
            String query = StringTools.encodeUrl(createSearchTerm(metadata, template, labelCreator));
            String locationQuery = "WKT_COORDS:\"Intersects(POINT(%s)) distErrPct=0\"".formatted(longLat);
            //            if (StringUtils.isNotBlank(query)) {
            //                return StringTools.encodeUrl("(%s) AND (%s)".formatted(query, locationQuery));
            //            } else {
            return StringTools.encodeUrl(locationQuery);
            //            }
        }
        return "";
    }

    private String createSearchTerm(MetadataContainer metadata, String template, LabelCreator labelCreator) {

        String docType = metadata.getFirstValue(SolrConstants.DOCTYPE);
        List<String> templateNames = labelCreator.getTemplateNames()
                .stream()
                .filter(t -> !t.equals(StringConstants.DEFAULT_NAME))
                .toList();

        switch (docType) {
            case "METADATA":
                if (!template.equals(StringConstants.DEFAULT_NAME) && templateNames.contains(template)) {
                    return "LABEL:(%s %s_LANG_*)".formatted(template, template);
                } else {
                    String definedTemplates = templateNames.stream()
                            .map(t -> "%s %s_LANG_*".formatted(t, t))
                            .collect(Collectors.joining(" "));
                    if (StringUtils.isNotBlank(definedTemplates)) {
                        return "-LABEL:(%s)".formatted(definedTemplates);
                    } else {
                        return "";
                    }
                }
            case "DOCSTRCT":
            default:
                if (!template.equals(StringConstants.DEFAULT_NAME) && templateNames.contains(template)) {
                    return "DOCSTRCT:%s".formatted(template);
                } else {
                    String definedTemplates = templateNames.stream()
                            .collect(Collectors.joining(" "));
                    if (StringUtils.isNotBlank(definedTemplates)) {
                        return "-DOCSTRCT:(%s)".formatted(definedTemplates);
                    } else {
                        return "";
                    }
                }
        }
    }

}
