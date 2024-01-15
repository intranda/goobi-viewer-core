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

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.controller.DateTools;

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

    private final Map<String, CSLItemData> itemDataMap = new TreeMap<>();

    /**
     *
     * @param id
     * @param fields
     * @param type
     * @return Created CSLItemData
     * @should add item data correctly
     * @should parse years correctly
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
                    try {
                        DateTools.formatterYearOnly.parse(entry.getValue().get(0));
                        builder.issued(Integer.valueOf(entry.getValue().get(0)));
                    } catch (DateTimeParseException e) {
                        builder.issued(new CSLDateBuilder().raw(entry.getValue().get(0)).build());
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

    /* (non-Javadoc)
     * @see de.undercouch.citeproc.ItemDataProvider#retrieveItem(java.lang.String)
     */
    @Override
    public CSLItemData retrieveItem(String id) {
        return itemDataMap.get(id);
    }

    /* (non-Javadoc)
     * @see de.undercouch.citeproc.ItemDataProvider#getIds()
     */
    @Override
    public Collection<String> getIds() {
        return itemDataMap.keySet();
    }

}
