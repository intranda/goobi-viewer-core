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
package io.goobi.viewer.controller.model;

import java.net.URI;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.variables.NoopVariableReplacer;
import io.goobi.viewer.model.variables.VariableReplacer;

/**
 * Bundles configuration for a web resource containing a URI and a label
 *
 * @author florian
 *
 */
public class WebResourceConfiguration {

    private final URI uri;
    private final String label;

    /**
     * 
     * @param uri
     * @param label
     * @throws PresentationException
     */
    public WebResourceConfiguration(String uri, String label) throws PresentationException {
        if (uri == null) {
            throw new PresentationException("URI must be provided");
        }
        this.uri = URI.create(uri);
        if (StringUtils.isBlank(label)) {
            throw new PresentationException("Label must be provided");
        }
        this.label = label;
    }

    /**
     * 
     * @param config
     * @throws PresentationException
     */
    public WebResourceConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws PresentationException {
        this(config, new NoopVariableReplacer());
    }

    /**
     * 
     * @param config
     * @param vr
     * @throws PresentationException
     */
    public WebResourceConfiguration(HierarchicalConfiguration<ImmutableNode> config, VariableReplacer vr) throws PresentationException {
        this(vr.replaceFirst(config.getString("url", null)), vr.replaceFirst(config.getString("label", null)));
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
}
