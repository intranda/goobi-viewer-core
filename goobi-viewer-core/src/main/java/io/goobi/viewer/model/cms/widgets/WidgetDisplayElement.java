package io.goobi.viewer.model.cms.widgets;

import java.util.List;
import java.util.Locale;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.translations.IPolyglott;

/**
 * Class for displaying available widgets in GUI
 * 
 * @author florian
 *
 */
public class WidgetDisplayElement implements IPolyglott{
    private final IMetadataValue title;
    private final IMetadataValue description;
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
        this.title = title;
        this.description = description;
        this.embeddingPages = embeddingPages;
        this.generationType = generationType;
        this.contentType = contentType;
        this.id = id;
    }
    /**
     * @return the title
     */
    public IMetadataValue getTitle() {
        return title;
    }
    /**
     * @return the description
     */
    public IMetadataValue getDescription() {
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

}
