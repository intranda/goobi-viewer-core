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
 * One bucket of the publication-centuries histogram.
 *
 * <p>
 * {@code century=18} stands for years 1800–1899, {@code century=20} stands for years 2000–2099, etc. Pre-modern
 * centuries (negative values) are dropped by the service to avoid the grammatically odd "-1. Jh." UI label.
 * </p>
 */
public record PublicationCenturyStatistic(int century, long count) {
}
