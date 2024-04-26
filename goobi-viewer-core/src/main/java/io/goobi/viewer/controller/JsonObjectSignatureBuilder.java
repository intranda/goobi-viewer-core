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
package io.goobi.viewer.controller;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import io.goobi.viewer.websockets.DownloadTaskEndpoint.SocketMessage;

public final class JsonObjectSignatureBuilder {

    /**
     * Hidden constructor. This class should not be instantiated
     */
    private JsonObjectSignatureBuilder() {
    }

    public static String getPropertiesAsJSON(Class<?> clazz) {
        Map<String, String> properties = listProperties(clazz);
        return new JSONObject(properties).toString();
    }

    public static Map<String, String> listProperties(Class<?> clazz) {

        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field.class::cast)
                .filter(field -> !"this$0".equals(field.getName()))
                .collect(Collectors.toMap(
                        Field::getName,
                        field -> getJSONType(field.getType())));

    }

    private static String getJSONType(Class<?> type) {
        if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
            return "boolean";
        } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            return "number";
        } else if (CharSequence.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type)) {
            return "string";
        } else {
            return "object";
        }
    }

    public static void main(String[] args) throws IOException {
        String json = "{\"action\":\"update\",\"pi\":\"34192383\",\"url\":\"http://d-nb.info/1303371537/34\"}";
        SocketMessage message = JsonTools.getAsObject(json, SocketMessage.class);
        //System.out.println(message);
    }

}
