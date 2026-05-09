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
package io.goobi.viewer.model.statistics.index;

// New exception type for #15809: lets statistics services signal "upstream is down AND no fresh
// cached snapshot is available" so the JAX-RS resource layer can translate to HTTP 503 instead of
// silently returning empty data (which would render as a blank chart).
/**
 * Thrown when a statistics aggregation cannot be served because Solr (or the underlying data source) is unreachable
 * AND no cached snapshot is available to fall back on. Translated to HTTP 503 by the resource layer.
 */
public class StatisticsUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public StatisticsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
