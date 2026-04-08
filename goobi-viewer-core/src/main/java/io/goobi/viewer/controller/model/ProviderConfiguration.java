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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.variables.NoopVariableReplacer;
import io.goobi.viewer.model.variables.VariableReplacer;

/**
 * Bundles provider configurations for IIIF manifests into a single object.
 *
 * @author Florian Alpers
 */
public class ProviderConfiguration {

    private final URI uri;
    private final String label;
    private final List<URI> logos = new ArrayList<>();
    private final List<WebResourceConfiguration> homepages = new ArrayList<>();

    /**
     * 
     * @param uri URI of the provider
     * @param label display label of the provider
     * @throws PresentationException
     */
    public ProviderConfiguration(String uri, String label) throws PresentationException {
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
     * @param config XML configuration node for the provider
     * @throws PresentationException
     */
    public ProviderConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws PresentationException {
        this(config, new NoopVariableReplacer());
    }

    /**
     * 
     * @param config XML configuration node for the provider
     * @param vr variable replacer for resolving placeholders in config values
     * @throws PresentationException
     */
    public ProviderConfiguration(HierarchicalConfiguration<ImmutableNode> config, VariableReplacer vr) throws PresentationException {
        this(vr.replaceFirst(config.getString("url", "")), vr.replaceFirst(config.getString("label", null)));

        List<Object> logos = config.getList("logo");
        logos.forEach(logo -> {
            this.logos.add(URI.create(vr.replaceFirst(logo.toString())));
        });

        List<HierarchicalConfiguration<ImmutableNode>> hp = config.configurationsAt("homepage");
        for (HierarchicalConfiguration<ImmutableNode> homepage : hp) {
            this.homepages.add(new WebResourceConfiguration(homepage, vr));
        }

    }

    /**

     */
    public URI getUri() {
        return uri;
    }

    /**

     */
    public String getLabel() {
        return label;
    }

    /**

     */
    public List<URI> getLogos() {
        return logos;
    }

    /**

     */
    public List<WebResourceConfiguration> getHomepages() {
        return homepages;
    }
}
