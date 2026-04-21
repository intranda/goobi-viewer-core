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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian Alpers
 */
public class RisResourceBuilder {

    private static final Logger logger = LogManager.getLogger(RisResourceBuilder.class);

    private HttpServletRequest request;
    private HttpServletResponse response;

    /**
     * 
     * @param request HTTP servlet request
     * @param response HTTP servlet response for setting headers
     */
    public RisResourceBuilder(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * @param se structure element to generate RIS for
     * @return {@link StreamingOutput}
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ContentNotFoundException
     * @throws ContentLibException
     */
    public StreamingOutput writeRIS(StructElement se)
            throws IndexUnreachableException, DAOException, ContentLibException {
        // logger.trace("writeRIS: {}", se); //NOSONAR Debug
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");

        try {
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, request)
                    .isGranted()) {
                throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
            }
        } catch (RecordNotFoundException e1) {
            throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
        }

        String ris = MetadataTools.generateRIS(se);
        if (ris == null) {
            throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
        }

        java.nio.file.Path tempFile = Paths.get(DataManager.getInstance().getConfiguration().getTempFolder(), fileName);
        try {
            Files.write(tempFile, ris.getBytes());
        } catch (IOException e) {
            if (Files.exists(tempFile)) {
                FileUtils.deleteQuietly(tempFile.toFile());
            }
            throw new ContentLibException("Could not create RIS file " + tempFile.toAbsolutePath().toString());
        }

        return out -> {
            try (FileInputStream in = new FileInputStream(tempFile.toFile())) {
                FileTools.copyStream(out, in);
            } catch (IOException e) {
                logger.error("Error reading RIS from file {}", tempFile, e);
            } finally {
                out.flush();
                out.close();
                if (Files.exists(tempFile)) {
                    FileUtils.deleteQuietly(tempFile.toFile());
                }
            }
        };
    }

    /**
     * @param se structure element to generate RIS for
     * @return Generated RIS
     * @throws ContentNotFoundException
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public String getRIS(StructElement se) throws ContentNotFoundException, IndexUnreachableException, DAOException {
        // logger.trace("getRis: {}", se); //NOSONAR Debug
        try {
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, request)
                    .isGranted()) {
                throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
            }
        } catch (RecordNotFoundException e) {
            throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
        }

        return MetadataTools.generateRIS(se);
    }
}
