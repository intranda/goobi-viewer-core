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
package io.goobi.viewer.model.download;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>TaskClient class.</p>
 *
 * @author florian
 */
public class TaskClient {

    /**
     * <p>createPost.</p>
     *
     * @param ocrServiceUrl a {@link java.lang.String} object.
     * @param fsourceDir a {@link java.lang.String} object.
     * @param ftargetDir a {@link java.lang.String} object.
     * @param flanguage a {@link java.lang.String} object.
     * @param ffontType a {@link java.lang.String} object.
     * @param fpriority a int.
     * @param fpostOCR a {@link java.lang.String} object.
     * @param ftitlePrefix a {@link java.lang.String} object.
     * @param ftemplateName a {@link java.lang.String} object.
     * @param fjobType a {@link java.lang.String} object.
     * @param fgoobiId a {@link java.lang.String} object.
     * @param fserverType a {@link java.lang.String} object.
     * @param fimageDir a {@link java.lang.String} object.
     * @param fmetsFile a {@link java.lang.String} object.
     * @param fanchorMetsFile a {@link java.lang.String} object.
     * @param flocale a {@link java.lang.String} object.
     * @param frightToLeft a boolean.
     * @return a {@link org.apache.http.client.methods.HttpPost} object.
     */
    public static HttpPost createPost(final String ocrServiceUrl, final String fsourceDir, final String ftargetDir, final String flanguage, final String ffontType, final int fpriority,
            final String fpostOCR, final String ftitlePrefix, final String ftemplateName, final String fjobType, final String fgoobiId, final String fserverType, final String fimageDir,
            final String fmetsFile, final String fanchorMetsFile, final String flocale, final boolean frightToLeft) {

        HttpPost post = new HttpPost(ocrServiceUrl);
        ContentProducer cp = new ContentProducer() {
            @Override
            public void writeTo(OutputStream out) throws IOException {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("goobiId", fgoobiId);
                    obj.put("sourceDir", (fsourceDir));
                    obj.put("targetDir", (ftargetDir));
                    obj.put("language", flanguage);
                    obj.put("fontType", ffontType);
                    //                  obj.put("jobFormats", fjobFormats);
                    obj.put("priority", fpriority);
                    obj.put("postOCR", fpostOCR);
                    obj.put("titleprefix", ftitlePrefix);
                    obj.put("templatename", (ftemplateName));
                    obj.put("jobtype", fjobType);
                    obj.put("serverType", fserverType);
                    obj.put("imageDir", fimageDir);
                    obj.put("metsFile", fmetsFile);
                    obj.put("anchorMetsFile", fanchorMetsFile);
                    obj.put("locale", flocale);
                    obj.put("rightToLeft", frightToLeft);

                    out.write(obj.toString().getBytes());

                } catch (JSONException e) {
                    throw new IllegalArgumentException("Illegal arguments for json ", e);
                }
            }
        };
        HttpEntity e = new EntityTemplate(cp);
        post.setEntity(e);
        return post;
    }
    
    /**
     * <p>getJsonResponse.</p>
     *
     * @param client a {@link org.apache.http.client.HttpClient} object.
     * @param post a {@link org.apache.http.client.methods.HttpPost} object.
     * @return a {@link org.json.JSONObject} object.
     * @throws java.io.IOException if any.
     * @throws org.apache.http.client.ClientProtocolException if any.
     * @throws org.json.JSONException if any.
     */
    public static JSONObject getJsonResponse(HttpClient client, HttpPost post) throws IOException, ClientProtocolException, JSONException {
        // ------------------------------------------------------------------------------------------
        // Answer from OcrService
        // ------------------------------------------------------------------------------------------
        ResponseHandler<byte[]> handler = new ResponseHandler<byte[]>() {
            @Override
            public byte[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toByteArray(entity);
                } else {
                    return null;
                }
            }
        };
        String response = new String(client.execute(post, handler));
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse;
    }

}
