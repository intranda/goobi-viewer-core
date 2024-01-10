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

import de.undercouch.citeproc.csl.CSLType;

public final class CitationTools {

    private CitationTools() {
    }

    /**
     *
     * @param docstruct
     * @param topstruct
     * @return CLSType for the given docstruct; default value if none mapped
     * @should return correct type
     */
    public static CSLType getCSLTypeForDocstrct(String docstruct, String topstruct) {
        if (docstruct == null) {
            return CSLType.ARTICLE;
        }

        switch (docstruct.toLowerCase()) {
            case "monograph":
            case "volume":
                return CSLType.BOOK;
            case "manuscript":
                return CSLType.MANUSCRIPT;
            case "map":
            case "singlemap":
                return CSLType.MAP;
            case "article":
                if (topstruct != null) {
                    if (topstruct.toLowerCase().startsWith("newspaper")) {
                        return CSLType.ARTICLE_NEWSPAPER;
                    }
                    if ("periodicalvolume".equalsIgnoreCase(topstruct)) {
                        return CSLType.ARTICLE_JOURNAL;
                    }
                }
                return CSLType.ARTICLE;
            case "chapter":
                return CSLType.CHAPTER;
            default:
                return CSLType.ARTICLE;
        }
    }
}
