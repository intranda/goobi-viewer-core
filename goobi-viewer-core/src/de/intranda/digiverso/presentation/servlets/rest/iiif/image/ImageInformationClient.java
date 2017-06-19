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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.persistence.oxm.MediaType;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.IiifProfile;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.Service;

public class ImageInformationClient {

    /**
     * @param string
     * @return
     * @throws FileNotFoundException
     */
    public ImageInformation getImageInfo(String url) throws FileNotFoundException {
        //		Client client = ClientBuilder.newClient();
        JerseyClient client = JerseyClientBuilder.createClient();
        WebTarget target = client.target(url);
        Response response = target.request(MediaType.APPLICATION_JSON.getMediaType()).get();
        try {
            String responseString = response.readEntity(String.class);
            responseString = responseString.replaceAll(
                    "\"@context\":\"http://iiif.io/api/image/2/context.json\",\"@id\":\"http://localhost:8081/ics/iiif/image/-/http://intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg\",",
                    "");
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.addMixIn(ImageInformation.class, SimpleImageInfo.class);
            ImageInformation info = mapper.readValue(responseString, ImageInformation.class);
            return info;
        } catch (NullPointerException | ClassCastException | IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException("Unable to resolve target at " + url);
        }
    }

    abstract class SimpleImageInfo {
        @JsonIgnore
        abstract List<IiifProfile> getProfiles();

        @JsonIgnore
        abstract Service getService();

        @JsonIgnore
        abstract void setService(Service service);
    }
}
