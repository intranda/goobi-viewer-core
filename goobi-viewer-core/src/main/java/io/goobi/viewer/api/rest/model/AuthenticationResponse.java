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
package io.goobi.viewer.api.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response model for authentication endpoints")
@JsonInclude(Include.NON_NULL)
public class AuthenticationResponse {

    @Schema(description = "Result status: 'success' or 'error'", example = "success")
    private final String status;

    @Schema(description = "Error message; present only on error responses")
    private final String message;

    @Schema(description = "Plaintext UserToken; present only on successful login")
    private final String token;

    @Schema(description = "Seconds to wait before retrying; present only on HTTP 429")
    private final Integer retryAfterSeconds;

    @JsonCreator
    public AuthenticationResponse(
            @JsonProperty("status") String status,
            @JsonProperty("message") String message,
            @JsonProperty("token") String token,
            @JsonProperty("retryAfterSeconds") Integer retryAfterSeconds) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
