package io.goobi.viewer.model.ead;

public class FieldValue {

    private String value;
    private EadMetadataField field;

    public FieldValue(EadMetadataField field) {
        this.field = field;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        if (field.getXpath().contains("unittitle")) {
            field.getEadEntry().setLabel(value);
        }

    }
}
