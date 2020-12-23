package io.goobi.viewer.model.archives;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ArchiveMetadataField {

    /** contains the internal name of the field. The value can be used to translate the field in the messages files */
    private String label;

    /**
     * metadata level, allowed values are 1-7:
     * <ul>
     * <li>1: metadata for Identity Statement Area</li>
     * <li>2: Context Area</li>
     * <li>3: Content and Structure Area</li>
     * <li>4: Condition of Access and Use Area</li>
     * <li>5: Allied Materials Area</li>
     * <li>6: Note Area</li>
     * <li>7: Description Control Area</li>
     * </ul>
     */
    private Integer type;

    /** contains a relative path to the ead value. The root of the xpath is either the {@code<ead>} element or the {@code<c>} element */
    private String xpath;

    /** type of the xpath return value, can be text, attribute, element (default) */
    private String xpathType;

    /** contains the metadata values */
    private List<FieldValue> values;

    /** links to the ead node. Required to set the title field for the entry while parsing metadata */
    //    @ToString.Exclude
    private ArchiveEntry eadEntry;




    public ArchiveMetadataField(String label, Integer type, String xpath, String xpathType) {
        this.label = label;
        this.type = type;
        this.xpath = xpath;
        this.xpathType = xpathType;
    }

    public boolean isFilled() {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (FieldValue val : values) {
            if (StringUtils.isNotBlank(val.getValue())) {
                return true;
            }
        }
        return false;
    }

    public void addFieldValue(FieldValue value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
    }

    public void addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(new FieldValue(this));
    }

    /**
     * @return the name
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param name the name to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the level
     */
    public Integer getType() {
        return type;
    }

    /**
     * @param level the level to set
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * @return the xpathType
     */
    public String getXpathType() {
        return xpathType;
    }

    /**
     * @param xpathType the xpathType to set
     */
    public void setXpathType(String xpathType) {
        this.xpathType = xpathType;
    }

    public String getValue() {
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        return values.get(0).getValue();
    }

    /**
     * @return the values
     */
    public List<FieldValue> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<FieldValue> values) {
        this.values = values;
    }

    /**
     * @return the xpath
     */
    public String getXpath() {
        return xpath;
    }

    /**
     * @param xpath the xpath to set
     */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    /**
     * @return the eadEntry
     */
    public ArchiveEntry getEadEntry() {
        return eadEntry;
    }

    /**
     * @param eadEntry the eadEntry to set
     */
    public void setEadEntry(ArchiveEntry eadEntry) {
        this.eadEntry = eadEntry;
    }
}
