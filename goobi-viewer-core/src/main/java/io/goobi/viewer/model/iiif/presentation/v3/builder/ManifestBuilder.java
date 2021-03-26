/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * ManifestBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ManifestBuilder.class);
    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();
    private final io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder v1Builder;
    
    /**
     * <p>
     * Constructor for ManifestBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public ManifestBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
        AbstractApiUrlManager v1Urls = DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v1).orElse(null);
        v1Builder = new io.goobi.viewer.model.iiif.presentation.v2.builder.AbstractBuilder(v1Urls) {};
    }
    

    /**
     * <p>
     * generateManifest.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.IPresentationModelElement} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public AbstractPresentationModelElement3 generateManifest(StructElement ele)
            throws URISyntaxException, PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {

        final AbstractPresentationModelElement3 manifest;

        if (ele.isAnchor()) {
            manifest = new Collection3(getManifestURI(ele.getPi()), ele.getPi());
            manifest.addViewingHint(ViewingHint.multipart);
        } else {
            ele.setImageNumber(1);
            manifest = new Manifest3(getManifestURI(ele.getPi()));
            SearchService search = new SearchService(v1Builder.getSearchServiceURI(ele.getPi()));
            AutoSuggestService autoComplete = new AutoSuggestService(v1Builder.getAutoCompleteServiceURI(ele.getPi()));
            search.addService(autoComplete);
            manifest.addService(search);
        }

        populate(ele, manifest);

        return manifest;
    }

    /**
     * <p>
     * populate.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public void populate(StructElement ele, final AbstractPresentationModelElement3 manifest)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException {
        manifest.setLabel(ele.getMultiLanguageDisplayLabel());
        getDescription(ele).ifPresent(desc -> manifest.setDescription(desc));
        
        manifest.addThumbnail(getThumbnail(ele));

        manifest.setRequiredStatement(getRequiredStatement());
        manifest.setLicense(DataManager.getInstance().getConfiguration().getIIIFLicenses().stream().findFirst().map(URI::create).orElse(null));
        //TODO: add provider from config
        
        addMetadata(manifest, ele);

        manifest.setNavDate(getNavDate(ele));
        
        addNavDate(ele, manifest);
        addSeeAlsos(manifest, ele);
        addRenderings(manifest, ele);

       addCmsPages(ele, manifest);
    }
    
    private LocalDateTime getNavDate(StructElement ele) {
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && StringUtils.isNotBlank(ele.getMetadataValue(navDateField))) {
            try {
                String eleValue = ele.getMetadataValue(navDateField);
                LocalDate date = LocalDate.parse(eleValue);
                return date.atStartOfDay();
            } catch (NullPointerException | DateTimeParseException e) {
                logger.warn("Unable to parse {} as Date", ele.getMetadataValue(navDateField));
            }
        }
        return null;
    }

}
