package io.goobi.viewer.model.cms.widgets;

import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

@Entity
@DiscriminatorValue("HtmlSidebarWidget")
public class HtmlSidebarWidget extends CustomSidebarWidget {

    @Column(name = "html_text", columnDefinition="LONGTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText htmlText = new TranslatedText(IPolyglott.getLocalesStatic());
    
    public HtmlSidebarWidget() {
        
    }
    
    public HtmlSidebarWidget(HtmlSidebarWidget o) {
        super(o);
        this.htmlText = new TranslatedText(o.htmlText);
    }
    
    public TranslatedText getHtmlText() {
        return htmlText;
    }

    @Override
    public boolean isComplete(Locale locale) {
        return super.isComplete(locale) && htmlText.isComplete(locale);
    }
    
    @Override
    public void setSelectedLocale(Locale locale) {
        super.setSelectedLocale(locale);
        this.htmlText.setSelectedLocale(locale);
    }

    @Override
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_HTML;
    }
}
