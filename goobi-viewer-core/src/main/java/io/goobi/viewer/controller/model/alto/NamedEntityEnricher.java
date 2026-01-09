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

package io.goobi.viewer.controller.model.alto;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoTags;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;

public class NamedEntityEnricher implements TextEnricher {

    private static final int MAX_ENRICHMENTS = 1;

    private static final String CONTENT_TEMPLATE =
            "<button class=\"view-fulltext__entity-action-button\" type=\"button\" data-entity-id=\"{tagRef}\" data-entity-type=\"{tagType}\""
                    + " data-entity-authority-data-uri=\"{tagRestUri}\" data-entity-authority-data-search=\"{tagSearchUri}\">{tagLabel}</button>";
    private static final String TAG_RESTURI_TEMPATE = "{restUri}authority/resolver?id={tagUri}&amp;=de";

    private final String restUri = DataManager.getInstance().getConfiguration().getRestApiUrl();

    private int numEnrichments = 0;

    @Override
    public String enrich(String content, LineElement element) {

        if (numEnrichments < MAX_ENRICHMENTS) {

            Page page = element.getPage();
            AltoTags tags = page.getDocument().getTags();
            List<Tag> referencingTags = tags.getTagsAsList().stream().filter(tag -> tag.getReferences().contains(element)).toList();
            if (!referencingTags.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Tag tag : referencingTags) {
                    String tagRef = Optional.ofNullable(tag.getId()).orElse("");
                    String tagType = Optional.ofNullable(tag.getType()).map(String::toLowerCase).orElse("");
                    String tagLabel = StringUtils.isNotBlank(tag.getLabel()) ? tag.getLabel() : content;
                    String tagUri = Optional.ofNullable(tag.getUri()).orElse("");

                    String tagRestUri = TAG_RESTURI_TEMPATE.replace("{restUri}", this.restUri).replace("{tagUri}", tagUri);
                    String searchString = "%s:%%22%s%%22".formatted(NormDataImporter.FIELD_IDENTIFIER,
                            tagUri.replaceAll("^https?:\\/\\/d-nb.info\\/gnd\\/([\\d-]+)\\/?$", "$1"));
                    String tagSearchUri = getSearchPageUrl(searchString);

                    String enriched = CONTENT_TEMPLATE.replace("{tagRef}", tagRef)
                            .replace("{tagType}", tagType)
                            .replace("{tagRestUri}", tagRestUri)
                            .replace("{tagSearchUri}", tagSearchUri)
                            .replace("{tagLabel}", tagLabel);

                    sb.append(enriched);
                }
                numEnrichments++;
                return sb.toString();
            }
        }
        return content;
    }

    String getSearchPageUrl(String searchString) {
        try {
            String tagSearchUri = PrettyUrlTools.getAbsolutePageUrl("search6", "-", searchString, "1", "-", "-");
            return tagSearchUri;
        } catch (IllegalStateException e) {
            return "";
        }
    }

}
