package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import io.goobi.viewer.controller.model.LabeledValue;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

public class GeomapItemFilter {

    private final String name;
    private final String label;
    private final boolean visible;
    private final List<LabeledValue> filters;

    public GeomapItemFilter(String name, String label, boolean visible, List<LabeledValue> filters) {
        super();
        this.name = name;
        this.label = label;
        this.visible = visible;
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean isVisible() {
        return visible;
    }

    public List<LabeledValue> getFilters() {
        return filters;
    }

    public String getAsJson() {
        Locale locale = BeanUtils.getLocale();

        JSONObject json = new JSONObject();
        json.put("name", getName());
        json.put("label", ViewerResourceBundle.getTranslation(getLabel(), locale));

        List<LabeledValue> translatedFilters = new ArrayList<>();
        for (LabeledValue filter : filters) {
            LabeledValue translatedFilter =
                    new LabeledValue(filter.getValue(), ViewerResourceBundle.getTranslation(filter.getLabel(), locale), filter.getStyleClass());
            translatedFilters.add(translatedFilter);
        }
        json.put("filter", translatedFilters);
        return json.toString();
    }

    public boolean isEmpty() {
        return filters == null || filters.isEmpty();
    }

    public List<String> getFields() {
        return filters.stream().map(LabeledValue::getValue).toList();
    }

}
