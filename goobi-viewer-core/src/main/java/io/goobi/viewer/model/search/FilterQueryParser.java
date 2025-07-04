package io.goobi.viewer.model.search;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.StringTools;
import jakarta.servlet.http.HttpServletRequest;

public class FilterQueryParser {

    private static final String FILTER_QUERY_REGEX = "[?&]?filterQuery=([^&]+)";

    public Optional<String> getFilterQuery(HttpServletRequest request) {
        if (request != null && StringUtils.isNotBlank(request.getQueryString())) {
            Matcher matcher = Pattern.compile(FILTER_QUERY_REGEX).matcher(request.getQueryString());
            if (matcher.find()) {
                return Optional.of(StringTools.decodeUrl(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

}
