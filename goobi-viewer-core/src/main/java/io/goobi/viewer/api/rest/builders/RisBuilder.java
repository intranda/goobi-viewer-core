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
package io.goobi.viewer.api.rest.builders;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author florian
 *
 */
public class RisBuilder {
    
    HttpServletRequest request;
    HttpServletResponse response;
    
    
    public RisBuilder(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }
    
    /**
     * @param se
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ContentNotFoundException
     * @throws ContentLibException
     */
    public StreamingOutput writeRIS(StructElement se)
            throws IndexUnreachableException, DAOException, ContentNotFoundException, ContentLibException {
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, request)) {
            throw new ContentNotFoundException("Resource not found");
        }

        String ris = MetadataTools.generateRIS(se);
        if (ris == null) {
            throw new ContentNotFoundException("Resource not found");
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

        return (out) -> {
            try (FileInputStream in = new FileInputStream(tempFile.toFile())) {
                FileTools.copyStream(out, in);
            } catch (IOException e) {
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
     * @param se
     * @return
     * @throws ContentNotFoundException 
     * @throws DAOException 
     * @throws IndexUnreachableException 
     */
    public String getRIS(StructElement se) throws ContentNotFoundException, IndexUnreachableException, DAOException {
        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(se.getPi(), se.getLogid(), IPrivilegeHolder.PRIV_LIST, request)) {
            throw new ContentNotFoundException("Resource not found");
        }

        return MetadataTools.generateRIS(se);
    } 
}
