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
package io.goobi.viewer.model.citation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.controller.DateTools;

/**
 * Implements the CSL {@link de.undercouch.citeproc.ItemDataProvider} interface by mapping Goobi
 * Viewer metadata fields to CSL item data for citation formatting.
 */
public class CitationDataProvider implements ItemDataProvider {

    public static final String AUTHOR = "author";
    public static final String COLLECTION_TITLE = "collection-title";
    public static final String COMPOSER = "composer";
    public static final String CONTAINER_TITLE = "container-title";
    public static final String DIRECTOR = "director";
    public static final String DOI = "DOI";
    public static final String EDITOR = "editor";
    public static final String ILLUSTRATOR = "illustrator";
    public static final String INTERVIEWER = "interviewer";
    public static final String ISBN = "ISBN";
    public static final String ISSN = "ISSN";
    public static final String ISSUED = "issued";
    public static final String LANGUAGE = "language";
    public static final String PUBLISHER_PLACE = "placepublish";
    public static final String PUBLISHER = "publisher";
    public static final String RECIPIENT = "recipient";
    public static final String SCALE = "scale";
    public static final String TITLE = "title";
    public static final String TRANSLATOR = "translator";
    public static final String URL = "url";

    // Matches ISO-like date strings (year, year-month, year-month-day) used to detect malformed
    // numeric dates such as "1910-00-00" that would otherwise be rejected by citeproc's date parser.
    private static final Pattern ISO_LIKE_DATE = Pattern.compile("^(\\d{4})(?:-\\d{1,2}){0,2}$");

    private final Map<String, CSLItemData> itemDataMap = new TreeMap<>();

    /**
     *
     * @param id citation item identifier
     * @param fields map of metadata field names to their values
     * @param type CSL document type for this citation
     * @return Created CSLItemData
     * @should store author name parts, issued date, URL, and ISBN in CSLItemData
     * @should parse year-only issued date into dateParts instead of raw string
     * @should fall back to leading year when issued date has zero month or day
     */
    public CSLItemData addItemData(String id, Map<String, List<String>> fields, CSLType type) {
        CSLItemDataBuilder builder = new CSLItemDataBuilder().type(type).id(id);

        for (Entry<String, List<String>> entry : fields.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            switch (entry.getKey()) {
                // Persons
                case AUTHOR:
                case COMPOSER:
                case DIRECTOR:
                case EDITOR:
                case ILLUSTRATOR:
                case INTERVIEWER:
                case RECIPIENT:
                case TRANSLATOR:
                    List<CSLName> names = new ArrayList<>(entry.getValue().size());
                    for (String name : entry.getValue()) {
                        if (StringUtils.isBlank(name)) {
                            continue;
                        }
                        if (name.contains(",")) {
                            String[] nameSplit = name.split(",");
                            if (nameSplit.length > 1) {
                                names.add(new CSLNameBuilder().given(nameSplit[1].trim()).family(nameSplit[0].trim()).build());
                            }
                        }
                    }
                    if (!names.isEmpty()) {
                        switch (entry.getKey()) {
                            case AUTHOR:
                                builder.author(names.toArray(new CSLName[0]));
                                break;
                            case COMPOSER:
                                builder.composer(names.toArray(new CSLName[0]));
                                break;
                            case DIRECTOR:
                                builder.director(names.toArray(new CSLName[0]));
                                break;
                            case EDITOR:
                                builder.editor(names.toArray(new CSLName[0]));
                                break;
                            case ILLUSTRATOR:
                                builder.illustrator(names.toArray(new CSLName[0]));
                                break;
                            case INTERVIEWER:
                                builder.interviewer(names.toArray(new CSLName[0]));
                                break;
                            case RECIPIENT:
                                builder.recipient(names.toArray(new CSLName[0]));
                                break;
                            case TRANSLATOR:
                                builder.translator(names.toArray(new CSLName[0]));
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                case COLLECTION_TITLE:
                    builder.collectionTitle(entry.getValue().get(0));
                    break;
                case CONTAINER_TITLE:
                    builder.containerTitle(entry.getValue().get(0));
                    break;
                case DOI:
                    builder.DOI(entry.getValue().get(0));
                    break;
                case ISBN:
                    builder.ISBN(entry.getValue().get(0));
                    break;
                case ISSN:
                    builder.ISSN(entry.getValue().get(0));
                    break;
                case ISSUED:
                    // Use different method for year-only values (to avoid duplicates in APA6)
                    String issuedValue = entry.getValue().get(0);
                    try {
                        DateTools.FORMATTERYEARONLY.parse(issuedValue);
                        builder.issued(Integer.valueOf(issuedValue));
                    } catch (DateTimeParseException e) {
                        // Some sources deliver ISO-like dates with zero month/day (e.g. "1910-00-00"),
                        // which citeproc rejects with DateTimeException ("Invalid value for MonthOfYear ... 0"),
                        // filling the error log and rendering a red error instead of the citation.
                        // If the value looks ISO-like but is not a valid LocalDate, fall back to the leading
                        // year; only strings that do not look like ISO dates continue to use the raw path.
                        Integer fallbackYear = extractLeadingYearFromInvalidIsoDate(issuedValue);
                        if (fallbackYear != null) {
                            builder.issued(fallbackYear);
                        } else {
                            builder.issued(new CSLDateBuilder().raw(issuedValue).build());
                        }
                    }
                    break;
                case LANGUAGE:
                    builder.language(entry.getValue().get(0));
                    break;
                case PUBLISHER:
                    builder.publisher(entry.getValue().get(0));
                    break;
                case PUBLISHER_PLACE:
                    builder.publisherPlace(entry.getValue().get(0));
                    break;
                case SCALE:
                    builder.scale(entry.getValue().get(0));
                    break;
                case TITLE:
                    builder.title(entry.getValue().get(0));
                    break;
                case URL:
                    builder.URL(entry.getValue().get(0));
                    break;
                default:
                    break;
            }
        }

        CSLItemData item = builder.build();
        itemDataMap.put(id, item);

        return item;
    }

    /**
     * Returns the 4-digit leading year of an ISO-like date string whose full value cannot be parsed
     * as a valid {@link LocalDate} (e.g. "1910-00-00", "1910-00", "1910-04"). Returns {@code null} for
     * valid ISO dates (so the caller preserves the full raw value) and for strings that do not look
     * ISO-like at all (so free-form dates continue to use the raw path).
     *
     * @param value metadata value from the issued field
     * @return leading year as Integer, or null if no fallback should be applied
     * @should return year when value has zero month and day
     * @should return null for valid iso date
     * @should return null for non iso string
     */
    static Integer extractLeadingYearFromInvalidIsoDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        Matcher m = ISO_LIKE_DATE.matcher(trimmed);
        if (!m.matches()) {
            return null;
        }
        try {
            LocalDate.parse(trimmed);
            // Valid ISO date — keep raw behavior intact so citeproc can format the full date.
            return null;
        } catch (DateTimeParseException e) {
            try {
                return Integer.valueOf(m.group(1));
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    @Override
    public CSLItemData retrieveItem(String id) {
        return itemDataMap.get(id);
    }

    @Override
    public Collection<String> getIds() {
        return itemDataMap.keySet();
    }

}