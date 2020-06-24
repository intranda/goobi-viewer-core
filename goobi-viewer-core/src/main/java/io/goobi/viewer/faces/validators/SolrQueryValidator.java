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
package io.goobi.viewer.faces.validators;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * Validates the query syntax and displays the number of hits.
 */
@FacesValidator("solrQueryValidator")
public class SolrQueryValidator implements Validator<String> {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SolrQueryValidator.class);

    /* (non-Javadoc)
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        // Empty query is ok
        if (StringUtils.isEmpty(value)) {
            return;
        }

        try {
            QueryResponse resp = DataManager.getInstance().getSearchIndex().testQuery(value);
            long hits = resp.getResults().getNumFound();
            logger.trace("{} hits", hits);
            if (hits == 0) {
                String message = ViewerResourceBundle.getTranslation("inline_help__solr_query_warning", null).replace("{0}", String.valueOf(hits));
                Messages.warn(component.getClientId(), message);
            } else {
                Messages.info(component.getClientId(), "");
            }

            return;
        } catch (SolrServerException | RemoteSolrException e) {
            if (SolrSearchIndex.isQuerySyntaxError(e)) {
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
}
