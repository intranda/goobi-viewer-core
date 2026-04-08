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
package io.goobi.viewer.model.archives;

import java.io.Serializable;
import java.util.Locale;

import org.jboss.weld.exceptions.IllegalArgumentException;

import io.goobi.viewer.model.cms.CMSArchiveConfig;

/**
 * Wrapper class for archive and configuration pairs.
 */
public class ArchiveResourceWrapper implements Serializable {

    private static final long serialVersionUID = 8240190195476685234L;

    private final ArchiveResource archiveResource;
    private CMSArchiveConfig archiveConfig;

    public ArchiveResourceWrapper(ArchiveResource archiveResource) {
        if (archiveResource == null) {
            throw new IllegalArgumentException("archiveResource may not be null");
        }
        this.archiveResource = archiveResource;
    }

    /**
     * 
     * @param locale locale for which to retrieve the label
     * @return Configured title of the given locale; resource name if no configured title found
     */
    public String getLabel(Locale locale) {
        if (archiveConfig != null && archiveConfig.getTitle() != null) {
            String title = archiveConfig.getTitle().getText(locale);
            if (title != null && !title.isEmpty()) {
                return title;
            }
        }

        return archiveResource.getResourceName();
    }

    /**
     * @return the archiveResource
     */
    public ArchiveResource getArchiveResource() {
        return archiveResource;
    }

    /**
     * @return the archiveConfig
     */
    public CMSArchiveConfig getArchiveConfig() {
        return archiveConfig;
    }

    /**
     * @param archiveConfig the archiveConfig to set
     */
    public void setArchiveConfig(CMSArchiveConfig archiveConfig) {
        this.archiveConfig = archiveConfig;
    }
}
