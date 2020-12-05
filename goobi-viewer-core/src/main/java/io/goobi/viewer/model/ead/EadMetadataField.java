package io.goobi.viewer.model.ead;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class EadMetadataField {

    /** contains the internal name of the field. The value can be used to translate the field in the messages files */
    private String name;

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
    private Integer level;

    /** contains a relative path to the ead value. The root of the xpath is either the {@code<ead>} element or the {@code<c>} element */
    private String xpath;

    /** type of the xpath return value, can be text, attribute, element (default) */
    private String xpathType;

    /** defines if the field can exist once or multiple times, values can be true/false, default is false */
    private boolean repeatable;

    /** contains the metadata values */
    private List<FieldValue> values;

    /** defines if the field is displayed on the UI, values can be true/false, default is true */
    private boolean visible;

    /** links to the ead node. Required to set the title field for the entry while parsing metadata */
    //    @ToString.Exclude
    private EadEntry eadEntry;




    public EadMetadataField(String name, Integer level, String xpath, String xpathType, boolean repeatable, boolean visible) {
        this.name = name;
        this.level = level;
        this.xpath = xpath;
        this.xpathType = xpathType;
        this.repeatable = repeatable;
        this.visible = visible;
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
        if (values.isEmpty() || repeatable) {
            values.add(value);
        }
    }

    public void addValue() {
        if (values == null) {
            values = new ArrayList<>();
        }
        if (values.isEmpty() || repeatable) {
            values.add(new FieldValue(this));
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the level
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
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

    /**
     * @return the repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * @param repeatable the repeatable to set
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
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
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
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
    public EadEntry getEadEntry() {
        return eadEntry;
    }

    /**
     * @param eadEntry the eadEntry to set
     */
    public void setEadEntry(EadEntry eadEntry) {
        this.eadEntry = eadEntry;
    }
}
