package io.goobi.viewer.controller;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import io.goobi.viewer.websockets.DownloadTaskEndpoint;

public class JsonObjectSignatureBuilder {
    
        public static String getPropertiesAsJSON(Class<?> clazz) {
            Map<String, String> properties = listProperties(clazz);
            return new JSONObject(properties).toString();
        }
        
        public static Map<String, String> listProperties(Class<?> clazz) {

           return Arrays.stream(clazz.getDeclaredFields()).map(Field.class::cast).filter(field -> field.getName() != "this$0")
                   .collect(Collectors.toMap(
                    Field::getName, 
                    field -> getJSONType(field.getType())));

        }

        private static String getJSONType(Class<?> type) {
            if(boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
                return "boolean";
            } else if(Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                return "number";
            } else if(CharSequence.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type)) {
                return "string";
            } else {
                return "object";
            }
        }

        public static void main(String[] args) throws IOException {
            Map<String, String> messageProperties =listProperties(DownloadTaskEndpoint.SocketMessage.class);
//            System.out.println(messageProperties);
//            System.out.println(getPropertiesAsJSON(DownloadTaskEndpoint.SocketMessage.class));
            String json = "{\"messageQueueId\":\"string\",\"action\":\"string\",\"pi\":\"string\",\"progress\":12345,\"resourceSize\":4565675,\"files\":[{\"url\":\"https://example.com/viewer\", \"path\":\"/path/to/viewer/files\"}],\"url\":\"string\",\"status\":\"string\"}";
            DownloadTaskEndpoint.SocketMessage message = JsonTools.getAsObject(json, DownloadTaskEndpoint.SocketMessage.class);
            String jsonOut = JsonTools.getAsJson(message);
            System.out.println(jsonOut);
        }

}
