package io.goobi.viewer.model.cms.widgets.type;

public interface WidgetContentType {

    public String getLabel();

    public String getFilename();

    public String getName();

    public static WidgetContentType valueOf(String name) {
        try {
            return DefaultWidgetType.valueOf(name);
        } catch (IllegalArgumentException e) {
            try {
                return AutomaticWidgetType.valueOf(name);
            } catch (IllegalArgumentException e2) {
                try {                    
                    return CustomWidgetType.valueOf(name);
                } catch (IllegalArgumentException e3) {
                    return null;
                }
            }
        }
    }
    

    public static WidgetGenerationType getGenerationType(WidgetContentType type) {
        switch (type.getClass().getSimpleName()) {
            case "DefaultWidgetType":
                return WidgetGenerationType.DEFAULT;
            case "AutomaticWidgetType":
                return WidgetGenerationType.AUTOMATIC;
            case "CustomWidgetType":
                return WidgetGenerationType.CUSTOM;
            default:
                throw new IllegalArgumentException("Generation type for WidgetContentType " + type + " not known");
        }
    }
}
