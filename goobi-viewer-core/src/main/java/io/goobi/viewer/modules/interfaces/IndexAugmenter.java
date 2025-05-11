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
package io.goobi.viewer.modules.interfaces;

import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexAugmenterException;

/**
 * @author florian
 *
 */
public interface IndexAugmenter {

    /**
     * Any additional tasks this module needs to perform when re-indexing a record (e.g. putting additional files into the hotfolder).
     *
     * @param pi a {@link java.lang.String} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @throws IndexAugmenterException
     */
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws IndexAugmenterException;

    /**
     * Any additional tasks this module needs to perform when re-indexing a page (e.g. putting additional files into the hotfolder).
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @return true if successful; false otherwise
     * @throws IndexAugmenterException
     */
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String dataRepository, String namingScheme)
            throws IndexAugmenterException;
}
