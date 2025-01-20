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
package io.goobi.viewer.faces.validators;

import java.io.IOException;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.solr.SolrTools;

/**
 * Validates the query syntax and displays the number of hits.
 */
@FacesValidator("solrQueryValidator")
public class SolrQueryValidator implements Validator<String> {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrQueryValidator.class);

    /* (non-Javadoc)
     * @see jakarta.faces.validator.Validator#validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        // Empty query is ok
        if (StringUtils.isEmpty(value)) {
            return;
        }

        logger.trace("clientId: {}", component.getClientId());
        try {
            long hits = getHitCount(value);
            logger.trace("{} hits", hits);
            if (hits == 0) {
                String message = ViewerResourceBundle.getTranslation("inline_help__solr_query_warning", null).replace("{0}", String.valueOf(hits));
                logger.trace(message);
                Messages.warn(component.getClientId(), message);
            } else {
                Messages.info(component.getClientId(), "");
            }
        } catch (SolrServerException | RemoteSolrException e) {
            if (SolrTools.isQuerySyntaxError(e)) {
                logger.debug(e.getMessage());
                String message = ViewerResourceBundle.getTranslation("inline_help__solr_query_danger", null);
                FacesMessage msg = new FacesMessage(message, "");
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     *
     * @param query
     * @return Hit count
     * @throws SolrServerException
     * @throws IOException
     */
    public static long getHitCount(String query) throws SolrServerException, IOException {
        QueryResponse resp = DataManager.getInstance().getSearchIndex().testQuery(query);
        return resp.getResults().getNumFound();
    }
}
