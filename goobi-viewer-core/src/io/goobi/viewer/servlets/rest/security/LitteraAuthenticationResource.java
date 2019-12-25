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
package io.goobi.viewer.servlets.rest.security;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.model.security.authentication.model.LitteraAuthenticationResponse;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * Sample littera authentication server mock for testing
 *
 * @author florian
 */
@Path("/littera")
public class LitteraAuthenticationResource {

	private static final String QUERY_PARAM_REGEX_ID = "id=(.*?)(?:&|$)";
	private static final String QUERY_PARAM_REGEX_PW = "pw=(.*?)(?:&|$)";
	private static final String RESPONSE_YES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
			"<Response authenticationSuccessful=\"true\" />";
	private static final String RESPONSE_NO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
			"<Response authenticationSuccessful=\"false\" />";
	
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;
	
    /**
     * <p>getResponse.</p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.model.LitteraAuthenticationResponse} object.
     * @throws io.goobi.viewer.exceptions.AuthenticationException if any.
     */
    @GET
	@Path("/externalauth")
	@Produces(MediaType.TEXT_XML)
	@ViewerRestServiceBinding
	public LitteraAuthenticationResponse getResponse() throws AuthenticationException {
		String query = servletRequest.getQueryString();
		String name = getFirstGroup(query, QUERY_PARAM_REGEX_ID).orElseThrow(() -> new AuthenticationException("No login name given"));
		String pwd = getFirstGroup(query, QUERY_PARAM_REGEX_PW).orElseThrow(() -> new AuthenticationException("No password given"));
		if(name.equals("test") && pwd.equals("test")) {
			return new LitteraAuthenticationResponse(true);
		} else {
			return new LitteraAuthenticationResponse(false);
		}
	}
	
	private Optional<String> getFirstGroup(String text, String regex) {
		if(StringUtils.isBlank(text)) {
			return Optional.empty();
		}
		Matcher matcher = Pattern.compile(regex).matcher(text);
		if(matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
			return Optional.of(matcher.group(1));
		} else {
			return Optional.empty();
		}
	}
	
}
