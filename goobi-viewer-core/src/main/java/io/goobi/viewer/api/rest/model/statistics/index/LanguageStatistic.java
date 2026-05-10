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
package io.goobi.viewer.api.rest.model.statistics.index;

/**
 * One slice of the languages chart.
 *
 * <p>
 * {@code code} is the raw Solr token (typically an ISO 639-1 / 639-3 code like {@code "en"} or {@code "fra"});
 * {@code label} is the locale-translated display value used by the pie tooltip. Translation lookup uses the bare
 * code as resource-bundle key, matching the existing message convention ({@code en=English}).
 * </p>
 */
public record LanguageStatistic(String code, String label, long count) {
}
