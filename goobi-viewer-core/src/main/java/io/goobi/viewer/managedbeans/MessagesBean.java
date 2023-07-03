package io.goobi.viewer.managedbeans;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.messages.ViewerResourceBundle;

@Named
@RequestScoped
public class MessagesBean {

    public String createMessageKey(String...strings) {
        return Arrays.stream(strings).map(this::clean).collect(Collectors.joining("__"));
    }
    
    private String clean(String string) {
        if(StringUtils.isNotBlank(string)) {
            return string.toLowerCase().replaceAll("\\s", "_").replaceAll("[^\\w-]", "");
        } else {
            return "none";
        }
    }
    
    private String translate(String value, Locale language) {
        return ViewerResourceBundle.getTranslation(value, language);
    }
    
    private IMetadataValue getTranslations(String value) {
        return ViewerResourceBundle.getTranslations(value);
    }
}
