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
package de.intranda.digiverso.presentation.servlets;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;

/**
 * @author Florian Alpers
 *
 */
public class DFGViewerImage extends HttpServlet implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(DFGViewerImage.class);
    
    /**
     * 
     */
    private static final long serialVersionUID = 683037127834153441L;

    public DFGViewerImage() {
        super();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        
        Path path = Paths.get(request.getPathInfo());

        String pi = path.getName(0).toString();
        String id = FilenameUtils.getBaseName(path.getName(3).toString());
        String format = FilenameUtils.getExtension(path.getName(3).toString());
        String widthString = path.getName(1).toString();
        String rotation = path.getName(2).toString();

        try {

            int width;
            try {
                width = Integer.parseInt(widthString);
            } catch (NullPointerException | NumberFormatException e) {
                throw new IllegalRequestException("Size parameter must be a number, but is " + widthString);
            }

            String baseUri = DataManager.getInstance().getConfiguration().getIiifUrl() + "image/" + pi + "/" + id;
            String uri = BeanUtils.getImageDeliveryBean().getIiif().getIIIFImageUrl(baseUri, RegionRequest.FULL, new Scale.ScaleToWidth(width),
                    new Rotation(rotation), Colortype.DEFAULT, ImageFileFormat.valueOf(format.toUpperCase()));
            logger.trace("Forwarding " + request.getPathInfo() + " to " + uri);
//            getServletContext().getRequestDispatcher(uri).forward(request, response);

            response.sendRedirect(uri);
            
        } catch (ContentLibException e) {
            throw new ServletException(e);
        }
    }
}
