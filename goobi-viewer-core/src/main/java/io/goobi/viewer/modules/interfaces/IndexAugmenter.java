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
 * @author Florian Alpers
 */
public interface IndexAugmenter {

    /**
     * Any additional tasks this module needs to perform when re-indexing a record (e.g. putting additional files into the hotfolder).
     *
     * @param pi persistent identifier of the record to re-index
     * @param dataRepository data repository name containing the record
     * @param namingScheme naming scheme used for hotfolder file names
     * @throws IndexAugmenterException
     */
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws IndexAugmenterException;

    /**
     * Any additional tasks this module needs to perform when re-indexing a page (e.g. putting additional files into the hotfolder).
     *
     * @param pi persistent identifier of the record containing the page
     * @param page page order number within the record
     * @param doc Solr document representing the page
     * @param dataRepository data repository name containing the record
     * @param namingScheme naming scheme used for hotfolder file names
     * @return true if successful; false otherwise
     * @throws IndexAugmenterException
     */
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String dataRepository, String namingScheme)
            throws IndexAugmenterException;
}
