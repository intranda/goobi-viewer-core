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
package io.goobi.viewer.model.iiif.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.oa.OpenAnnotation;

/**
 * An ALTO search hit annotation within a handwritten annotation block. Besides the regular annotation properties it carries the block's layout
 * tag LABEL and the configured style class, so clients can render annotation hits differently from hits in the printed text. These are
 * viewer-specific extension properties; other IIIF clients ignore them.
 *
 * <p>
 * Equality is inherited (class and id): the extra properties derive from the annotated ALTO element, so they are equal whenever the ids are.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayoutTagAnnotation extends OpenAnnotation {

    private final String layoutTag;
    private final String styleClass;

    /**
     * Creates a copy of the given annotation, extended by the layout tag of its annotation block.
     *
     * @param source the plain ALTO annotation to copy id, motivation, target, body and rights from
     * @param layoutTag LABEL of the annotation block's layout tag
     * @param styleClass configured CSS style class of the layout tag; may be null
     */
    public LayoutTagAnnotation(AbstractAnnotation source, String layoutTag, String styleClass) {
        super(source.getId());
        setMotivation(source.getMotivation());
        setTarget(source.getTarget());
        setBody(source.getBody());
        setRights(source.getRights());
        this.layoutTag = layoutTag;
        this.styleClass = styleClass;
    }

    /**
     * @return LABEL of the annotation block's layout tag
     */
    @JsonProperty("layoutTag")
    public String getLayoutTag() {
        return layoutTag;
    }

    /**
     * @return configured CSS style class of the layout tag; null if none is configured
     */
    @JsonProperty("styleClass")
    public String getStyleClass() {
        return styleClass;
    }
}
