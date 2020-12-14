package io.goobi.viewer.model.archives;

public class FieldValue {

    private String value;
    private ArchiveMetadataField field;

    public FieldValue(ArchiveMetadataField field) {
        this.field = field;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (field.getXpath().contains("unittitle")) {
            field.getEadEntry().setLabel(value);
        } else {
            this.value = value;            
        }

    }
}
