package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.goobi.viewer.controller.DateTools;

@Named("utils")
@ApplicationScoped
public class UtilityBean implements Serializable{
    
    public Map createMap(List list) {
        Map map = new HashMap<>();
        if(list != null && list.size() > 1) {
            for (int i = 0; i < list.size()-1; i+=2) {
                int keyIndex = i;
                int valueIndex = i+1;
                Object key = list.get(keyIndex);
                if(list.get(valueIndex) instanceof List) {
                    Map value = createMap((List) list.get(valueIndex));
                    map.put(key,  value);
                } else {
                    map.put(key,  list.get(valueIndex));
                }
            }
        }
        return map;
    }
    
    public LocalDate getAsDate(String string) {
        return DateTools.parseDateFromString(string).toLocalDate();
    }
    
    public LocalDateTime getAsDateTime(String string) {
        return DateTools.parseDateFromString(string);
    }

}
