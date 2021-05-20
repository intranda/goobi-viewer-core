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
package io.goobi.viewer.api.rest.v1.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;
import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;

/**
 * @author florian
 *
 */
public class MediaResourceTest extends AbstractRestApiTest {

    private static final Object PI = "02008070428708";
    private static final String FILENAME = "00000032";
    private static final Object MIMETYPE = "mpeg3";

    @Test
    public void testLoadAudio() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_AUDIO).params(PI, MIMETYPE, FILENAME + ".mp3").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());   
        }
    }       
    
    @Test
    public void testLoadMissingAudio() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_AUDIO).params(PI, MIMETYPE, FILENAME + ".mp4").build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 404", 404, response.getStatus());   
        }
    }  

    @Test
    public void testLoadAudioRange() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_AUDIO).params(PI, MIMETYPE, FILENAME + ".mp3").build();
        try (Response response = target(url)
                .request()
                .header("Range", "bytes=0-")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 206", 206, response.getStatus());   
        }
    } 
    
    @Test
    public void testLoadAudioMultiRange() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_AUDIO).params(PI, MIMETYPE, FILENAME + ".mp3").build();
        try (Response response = target(url)
                .request()
                .header("Range", "bytes=0-4,12-32,100-2000")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 206", 206, response.getStatus());   
        }
    } 
    
    @Test
    public void testLoadAudioIllegalRange() {
        String url = urls.path(RECORDS_FILES, RECORDS_FILES_AUDIO).params(PI, MIMETYPE, FILENAME + ".mp3").build();
        try (Response response = target(url)
                .request()
                .header("Range", "bytes2342345243645")
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 416", 416, response.getStatus());   
        }
    } 

}
