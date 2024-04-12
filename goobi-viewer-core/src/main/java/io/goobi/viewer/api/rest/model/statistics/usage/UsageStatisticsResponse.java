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
package io.goobi.viewer.api.rest.model.statistics.usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.statistics.usage.RequestTypeSummary;
import io.goobi.viewer.model.statistics.usage.StatisticsSummary;

public class UsageStatisticsResponse {

    private final StatisticsSummary summary;
    private final List<StatisticsSummary> values;

    /**
     * @param summary
     * @param values
     */
    public UsageStatisticsResponse(StatisticsSummary summary, List<StatisticsSummary> values) {
        super();
        this.summary = summary;
        this.values = values;
    }

    /**
     * @return the summary
     */
    public StatisticsSummary getSummary() {
        return summary;
    }

    /**
     * @return the values
     */
    public List<StatisticsSummary> getValues() {
        return values;
    }

    @JsonIgnore
    public String getAsCsv(Locale locale, String columnSeparator, String rowSeparator) {
        List<List<String>> lines = new ArrayList<>();

        //HEADER
        List<String> header = new ArrayList<>();
        header.add("type");
        header.add("query");
        header.add("date");
        summary.getTypes().keySet().stream().sorted().forEach(type -> {
            String typeLabel = ViewerResourceBundle.getTranslation(type.getLabel(), locale);
            header.add(typeLabel + " " + ViewerResourceBundle.getTranslation("statistics__total_requests", locale));
            header.add(typeLabel + " " + ViewerResourceBundle.getTranslation("statistics__unique_requests", locale));
        });
        lines.add(header);

        //SUMMARY
        List<String> summaryLine = createCsvLine(this.getSummary(), "summary");
        lines.add(summaryLine);

        for (StatisticsSummary item : this.getValues()) {
            List<String> itemLine = createCsvLine(item, "item");
            lines.add(itemLine);
        }

        StringBuilder sb = new StringBuilder();
        for (List<String> line : lines) {
            appendLine(sb, line, columnSeparator);
            sb.append(rowSeparator);
        }
        sb.delete(sb.length() - rowSeparator.length(), sb.length());
        return sb.toString();

    }

    StringBuilder appendLine(StringBuilder sb, List<String> cell, String separator) {
        for (String string : cell) {
            sb.append(string).append(separator);
        }
        sb.delete(sb.length() - separator.length(), sb.length());
        return sb;
    }

    private List<String> createCsvLine(StatisticsSummary summaryItem, String name) {
        List<String> summaryLine = new ArrayList<>();
        summaryLine.add(name);
        if (summaryItem.getInformation() != null) {
            summaryLine.add(summaryItem.getInformation().getQuery());
            summaryLine.add(summaryItem.getInformation().getDateString());
        } else {
            summaryLine.add("");
            summaryLine.add("");
        }
        summaryItem.getTypes().keySet().stream().sorted().forEach(type -> {
            RequestTypeSummary data = summaryItem.getTypes().get(type);
            summaryLine.add(Long.toString(data.getTotalRequests()));
            summaryLine.add(Long.toString(data.getUniqueRequests()));
        });
        return summaryLine;
    }

}
