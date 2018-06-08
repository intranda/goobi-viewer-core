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
package de.intranda.digiverso.presentation.servlets.rest.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.CmsCollectionsBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/contentAssist")
@ViewerRestServiceBinding
public class ContentAssistResource {
	
	@GET
    @Path("/mediaTags/{input}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<String> getTagsForPageJson(@PathParam("input") String inputString) throws DAOException {
		List<String> suggestions = DataManager.getInstance().getDao().getMatchingTags(inputString);
		return suggestions;
	}
	
	@GET
    @Path("/collections/{solrField}/{input}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<String> getCollections(@PathParam("solrField") String solrField, @PathParam("input") String inputString) throws DAOException, IndexUnreachableException, PresentationException {
	    if("-".equals(inputString)) {
	        inputString = "";
	    }
	    String query = "DOCTYPE:DOCSTRCT AND (ISANCHOR:true OR ISWORK:true)";
	    List<String> facets = SearchHelper.getFacetValues(query, solrField, inputString, 0);
	    
	    List<String> collections = new ArrayList<>();
	    CmsCollectionsBean bean = BeanUtils.getCMSCollectionsBean();
	    if(bean != null) {
	        collections.addAll(bean.getCollections().stream().map(collection -> collection.getSolrFieldValue()).collect(Collectors.toList()));
	    }
	    List<String> list = facets.stream().flatMap(facet -> getHierarchy("", facet).stream()).distinct().filter(facet -> !collections.contains(facet))
	            .sorted()
	            .sorted((f1,f2) -> Integer.compare(f1.split(BrowseDcElement.split).length, f2.split(BrowseDcElement.split).length))
	            .collect(Collectors.toList());
	    
	    
	    return list;
	}
	
	
	private List<String> getHierarchy(String prefix, String facet) {
	    if(!facet.contains(BrowseDcElement.split)) {
	        ArrayList<String> list = new ArrayList<>();
	        list.add(prefix + facet);
	        return list;
	    } else {
	        int firstSeparator = facet.indexOf(BrowseDcElement.split);
	        String parent = facet.substring(0, firstSeparator);
	        List<String> children = getHierarchy(prefix + parent + BrowseDcElement.split, facet.substring(firstSeparator+BrowseDcElement.split.length()));
	        children.add(prefix + parent);
	        return children;
	    }
	}
}
