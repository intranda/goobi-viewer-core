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
package io.goobi.viewer.api.rest.v2.cms;

import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_IMAGE;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.image.v3.ImageInformation3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_IMAGE)
@CORSBinding
public class CMSMediaImageResource3 extends ImageResource {

    public CMSMediaImageResource3(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context ApiUrls urls,
            @Parameter(description = "Filename of the image") @PathParam("filename") String filename) {
        super(context, request, response, "", getMediaFileUrl(filename).toString());
        request.setAttribute("filename", this.imageURI.toString());
        request.setAttribute(ImageResource.IIIF_VERSION, "3.0");

        String baseImageUrl = (ApiUrls.CMS_MEDIA + ApiUrls.CMS_MEDIA_FILES_FILE).replace("{filename}", "");

        String requestUrl = new String(request.getRequestURL());

        int baseStartIndex = requestUrl.indexOf(baseImageUrl);
        int baseEndIndex = baseStartIndex + baseImageUrl.length();

        String imageRequestPath = requestUrl.substring(baseEndIndex);
        int parameterPathIndex = imageRequestPath.indexOf("/") + 1;
        String imageParameterPath = "";
        if (parameterPathIndex > 0 && parameterPathIndex < imageRequestPath.length()) {
            imageParameterPath = imageRequestPath.substring(parameterPathIndex);
            requestUrl = requestUrl.substring(0, baseEndIndex + parameterPathIndex);
        }
        this.resourceURI = URI.create(requestUrl);

        List<String> parts = Arrays.stream(imageParameterPath.split("/")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (parts.size() == 4) {
            //image request
            request.setAttribute("iiif-info", false);
            request.setAttribute("iiif-region", parts.get(0));
            request.setAttribute("iiif-size", parts.get(1));
            request.setAttribute("iiif-rotation", parts.get(2));
            request.setAttribute("iiif-format", parts.get(3));
        } else if (imageRequestPath.endsWith("info.json")) {
            //image info request
            request.setAttribute("iiif-info", true);
        }
    }

    /**
     * @param filename
     * @return {@link URI}
     */
    private static URI getMediaFileUrl(String filename) {
        Path folder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path file = folder.resolve(Paths.get(filename).getFileName());
        return PathConverter.toURI(file);
    }

    @Override
    public void createResourceURI(HttpServletRequest request, String directory, String filename) throws IllegalRequestException {
        //don't do anyhting. The resource url has already been set in constructor
    }

    @Override
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_JSONLD })
    @ContentServerImageInfoBinding
    @Operation(tags = { "iiif" },
            summary = "IIIF image identifier for the CMS image file of the given filename. Returns a IIIF 3.0 image information object")
    public Response redirectToCanonicalImageInfo() throws ContentLibException {
        return super.redirectToCanonicalImageInfo();
    }

    @Override
    @GET
    @jakarta.ws.rs.Path("/info.json")
    @Produces({ MEDIA_TYPE_APPLICATION_JSONLD, MediaType.APPLICATION_JSON })
    @ContentServerImageInfoBinding
    @CORSBinding
    public ImageInformation getInfoAsJson() throws ContentLibException {
        ImageInformation info = super.getInfoAsJson();
        return new ImageInformation3(info);
    }

}
