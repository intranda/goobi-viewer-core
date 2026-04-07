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
package io.goobi.viewer.model.metadata;

import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * CompareYearSolrDocWrapper class.
 */
public class CompareYearSolrDocWrapper implements Comparable<CompareYearSolrDocWrapper> {

    private static final Logger logger = LogManager.getLogger(CompareYearSolrDocWrapper.class);

    private SolrDocument solrDocument = null;
    private long year = 0;

    /**
     * Creates a new CompareYearSolrDocWrapper instance.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     */
    public CompareYearSolrDocWrapper(SolrDocument doc) {
        this.solrDocument = doc;
        if (doc.getFieldValue("YEAR") != null) {
            this.year = (Long) doc.getFirstValue("YEAR");
        } else {
            this.year = 0;
        }
    }

    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
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

        return year == other.year;
    }

    /**
     * Getter for the field <code>solrDocument</code>.
     *
     * @return a {@link org.apache.solr.common.SolrDocument} object.
     */
    public SolrDocument getSolrDocument() {
        return solrDocument;
    }

    /**
     * Setter for the field <code>solrDocument</code>.
     *
     * @param solrDocument a {@link org.apache.solr.common.SolrDocument} object.
     */
    public void setSolrDocument(SolrDocument solrDocument) {
        this.solrDocument = solrDocument;
    }

    /**
     * Getter for the field <code>year</code>.
     *
     * @return a long.
     */
    public long getYear() {
        return year;
    }

    /**
     * Setter for the field <code>year</code>.
     *
     * @param year a long.
     */
    public void setYear(long year) {
        this.year = year;
    }

}
