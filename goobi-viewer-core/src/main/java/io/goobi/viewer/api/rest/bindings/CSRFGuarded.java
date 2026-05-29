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
package io.goobi.viewer.api.rest.bindings;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.ws.rs.NameBinding;

/**
 * NameBinding annotation for the CSRFRequestFilter which enforces an
 * Origin/Referer whitelist on the four simple-request REST endpoints (multipart
 * upload) that bypass the browser's CORS preflight protection.
 *
 * <p>The filter is opt-in via {@code webapi.csrf[@enabled]=true} - when the
 * configuration switch is {@code false} (default), endpoints annotated with
 * {@code @CSRFGuarded} behave exactly like before.
 *
 * <p>Bearer-token authenticated requests are always bypassed: a bearer token is never
 * sent automatically by the browser, so its presence proves a deliberate client call.
 *
 * @see io.goobi.viewer.api.rest.filters.CSRFRequestFilter
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface CSRFGuarded {
}
