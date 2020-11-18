package io.goobi.viewer.model.ead;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class FieldValue {

    private String value;
    /** contains the list of selected values in multiselect */
    private List<String> multiselectSelectedValues = new ArrayList<>();
    //    @ToString.Exclude
    private EadMetadataField field;

    public FieldValue(EadMetadataField field) {
        this.field = field;
    }

    public List<String> getPossibleValues() {
        List<String> answer = new ArrayList<>();
        for (String possibleValue : field.getSelectItemList()) {
            if (!multiselectSelectedValues.contains(possibleValue)) {
                answer.add(possibleValue);
            }
        }
        return answer;
    }

    public String getMultiselectValue() {
        return "";
    }

    public void setMultiselectValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            multiselectSelectedValues.add(value);
        }
    }

    public void removeSelectedValue(String value) {
        multiselectSelectedValues.remove(value);
    }

    public String getValuesForXmlExport() {
        if (!multiselectSelectedValues.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String selectedValue : multiselectSelectedValues) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(selectedValue);
            }
            return sb.toString();

        }
        return value;
    }

    public List<String> getSelectItemList() {
        return field.getSelectItemList();
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
