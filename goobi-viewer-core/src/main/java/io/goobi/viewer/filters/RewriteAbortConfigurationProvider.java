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
package io.goobi.viewer.filters;

import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Direction;
import org.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import org.ocpsoft.rewrite.servlet.config.Lifecycle;
import org.ocpsoft.rewrite.servlet.config.Response;

import jakarta.servlet.ServletContext;

/**
 * Aborts the rewrite/PrettyFaces lifecycle as soon as the response has been committed by an upstream filter or servlet.
 *
 * <p>Without this rule, {@link org.ocpsoft.rewrite.servlet.RewriteFilter} logs
 * "Response has already been committed, and further write operations are not permitted" before still calling
 * {@code chain.doFilter()} on the committed response. The downstream JSF lifecycle then triggers
 * {@code com.sun.faces.context.ExternalContextImpl.setResponseBufferSize(int)} which Tomcat rejects with
 * "Skipping attempt to set buffer size on a committed response".
 *
 * <p>The rule is the exact mitigation suggested in the rewrite warning text and runs with the highest
 * possible priority so it fires before any other rewrite/PrettyFaces rule when the response is already committed.
 */
public class RewriteAbortConfigurationProvider extends HttpConfigurationProvider {

    /**
     * @return {@link Integer#MAX_VALUE} so the abort rule fires before any other rewrite/PrettyFaces rule
     * @should return Integer MAX_VALUE
     */
    @Override
    public int priority() {
        // Run before all other rewrite/PrettyFaces rules so an already-committed
        // response short-circuits the lifecycle instead of being passed further down.
        return Integer.MAX_VALUE;
    }

    /**
     * Builds the rewrite configuration containing a single inbound rule that calls {@link Lifecycle#abort()}
     * when {@link Response#isCommitted()} matches.
     *
     * @param context active servlet context, may be null in unit tests
     * @return non-null configuration with at least one rule
     * @should return non null configuration with at least one rule
     */
    @Override
    public Configuration getConfiguration(ServletContext context) {
        return ConfigurationBuilder.begin()
                .addRule()
                .when(Direction.isInbound().and(Response.isCommitted()))
                .perform(Lifecycle.abort());
    }
}
