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

/**
 * This class bundles provider configurations for IIIF manifests int a s single object
 *
 * @author florian
 *
 */
public class ProviderConfiguration {

    public final URI uri;
    public final String label;
    public final List<URI> logos = new ArrayList<>();
    public final List<WebResourceConfiguration> homepages = new ArrayList<>();

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

    public ProviderConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws PresentationException {
        this(config.getString("url", null), config.getString("label", null));

        List<Object> logos = config.getList("logo");
        logos.forEach(logo -> {
            this.logos.add(URI.create(logo.toString()));
        });

        List<HierarchicalConfiguration<ImmutableNode>> homepages = config.configurationsAt("homepage");
        for (HierarchicalConfiguration<ImmutableNode> homepage : homepages) {
            this.homepages.add(new WebResourceConfiguration(homepage));
        }

    }

}
