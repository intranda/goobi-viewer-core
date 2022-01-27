package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

public class SidebarWidgetUpdate implements IModelUpdate {

    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        
        List<Object[]> info = dao.createNativeQuery("desc cms_sidebar_elements").getResultList();
        
        List<Object[]> legacyWidgets = dao.createNativeQuery("SELECT * FROM cms_sidebar_elements").getResultList();
        
        List<String> columnNames = info.stream().map(o -> (String)o[0]).collect(Collectors.toList());
        
        for (Object[] widget : legacyWidgets) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size()).boxed().filter(i -> widget[i] != null).collect(Collectors.toMap(i -> columnNames.get(i), i -> widget[i]));
            
            Long cms_sidebar_element_id = Optional.ofNullable(columns.get("cms_sidebar_element_id")).map(o -> (Long) o).orElse(null);
            String widget_type = Optional.ofNullable(columns.get("widget_type")).map(o -> (String) o).orElse(null);
            String css_class = Optional.ofNullable(columns.get("css_class")).map(o -> (String) o).orElse(null);
            Long geomap__id = Optional.ofNullable(columns.get("geomap__id")).map(o -> (Long) o).orElse(null);
            String inner_html = Optional.ofNullable(columns.get("inner_html")).map(o -> (String) o).orElse(null);
            String linked_pages = Optional.ofNullable(columns.get("linked_pages")).map(o -> (String) o).orElse(null);
            Integer sort_order = Optional.ofNullable(columns.get("sort_order")).map(o -> (Integer) o).orElse(null);
            String type = Optional.ofNullable(columns.get("type")).map(o -> (String) o).orElse(null);
            String value = Optional.ofNullable(columns.get("value")).map(o -> (String) o).orElse(null);
            String widget_mode = Optional.ofNullable(columns.get("widget_mode")).map(o -> (String) o).orElse(null);
            String widget_title = Optional.ofNullable(columns.get("widget_title")).map(o -> (String) o).orElse(null);
            Long owner_page_id = Optional.ofNullable(columns.get("owner_page_id")).map(o -> (Long) o).orElse(null);
            String additional_query = Optional.ofNullable(columns.get("additional_query")).map(o -> (String) o).orElse(null);
            Boolean descending_order = Optional.ofNullable(columns.get("descending_order")).map(o -> (Boolean) o).orElse(null);
            Integer result_display_limit = Optional.ofNullable(columns.get("result_display_limit")).map(o -> (Integer) o).orElse(null);
            String search_field = Optional.ofNullable(columns.get("search_field")).map(o -> (String) o).orElse(null);
            String generation_type = Optional.ofNullable(columns.get("generation_type")).map(o -> (String) o).orElse(null);
            String geomap_id = Optional.ofNullable(columns.get("geomap_id")).map(o -> (String) o).orElse(null);
            Long custom_widget_id = Optional.ofNullable(columns.get("custom_widget_id")).map(o -> (Long) o).orElse(null);
            
            if(StringUtils.isNotBlank(type)) {
                switch(type) {
                    
                }
            }
        }
        
        return false;
    }

}
