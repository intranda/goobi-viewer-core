package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;

import io.goobi.viewer.controller.model.CachingMap;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.ViewManager;

public class RecordPropertyCache {

    private final Map<String, Collection<FileType>> recordFileTypes = new CachingMap<>(50);
    private final Map<Pair<String, Integer>, Collection<FileType>> pageFileTypes = new CachingMap<>(400);
    private final Map<String, Map<String, AccessPermission>> recordPermissions = new CachingMap<>(50);
    private final Map<Pair<String, Integer>, Map<String, AccessPermission>> pagePermissions = new CachingMap<>(50);

    public Collection<FileType> getFileTypesForRecord(ViewManager viewManager) throws IndexUnreachableException, PresentationException {

        Collection<FileType> fileTypes = recordFileTypes.get(viewManager.getPi());
        if (fileTypes == null) {
            fileTypes = FileType.containedFiletypes(viewManager);
            recordFileTypes.put(viewManager.getPi(), fileTypes);
        }
        return fileTypes;
    }

    public Collection<FileType> getFileTypesForPage(PhysicalElement page)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {

        Pair<String, Integer> key = Pair.of(page.getPi(), page.getOrder());
        Collection<FileType> fileTypes = pageFileTypes.get(key);
        if (fileTypes == null) {
            fileTypes = FileType.containedFiletypes(page);
            pageFileTypes.put(key, fileTypes);
        }
        return fileTypes;
    }

    public AccessPermission getPermissionForRecord(ViewManager viewManager, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {

        Map<String, AccessPermission> permissionMap = recordPermissions.computeIfAbsent(viewManager.getPi(), s -> new HashMap<>());
        AccessPermission permission = permissionMap.get(privilege);
        if (permission == null) {
            permission = checkAccessPermissionForRecord(viewManager, privilege, request);
            permissionMap.put(privilege, permission);
        }
        return permission;
    }

    public AccessPermission getPermissionForPage(PhysicalElement page, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {

        Pair<String, Integer> key = Pair.of(page.getPi(), page.getOrder());
        Map<String, AccessPermission> permissionMap = pagePermissions.computeIfAbsent(key, s -> new HashMap<>());
        AccessPermission permission = permissionMap.get(privilege);
        if (permission == null) {
            permission = checkAccessPermissionForPage(page, privilege, request);
            permissionMap.put(privilege, permission);
        }
        return permission;
    }

    private AccessPermission checkAccessPermissionForRecord(ViewManager viewManager, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {
        AccessPermission accessPermission =
                AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(viewManager.getPi(), null,
                        privilege,
                        request);
        return accessPermission;
    }

    private AccessPermission checkAccessPermissionForPage(PhysicalElement page, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {
        AccessPermission accessPermission =
                AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, page.getPi(), page.getFileName(),
                        privilege);
        return accessPermission;
    }

}
