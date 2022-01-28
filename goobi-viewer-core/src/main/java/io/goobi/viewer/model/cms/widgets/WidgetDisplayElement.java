package io.goobi.viewer.model.cms.widgets;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class for displaying available widgets in GUI
 * 
 * @author florian
 *
 */
public class WidgetDisplayElement implements IPolyglott, Comparable<WidgetDisplayElement>{
        
    private final TranslatedText title;
    private final TranslatedText description;
    private final List<CMSPage> embeddingPages;
    private final WidgetGenerationType generationType;
    private final WidgetContentType contentType;
    private final Long id;
    
    
    /**
     * @param title
     * @param description
     * @param embeddingPages
     * @param generationType
     * @param contentType
     */
    public WidgetDisplayElement(IMetadataValue title, IMetadataValue description, List<CMSPage> embeddingPages, WidgetGenerationType generationType,
            WidgetContentType contentType) {
        this(title, description, embeddingPages, generationType, contentType, null);
    }
    
    /**
     * @param title
     * @param description
     * @param embeddingPages
     * @param generationType
     * @param contentType
     */
    public WidgetDisplayElement(IMetadataValue title, IMetadataValue description, List<CMSPage> embeddingPages, WidgetGenerationType generationType,
            WidgetContentType contentType, Long id) {
        super();
        this.title = new TranslatedText(title, getSelectedLocale());
        this.description = new TranslatedText(description, getSelectedLocale());
        this.embeddingPages = embeddingPages;
        this.generationType = generationType;
        this.contentType = contentType;
        this.id = id;
    }
    /**
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }
    /**
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }
    /**
     * @return the embeddingPages
     */
    public List<CMSPage> getEmbeddingPages() {
        return embeddingPages;
    }
    /**
     * @return the generationType
     */
    public WidgetGenerationType getGenerationType() {
        return generationType;
    }
    /**
     * @return the contentType
     */
    public WidgetContentType getContentType() {
        return contentType;
    }
    
    public Long getId() {
        return id;
    }
    
    /**
     * Both title and description are filled
     */
    @Override
    public boolean isComplete(Locale locale) {
        return !this.title.isEmpty(locale) && !this.description.isEmpty(locale);
    }
    /**
     * At least one of title and description is filled
     */
    @Override
    public boolean isValid(Locale locale) {
        return !this.title.isEmpty();
    }
    @Override
    public boolean isEmpty(Locale locale) {
        return this.title.isEmpty(locale);
    }
    @Override
    public Locale getSelectedLocale() {
        return IPolyglott.getCurrentLocale();
    }
    @Override
    public void setSelectedLocale(Locale locale) {
        //Do nothing
    }
    
    @Override
    public String toString() {
        return getTitle().getText();
    }

    @Override
    public int compareTo(WidgetDisplayElement other) {
        return StringUtils.compare(this.getTitle().getText(), other.getTitle().getText());
    }

}
