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
package de.intranda.digiverso.presentation.model.metadata;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareYearSolrDocWrapper implements Comparable<CompareYearSolrDocWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(CompareYearSolrDocWrapper.class);

    private SolrDocument solrDocument = null;
    private long year = 0;

    public CompareYearSolrDocWrapper(SolrDocument doc) {
        this.solrDocument = doc;
        if (doc.getFieldValue("YEAR") != null) {
            this.year = (Long) doc.getFirstValue("YEAR");
        } else {
            this.year = 0;
        }
    }

    @Override
    public int compareTo(CompareYearSolrDocWrapper comp) {
        int ret = 0;
        if (comp != null) {
            if (this.year == comp.getYear()) {
                ret = 0;
            } else if (this.year > comp.getYear()) {
                ret = 1;
            } else {
                ret = -1;
            }
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (year ^ year >>> 32);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompareYearSolrDocWrapper other = (CompareYearSolrDocWrapper) obj;
        if (year != other.year) {
            return false;
        }
        return true;
    }

    public SolrDocument getSolrDocument() {
        return solrDocument;
    }

    public void setSolrDocument(SolrDocument solrDocument) {
        this.solrDocument = solrDocument;
    }

    public long getYear() {
        return year;
    }

    public void setYear(long year) {
        this.year = year;
    }

}
