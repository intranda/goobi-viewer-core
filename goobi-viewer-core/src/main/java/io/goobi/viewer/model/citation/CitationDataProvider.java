/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.citation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class CitationDataProvider implements ItemDataProvider {

    public static final String AUTHOR = "author";
    public static final String DOI = "DOI";
    public static final String ISBN = "ISBN";
    public static final String ISSN = "ISSN";
    public static final String ISSUED = "issued";
    public static final String LANGUAGE = "language";
    public static final String PUBLISHER_PLACE = "placepublish";
    public static final String PUBLISHER = "publisher";
    public static final String TITLE = "title";

    private final Map<String, CSLItemData> itemDataMap = new TreeMap<>();

    /**
     * 
     * @param id
     * @param fields
     * @param type
     * @return Created CSLItemData
     * @should add item data correctly
     */
    public CSLItemData addItemData(String id, Map<String, List<String>> fields, CSLType type) {
        CSLItemDataBuilder builder = new CSLItemDataBuilder().type(type).id(id);

        for (String key : fields.keySet()) {
            if (fields.get(key) == null || fields.get(key).isEmpty()) {
                continue;
            }

            switch (key) {
                case AUTHOR:
                    List<CSLName> names = new ArrayList<>(fields.get(key).size());
                    for (String name : fields.get(key)) {
                        if (StringUtils.isBlank(name)) {
                            continue;
                        }
                        if (name.contains(",")) {
                            String[] nameSplit = name.split(",");
                            if (nameSplit.length > 1) {
                                names.add(new CSLNameBuilder().given(nameSplit[1].trim()).family(nameSplit[0].trim()).build());
                            } else {
                                //                                builder.author("", nameSplit[0].trim());
                            }
                        } else {
                            //                            String[] nameSplit = name.split(" ");
                            //                            if (nameSplit.length > 1) {
                            //                                names.add(new CSLNameBuilder().given(nameSplit[0].trim()).family(nameSplit[1].trim()).build());
                            //                            } else {
                            //                                builder.author("", name);
                            //                            }
                        }
                    }
                    if (!names.isEmpty()) {
                        builder.author(names.toArray(new CSLName[0]));
                    }
                    break;
                case DOI:

                    builder.DOI(fields.get(key).get(0));
                    break;
                case ISBN:
                    builder.ISBN(fields.get(key).get(0));
                    break;
                case ISSN:
                    builder.ISSN(fields.get(key).get(0));
                    break;
                case ISSUED:
                    builder.issued(new CSLDateBuilder().raw(fields.get(key).get(0)).build());
                    break;
                case LANGUAGE:
                    builder.language(fields.get(key).get(0));
                    break;
                case PUBLISHER:
                    builder.publisher(fields.get(key).get(0));
                    break;
                case PUBLISHER_PLACE:
                    builder.publisherPlace(fields.get(key).get(0));
                    break;
                case TITLE:
                    builder.title(fields.get(key).get(0));
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
    public String[] getIds() {
        return itemDataMap.keySet().toArray(new String[0]);
    }

}
