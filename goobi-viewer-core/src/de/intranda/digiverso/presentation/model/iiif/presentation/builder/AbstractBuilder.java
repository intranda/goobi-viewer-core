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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.AnnotationType;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.EventElement;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

/**
 * @author Florian Alpers
 *
 */
public abstract class AbstractBuilder {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBuilder.class);

	private static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE,
			SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT,
			SolrConstants.DOCTYPE, SolrConstants.METADATATYPE, SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_WEBM,
			SolrConstants.PI_PARENT, SolrConstants.PI_ANCHOR, SolrConstants.LOGID, SolrConstants.ISWORK,
			SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT,
			SolrConstants.LOGID, SolrConstants.THUMBPAGENO, SolrConstants.IDDOC_PARENT, SolrConstants.NUMPAGES,
			SolrConstants.DATAREPOSITORY, SolrConstants.SOURCEDOCFORMAT };

	private final URI servletURI;
	private final URI requestURI;
	private final Optional<HttpServletRequest> request;

	public AbstractBuilder(HttpServletRequest request) throws URISyntaxException {
		this.request = Optional.ofNullable(request);
		this.servletURI = new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
		this.requestURI = new URI(
				ServletUtils.getServletPathWithoutHostAsUrlFromRequest(request) + request.getRequestURI());
	}

	public AbstractBuilder(URI servletUri, URI requestURI) {
		this.request = Optional.empty();
		this.servletURI = servletUri;
		this.requestURI = requestURI;
	}

	/**
	 * @param language
	 * @return
	 */
	protected Locale getLocale(String language) {
		Locale locale = Locale.forLanguageTag(language);
		if (locale == null) {
			locale = Locale.ENGLISH;
		}
		return locale;
	}

	protected URI getServletURI() {
		return servletURI;
	}

	protected URI absolutize(URI uri) throws URISyntaxException {
		if (uri != null && !uri.isAbsolute()) {
			return new URI(getServletURI().toString() + uri.toString());
		}
		return uri;
	}

	/**
	 * @param rssUrl
	 * @return
	 * @throws URISyntaxException
	 */
	protected URI absolutize(String url) throws URISyntaxException {
		if (url != null) {
			url = url.replaceAll("\\s", "+");
		}
		return absolutize(new URI(url));
	}

	/**
	 * @return The requested url before any presentation specific parts. Generally
	 *         the rest api url. Includes a trailing slash
	 */
	protected URI getBaseUrl() {

		String request = requestURI.toString();
		if (!request.contains("/iiif/")) {
			return requestURI;
		}
		request = request.substring(0, request.indexOf("/iiif/") + 1);
		try {
			return new URI(request);
		} catch (URISyntaxException e) {
			return requestURI;
		}

	}

	/**
	 * @return METS resolver link for the DFG Viewer
	 */
	public String getMetsResolverUrl(StructElement ele) {
		try {
			return getServletURI() + "/metsresolver?id=" + ele.getPi();
		} catch (Exception e) {
			logger.error("Could not get METS resolver URL for {}.", ele.getLuceneId());
			Messages.error("errGetCurrUrl");
		}
		return getServletURI() + "/metsresolver?id=" + 0;
	}

	/**
	 * @return LIDO resolver link for the DFG Viewer
	 */
	public String getLidoResolverUrl(StructElement ele) {
		try {
			return getServletURI() + "/lidoresolver?id=" + ele.getPi();
		} catch (Exception e) {
			logger.error("Could not get LIDO resolver URL for {}.", ele.getLuceneId());
			Messages.error("errGetCurrUrl");
		}
		return getServletURI() + "/lidoresolver?id=" + 0;
	}

	/**
	 * @return viewer image view url for the given page
	 */
	public String getViewImageUrl(PhysicalElement ele) {
		return getViewUrl(ele, PageType.viewImage);
	}

	/**
	 * @return viewer url for the given page in the given {@link PageType}
	 */
	public String getViewUrl(PhysicalElement ele, PageType pageType) {
		try {
			return getServletURI() + "/" + pageType.getName() + ele.getPurlPart();
		} catch (Exception e) {
			logger.error("Could not get METS resolver URL for page {} + in {}.", ele.getOrder(), ele.getPi());
			Messages.error("errGetCurrUrl");
		}
		return getServletURI() + "/metsresolver?id=" + 0;
	}

	/**
	 * Simple method to create a label for a {@link SolrDocument} from
	 * {@link SolrConstants.LABEL}, {@link SolrConstants.TITLE} or
	 * {@link SolrConstants.DOCSTRUCT}
	 * 
	 * @param solrDocument
	 * @return
	 */
	public static Optional<IMetadataValue> getLabelIfExists(SolrDocument solrDocument) {

		String label = (String) solrDocument.getFirstValue(SolrConstants.LABEL);
		String title = (String) solrDocument.getFirstValue(SolrConstants.TITLE);
		String docStruct = (String) solrDocument.getFirstValue(SolrConstants.DOCSTRCT);

		if (StringUtils.isNotBlank(label)) {
			return Optional.of(new SimpleMetadataValue(label));
		} else if (StringUtils.isNotBlank(title)) {
			return Optional.of(new SimpleMetadataValue(title));
		} else if (StringUtils.isNotBlank(docStruct)) {
			return Optional.of(IMetadataValue.getTranslations(docStruct));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @param manifest
	 * @param ele
	 */
	public void addMetadata(AbstractPresentationModelElement manifest, StructElement ele) {
		List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
		List<String> eventFields = DataManager.getInstance().getConfiguration().getIIIFEventFields();
		displayFields.addAll(eventFields);

		for (String field : getMetadataFields(ele)) {
			if (contained(field, displayFields) && !field.endsWith(SolrConstants._UNTOKENIZED)
					&& !field.matches(".*_LANG_\\w{2,3}")) {
				String configuredLabel = DataManager.getInstance().getConfiguration().getIIIFMetadataLabel(field);
				String label = StringUtils.isNotBlank(configuredLabel) ? configuredLabel : (field.contains("/") ? field.substring(field.indexOf("/")+1) : field);
				IMetadataValue.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2)
						.map(value -> new Metadata(IMetadataValue.getTranslations(label), value)).ifPresent(md -> {
							md.getLabel().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
							md.getValue().removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
							manifest.addMetadata(md);
						});
			}
		}
	}

	/**
	 * Return true if the field is contained in displayFields, accounting for
	 * wildcard characters
	 * 
	 * @param field
	 * @param displayFields
	 * @return
	 */
	private boolean contained(String field, List<String> displayFields) {

		return displayFields.stream().map(displayField -> displayField.replace("*", ""))
				.anyMatch(displayField -> field.startsWith(displayField));
	}

	/**
	 * @param displayFields
	 * @param allLocales
	 * @return
	 */
	private List<String> addLanguageFields(List<String> displayFields, List<Locale> locales) {
		return displayFields.stream().flatMap(field -> getLanguageFields(field, locales, true).stream())
				.collect(Collectors.toList());
	}

	private List<String> getLanguageFields(String field, List<Locale> locales, boolean includeSelf) {
		List<String> fields = new ArrayList<>();
		if (includeSelf) {
			fields.add(field);
		}
		fields.addAll(locales.stream().map(Locale::getLanguage).map(String::toUpperCase)
				.map(string -> field.concat("_LANG_").concat(string)).collect(Collectors.toList()));
		return fields;
	}

	/**
	 * @param ele
	 * @return
	 */
	private static List<String> getMetadataFields(StructElement ele) {
		Set<String> fields = ele.getMetadataFields().keySet();
		List<String> baseFields = fields.stream().map(field -> field.replaceAll("_LANG_\\w{2,3}$", "")).distinct()
				.collect(Collectors.toList());
		return baseFields;
	}

	/**
	 * Queries all DocStructs which have the given PI as PI_TOPSTRUCT or anchor (or
	 * are the anchor themselves). Works are sorted by a
	 * {@link StructElementComparator} If no hits are found, an empty list is
	 * returned
	 * 
	 * @param pi
	 * @return A list of all docstructs with the given pi or children thereof. An
	 *         empty list if no hits are found
	 * @throws PresentationException
	 * @throws IndexUnreachableException
	 */
	public List<StructElement> getDocumentWithChildren(String pi)
			throws PresentationException, IndexUnreachableException {
		String anchorQuery = "(ISWORK:* AND PI_PARENT:" + pi + ") OR (ISANCHOR:* AND PI:" + pi + ")";
		String workQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:DOCSTRCT";
		String query = "(" + anchorQuery + ") OR (" + workQuery + ")";
		List<String> displayFields = addLanguageFields(getSolrFieldList(), ViewerResourceBundle.getAllLocales());

		// handle metadata fields from events
		Map<String, List<String>> eventFields = getEventFields();
		if (!eventFields.isEmpty()) {
			String eventQuery = "PI_TOPSTRUCT:" + pi + " AND DOCTYPE:EVENT";
			query += " OR (" + eventQuery + ")";
			displayFields.addAll(
					eventFields.values().stream().flatMap(value -> value.stream()).collect(Collectors.toList()));
			displayFields.add(SolrConstants.EVENTTYPE);
		}

		List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, displayFields);
		List<StructElement> eles = new ArrayList<>();
		List<SolrDocument> events = new ArrayList<>();
		if (docs != null) {
			for (SolrDocument doc : docs) {
				if ("EVENT".equals(doc.get(SolrConstants.DOCTYPE))) {
					events.add(doc);
				} else {
					StructElement ele = new StructElement(
							Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
					eles.add(ele);
					try {
						Integer pageNo = (Integer) doc.getFieldValue(SolrConstants.THUMBPAGENO);
						ele.setImageNumber(pageNo);
						// Integer numPages = (Integer) doc.getFieldValue(SolrConstants.NUMPAGES);
					} catch (NullPointerException | ClassCastException e) {
						ele.setImageNumber(1);
					}
				}
			}
		}
		Collections.sort(eles, new StructElementComparator());
		addEventMetadataToWorkElement(eles, events);
		return eles;
	}

	/**
	 * Adds all metadata from the given events to the first work document contained
	 * in eles. All metadata will be attached twice, once in the form "/[fieldName]"
	 * and once in the form "[eventType]/[fieldName]"
	 * 
	 * @param eles   The list of StructElements from which to select the first work
	 *               document. All metadata are attached to this document
	 * @param events The list of event SolrDocuments from which to take the metadata
	 */
	protected void addEventMetadataToWorkElement(List<StructElement> eles, List<SolrDocument> events) {
		Optional<StructElement> main_o = eles.stream().filter(ele -> ele.isWork()).findFirst();
		if (main_o.isPresent()) {
			StructElement main = main_o.get();
			for (SolrDocument event : events) {
				String eventType = event.getFieldValue(SolrConstants.EVENTTYPE).toString();
				Map<String, List<String>> mds = main.getMetadataFields();
				for (String eventField : event.getFieldNames()) {
					Collection<Object> fieldValues = event.getFieldValues(eventField);
					List<String> fieldValueList = fieldValues.stream().map(Object::toString)
							.collect(Collectors.toList());
					// add the event field twice to the md-list: Once for unspecified event type and
					// once for the specific event type
					mds.put("/" + eventField, fieldValueList);
					mds.put(eventType + "/" + eventField, fieldValueList);
				}
			}
		}
	}

	/**
	 * @return
	 */
	protected Map<String, List<String>> getEventFields() {
		List<String> eventStrings = DataManager.getInstance().getConfiguration().getIIIFEventFields();
		Map<String, List<String>> events = new HashMap<>();
		for (String string : eventStrings) {
			String event, field;
			int separatorIndex = string.indexOf("/");
			if (separatorIndex > 0) {
				event = string.substring(0, separatorIndex);
				field = string.substring(separatorIndex + 1);
			} else {
				event = "";
				field = string;
			}
			List<String> eventFields = events.get(event);
			if (eventFields == null) {
				eventFields = new ArrayList<>();
				events.put(event, eventFields);
			}
			eventFields.add(field);
		}
		return events;
	}

	/**
	 * @param pi
	 * @return
	 * @throws PresentationException
	 * @throws IndexUnreachableException
	 */
	public StructElement getDocument(String pi) throws PresentationException, IndexUnreachableException {
		String query = "PI:" + pi;
		SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, getSolrFieldList());
		if (doc != null) {
			StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()),
					doc);
			ele.setImageNumber(1);
			return ele;
		}
		return null;
	}

	/**
	 * @return
	 */
	protected List<String> getSolrFieldList() {
		List<String> fields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
		for (String string : REQUIRED_SOLR_FIELDS) {
			if (!fields.contains(string)) {
				fields.add(string);
			}
		}
		String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
		if (StringUtils.isNotBlank(navDateField) && !fields.contains(navDateField)) {
			fields.add(navDateField);
		}
		fields.addAll(DataManager.getInstance().getConfiguration().getIIIFDescriptionFields());
		return fields;
	}

	/**
	 * Gets the attribution text configured in webapi.iiif.attribution and returns
	 * all translations if any are found, or the configured string itself otherwise
	 * 
	 * @return the configured attribution
	 */
	protected IMetadataValue getAttribution() {
		String message = DataManager.getInstance().getConfiguration().getIIIFAttribution();
		return IMetadataValue.getTranslations(message);
	}

	protected Optional<IMetadataValue> getDescription(StructElement ele) {
		List<String> fields = DataManager.getInstance().getConfiguration().getIIIFDescriptionFields();
		for (String field : fields) {
			Optional<IMetadataValue> optional = IMetadataValue.getTranslations(field, ele, (s1, s2) -> s1 + "; " + s2)
					.map(md -> {
						md.removeTranslation(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
						return md;
					});
			if (optional.isPresent()) {
				return optional;
			}
		}
		return Optional.empty();
	}

	/**
	 * @return the request
	 */
	protected Optional<HttpServletRequest> getRequest() {
		return request;
	}

	public URI getCollectionURI(String collectionField, String baseCollectionName) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/collections/")
				.append(collectionField);
		if (StringUtils.isNotBlank(baseCollectionName)) {
			sb.append("/").append(baseCollectionName);
		}
		return URI.create(sb.toString());
	}

	public URI getManifestURI(String pi) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/manifest");
		return URI.create(sb.toString());
	}

	public URI getRangeURI(String pi, String logId) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/range/").append(logId);
		return URI.create(sb.toString());
	}

	public URI getSequenceURI(String pi, String label) {
		if (StringUtils.isBlank(label)) {
			label = "basic";
		}
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/sequence/").append(label);
		return URI.create(sb.toString());
	}

	public URI getCanvasURI(String pi, int pageNo) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/canvas/").append(pageNo);
		return URI.create(sb.toString());
	}

	public URI getAnnotationListURI(String pi, int pageNo, AnnotationType type) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/list/").append(pageNo).append("/").append(type.name());
		return URI.create(sb.toString());
	}

	public URI getAnnotationListURI(String pi, AnnotationType type) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/list/").append(type.name());
		return URI.create(sb.toString());
	}

	public URI getLayerURI(String pi, AnnotationType type) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/layer");
		sb.append("/").append(type.name());
		return URI.create(sb.toString());
	}

	public URI getLayerURI(String pi, String logId) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/layer");
		if (StringUtils.isNotBlank(logId)) {
			sb.append("/").append(logId);
		} else {
			sb.append("/base");
		}
		return URI.create(sb.toString());
	}

	/**
	 * @param pi
	 * @param order
	 * @return
	 * @throws URISyntaxException
	 */
	public URI getImageAnnotationURI(String pi, int order) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/canvas/").append(order).append("/image/1");
		return URI.create(sb.toString());
	}

	public URI getAnnotationURI(String pi, int order, AnnotationType type, int annoNum) throws URISyntaxException {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append("/canvas/").append(order).append("/").append(type.name()).append("/").append(annoNum);
		return URI.create(sb.toString());
	}

	public URI getAnnotationURI(String pi, AnnotationType type, int annoNum) {
		StringBuilder sb = new StringBuilder(getBaseUrl().toString()).append("iiif/manifests/").append(pi)
				.append(type.name()).append("/").append(annoNum);
		return URI.create(sb.toString());
	}

}
