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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.weld.exceptions.IllegalArgumentException;

import de.undercouch.citeproc.CSL;

public class CitationProcessorWrapper implements Serializable {

    private static final long serialVersionUID = 1599943007739055358L;

    /** Processors for each used style. */
    private final Map<String, CSL> citationProcessors = new ConcurrentHashMap<>();

    /** Single item data provider . */
    private final CitationDataProvider citationItemDataProvider = new CitationDataProvider();

    /**
     * 
     * @param style
     * @return the citationProcessor
     * @throws IOException
     * @should create citation processor correctly
     */
    public CSL getCitationProcessor(String style) throws IOException {
        if (style == null) {
            throw new IllegalArgumentException("style may not be null");
        }

        if (citationProcessors.get(style) == null) {
            CSL citationProcessor = new CSL(citationItemDataProvider, style);
            citationProcessors.put(style, citationProcessor);
        }

        return citationProcessors.get(style);
    }

    /**
     * @return the citationItemDataProvider
     */
    public CitationDataProvider getCitationItemDataProvider() {
        return citationItemDataProvider;
    }
}
