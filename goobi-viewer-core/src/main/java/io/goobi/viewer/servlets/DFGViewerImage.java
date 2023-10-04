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
package io.goobi.viewer.servlets;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * <p>
 * DFGViewerImage class.
 * </p>
 *
 * @author Florian Alpers
 */
public class DFGViewerImage extends HttpServlet implements Serializable {

    private static final Logger logger = LogManager.getLogger(DFGViewerImage.class);

    /**
     *
     */
    private static final long serialVersionUID = 683037127834153441L;

    /**
     * <p>
     * Constructor for DFGViewerImage.
     * </p>
     */
    public DFGViewerImage() {
        super();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Path path = Paths.get(request.getPathInfo());

        String pi = path.getName(0).toString();
        String id = FilenameUtils.getBaseName(path.getName(3).toString());
        String widthString = path.getName(1).toString();
        String rotation = path.getName(2).toString();

        int width;
        try {
            width = Integer.parseInt(widthString);
        } catch (NullPointerException | NumberFormatException e) {
            logger.error("Size parameter must be a number, but is {}", widthString);
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Size parameter must be a number, but is: " + widthString);
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
            return;
        }

        String baseUri = DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(ApiUrls.RECORDS_FILES_IMAGE).params(pi, id).build())
                .orElse(DataManager.getInstance().getConfiguration().getRestApiUrl() + "image/" + pi + "/" + id);

        try {
            String format = FilenameUtils.getExtension(path.getName(3).toString());
            String uri = BeanUtils.getImageDeliveryBean()
                    .getIiif()
                    .getIIIFImageUrl(baseUri, RegionRequest.FULL, new Scale.ScaleToWidth(width), new Rotation(rotation), Colortype.DEFAULT,
                            ImageFileFormat.getImageFileFormatFromFileExtension(format));
            response.sendRedirect(uri);
        } catch (IllegalArgumentException | ViewerConfigurationException | IOException | IllegalRequestException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException e1) {
                logger.error(e1.getMessage());
            }
        }
    }

}
