/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.v1.archives;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG_BOUNDARIES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG_DRAWER_REGISTER;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG_ENTRIES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG_LOOKUP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ARCHIVES_CARDCATALOG_REGISTER;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.AccessRightsBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.model.archives.CardCatalogEntryLocation;
import io.goobi.viewer.api.rest.model.archives.CardCatalogInfo;
import io.goobi.viewer.api.rest.model.archives.CardCatalogRegister;
import io.goobi.viewer.api.rest.model.archives.CardCatalogSection;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.archives.CardCatalogDrawer;
import io.goobi.viewer.model.archives.CardCatalogEntry;
import io.goobi.viewer.model.archives.CardCatalogIndex;
import io.goobi.viewer.model.archives.CardCatalogManager;
import io.goobi.viewer.model.archives.CardCatalogRegisterRange;
import io.goobi.viewer.model.cms.CMSArchiveLink;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for the card catalog view of an archive: positional navigation over the cards of a digitized card catalog (drawers, zoom boundaries,
 * dense card ranges, computed from-to register). The endpoints are independent of whether the archive is flagged as a card catalog; the flag only
 * controls which view the archive page renders.
 */
@jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG)
@ViewerRestServiceBinding
@AccessRightsBinding({ IPrivilegeHolder.PRIV_ARCHIVE_DISPLAY_NODE })
@CORSBinding
public class CardCatalogResource {

    private static final Logger logger = LogManager.getLogger(CardCatalogResource.class);

    private static final int THUMBNAIL_SIZE = 400;
    private static final int IMAGE_SIZE = 1200;
    private static final String CACHE_CONTROL = "private, no-store";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final String pi;

    public CardCatalogResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the archive resource") @PathParam("pi") String pi) {
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid archive identifier: " + pi);
        }
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" }, summary = "Get card catalog structure and configuration for an archive")
    @ApiResponse(responseCode = "200", description = "Card catalog info with drawers and navigation configuration")
    @ApiResponse(responseCode = "404", description = "No indexed archive with the given identifier")
    public CardCatalogInfo getInfo() throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        io.goobi.viewer.controller.Configuration config = DataManager.getInstance().getConfiguration();

        CardCatalogInfo ret = new CardCatalogInfo();
        ret.setPi(index.getPi());
        ret.setLabel(index.getLabel());
        ret.setTotalEntries(index.getTotalEntries());
        ret.setMode(index.getTotalEntries() > config.getCardCatalogRegisterThreshold() ? CardCatalogInfo.MODE_LETTERS
                : CardCatalogInfo.MODE_REGISTER);
        ret.getConfig().setZoomFactor(CardCatalogManager.normalizeZoomFactor(config.getCardCatalogZoomFactor()));
        // the entries endpoint hard-caps ranges at MAX_ENTRIES_PER_REQUEST, so the page size
        // advertised to the client must never exceed it — otherwise the client would skip
        // the silently dropped tail when paging
        ret.getConfig().setListPageSize(Math.max(1, Math.min(config.getCardCatalogListPageSize(), CardCatalogManager.MAX_ENTRIES_PER_REQUEST)));
        ret.getConfig().setFullViewThreshold(Math.max(2,
                Math.min(config.getCardCatalogFullViewThreshold(), CardCatalogManager.MAX_ENTRIES_PER_REQUEST)));
        ret.getConfig().setRegisterThreshold(config.getCardCatalogRegisterThreshold());
        for (CardCatalogDrawer drawer : index.getDrawers()) {
            ret.getDrawers().add(new CardCatalogInfo.Drawer(drawer.getId(), drawer.getLabel(), drawer.getEntryCount()));
        }
        try {
            DataManager.getInstance().getDao().getCmsArchiveConfigForArchive(pi).ifPresent(archiveConfig -> {
                for (CMSArchiveLink link : archiveConfig.getLinks()) {
                    ret.getLinks().add(new CardCatalogInfo.Link(link.getLabel().getTextOrDefault(), link.getUrl()));
                }
            });
        } catch (DAOException e) {
            logger.warn("Could not load archive configuration for '{}': {}", pi, e.getMessage());
        }

        setCacheHeaders();
        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG_BOUNDARIES)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" },
            summary = "Get the boundary cards of one zoom step, evenly distributed over a position interval of a drawer")
    @ApiResponse(responseCode = "200", description = "Boundary cards with positions, labels and image URLs")
    @ApiResponse(responseCode = "404", description = "Unknown archive or drawer")
    public CardCatalogSection getBoundaries(
            @Parameter(description = "EAD node ID of the drawer") @PathParam("drawerId") String drawerId,
            @Parameter(description = "1-based start position within the drawer; defaults to 1") @QueryParam("from") Integer from,
            @Parameter(description = "1-based end position within the drawer; defaults to the drawer size") @QueryParam("to") Integer to,
            @Parameter(description = "Number of boundary cards; defaults to zoomFactor + 1") @QueryParam("count") Integer count)
            throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        CardCatalogDrawer drawer = getDrawer(index, drawerId);
        CardCatalogManager manager = DataManager.getInstance().getCardCatalogManager();

        List<Integer> positions = manager.getBoundaryPositions(drawer, from, to, count);

        CardCatalogSection ret = new CardCatalogSection();
        ret.setDrawerId(drawer.getId());
        ret.setEntryCount(drawer.getEntryCount());
        if (!positions.isEmpty()) {
            ret.setFrom(positions.get(0));
            ret.setTo(positions.get(positions.size() - 1));
        }
        List<CardCatalogEntry> entries = manager.getEntriesAtPositions(drawer, positions);
        for (int i = 0; i < positions.size(); i++) {
            ret.getItems().add(createItem(entries.get(i), positions.get(i)));
        }

        setCacheHeaders();
        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG_ENTRIES)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" }, summary = "Get a dense card range of a drawer for the full view")
    @ApiResponse(responseCode = "200", description = "Cards of the requested position range")
    @ApiResponse(responseCode = "404", description = "Unknown archive or drawer")
    public CardCatalogSection getEntries(
            @Parameter(description = "EAD node ID of the drawer") @PathParam("drawerId") String drawerId,
            @Parameter(description = "1-based start position within the drawer; defaults to 1") @QueryParam("from") Integer from,
            @Parameter(description = "1-based end position within the drawer; capped to 100 cards per request") @QueryParam("to") Integer to)
            throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        CardCatalogDrawer drawer = getDrawer(index, drawerId);
        CardCatalogManager manager = DataManager.getInstance().getCardCatalogManager();

        int n = drawer.getEntryCount();
        CardCatalogSection ret = new CardCatalogSection();
        ret.setDrawerId(drawer.getId());
        ret.setEntryCount(n);
        if (n > 0) {
            int fromPos = Math.max(1, Math.min(from != null ? from : 1, n));
            int toPos = Math.max(fromPos, Math.min(to != null ? to : fromPos + CardCatalogManager.MAX_ENTRIES_PER_REQUEST - 1, n));
            toPos = Math.min(toPos, fromPos + CardCatalogManager.MAX_ENTRIES_PER_REQUEST - 1);
            ret.setFrom(fromPos);
            ret.setTo(toPos);
            List<CardCatalogEntry> entries = manager.getEntryRange(drawer, fromPos, toPos);
            for (int i = 0; i < entries.size(); i++) {
                ret.getItems().add(createItem(entries.get(i), fromPos + i));
            }
        }

        setCacheHeaders();
        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG_REGISTER)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" },
            summary = "Get the computed from-to register of the card catalog; unavailable above the configured register threshold")
    @ApiResponse(responseCode = "200", description = "Register ranges per drawer, or available=false above the threshold")
    @ApiResponse(responseCode = "404", description = "No indexed archive with the given identifier")
    public CardCatalogRegister getRegister() throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        CardCatalogManager manager = DataManager.getInstance().getCardCatalogManager();

        CardCatalogRegister ret = new CardCatalogRegister();
        Map<CardCatalogDrawer, List<CardCatalogRegisterRange>> register = manager.getRegister(index).orElse(null);
        ret.setAvailable(register != null);
        if (register != null) {
            for (Map.Entry<CardCatalogDrawer, List<CardCatalogRegisterRange>> entry : register.entrySet()) {
                ret.getDrawers().add(createRegisterDrawer(entry.getKey(), entry.getValue()));
            }
        }

        setCacheHeaders();
        return ret;
    }

    @GET
    @jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG_DRAWER_REGISTER)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" }, summary = "Get the computed from-to register for one card catalog drawer")
    @ApiResponse(responseCode = "200", description = "One drawer's register ranges, or available=false above the threshold")
    @ApiResponse(responseCode = "404", description = "Unknown archive or drawer")
    public CardCatalogRegister getDrawerRegister(
            @Parameter(description = "EAD node ID of the drawer") @PathParam("drawerId") String drawerId)
            throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        CardCatalogDrawer drawer = getDrawer(index, drawerId);
        CardCatalogRegister ret = new CardCatalogRegister();
        DataManager.getInstance().getCardCatalogManager().getDrawerRegister(index, drawer).ifPresent(ranges -> {
            ret.setAvailable(true);
            ret.getDrawers().add(createRegisterDrawer(drawer, ranges));
        });
        setCacheHeaders();
        return ret;
    }

    private static CardCatalogRegister.RegisterDrawer createRegisterDrawer(CardCatalogDrawer source, List<CardCatalogRegisterRange> ranges) {
        CardCatalogRegister.RegisterDrawer drawer = new CardCatalogRegister.RegisterDrawer();
        drawer.setId(source.getId());
        drawer.setLabel(source.getLabel());
        drawer.setEntryCount(source.getEntryCount());
        for (CardCatalogRegisterRange range : ranges) {
            drawer.getRanges().add(new CardCatalogRegister.Range(range.getFrom(), range.getTo(), range.getFromLabel(), range.getToLabel()));
        }
        return drawer;
    }

    @GET
    @jakarta.ws.rs.Path(ARCHIVES_CARDCATALOG_LOOKUP)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "archives" }, summary = "Locate a card within the catalog by its EAD node ID (drawer and position)")
    @ApiResponse(responseCode = "200", description = "Drawer id and 1-based position of the card")
    @ApiResponse(responseCode = "404", description = "Unknown archive or entry")
    public CardCatalogEntryLocation lookupEntry(
            @Parameter(description = "EAD node ID of the card to locate") @QueryParam("entryId") String entryId)
            throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = getIndex();
        setCacheHeaders();
        return DataManager.getInstance()
                .getCardCatalogManager()
                .findEntryPosition(index, entryId)
                .map(location -> new CardCatalogEntryLocation(entryId, location.getLeft().getId(), location.getRight()))
                .orElseThrow(() -> new NotFoundException("No card found with entry id: " + entryId));
    }

    private CardCatalogIndex getIndex() throws PresentationException, IndexUnreachableException {
        CardCatalogIndex index = DataManager.getInstance()
                .getCardCatalogManager()
                .getCatalog(pi)
                .orElseThrow(() -> new NotFoundException("No indexed archive found with identifier: " + pi));
        return requirePublicDescendants(index);
    }

    /**
     * A positional catalog cannot omit individual protected nodes without changing every range,
     * boundary and deep link. Until the API supports permission-specific positions, a catalog
     * with protected descendants is unavailable as a whole.
     */
    static CardCatalogIndex requirePublicDescendants(CardCatalogIndex index) {
        if (index.hasRestrictedDescendants()) {
            throw new ForbiddenException("Card catalog contains restricted archive nodes: " + index.getPi());
        }
        return index;
    }

    private static CardCatalogDrawer getDrawer(CardCatalogIndex index, String drawerId) {
        return index.getDrawer(drawerId)
                .orElseThrow(() -> new NotFoundException("No drawer found with id: " + drawerId));
    }

    private CardCatalogSection.CardItem createItem(CardCatalogEntry entry, int position) {
        CardCatalogSection.CardItem item = new CardCatalogSection.CardItem();
        item.setPosition(position);
        item.setEntryId(entry.getId());
        item.setLabel(entry.getLabel());
        item.setRecordPi(entry.getRecordPi());
        item.setHasImage(entry.isHasImage());
        if (entry.isHasImage() && entry.getRecordPi() != null) {
            try {
                item.setThumbnailUrl(BeanUtils.getImageDeliveryBean()
                        .getThumbs()
                        .getThumbnailUrl(entry.getRecordPi(), THUMBNAIL_SIZE,
                                THUMBNAIL_SIZE));
                item.setImageUrl(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(entry.getRecordPi(), IMAGE_SIZE, IMAGE_SIZE));
            } catch (IndexUnreachableException | PresentationException | ViewerConfigurationException e) {
                logger.warn("Could not build image URL for record '{}': {}", entry.getRecordPi(), e.getMessage());
            }
        }
        return item;
    }

    private void setCacheHeaders() {
        if (servletResponse != null) {
            servletResponse.setHeader("Cache-Control", CACHE_CONTROL);
        }
    }
}
