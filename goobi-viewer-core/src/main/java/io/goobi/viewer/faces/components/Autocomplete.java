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
package io.goobi.viewer.faces.components;

import java.io.IOException;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.json.JSONArray;

import com.sun.faces.facelets.el.ContextualCompositeMethodExpression;

/**
 * java-backend for autocomplete composite component. Handles the commandscript call with {@link #handleAutocomplete()} and returns a call to the
 * method given in the attribute 'items'
 *
 * @author florian
 *
 */
@FacesComponent("io.goobi.viewer.faces.components.Autocomplete")
@SuppressWarnings("unchecked")
public class Autocomplete extends UINamingContainer {

    public Autocomplete() {
        super();
    }

    /**
     * Retrieve the request parameter 'term' from a commandscript call and return the result of a call to the method given in attribute 'items'
     *
     * @throws IOException
     */
    public void handleAutocomplete() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        String term = externalContext.getRequestParameterMap().get("term");
        List<String> result = getItems(term);
        externalContext.setResponseContentType("application/json");
        externalContext.setResponseCharacterEncoding("UTF-8");
        externalContext.getResponseOutputWriter().write(new JSONArray(result).toString());
        facesContext.responseComplete();
    }

    private List<String> getItems(String term) {
        ContextualCompositeMethodExpression items = (ContextualCompositeMethodExpression) getAttributes().get("items");
        return (List<String>) items.invoke(getFacesContext().getELContext(), new String[] { term });
    }

}
