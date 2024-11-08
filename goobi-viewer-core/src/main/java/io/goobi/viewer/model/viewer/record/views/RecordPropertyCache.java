package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LogManager.getLogger(RecordPropertyCache.class);

    private final Map<String, Collection<FileType>> recordFileTypes = new CachingMap<>(50);
    private final Map<Pair<String, Integer>, Collection<FileType>> pageFileTypes = new CachingMap<>(400);
    private final Map<String, Map<String, AccessPermission>> recordPermissions = new CachingMap<>(50);
    private final Map<Pair<String, Integer>, Map<String, AccessPermission>> pagePermissions = new CachingMap<>(50);

    public Collection<FileType> getFileTypesForRecord(ViewManager viewManager, boolean localFilesOnly)
            throws IndexUnreachableException, PresentationException {

        Collection<FileType> fileTypes = recordFileTypes.get(viewManager.getPi());
        if (fileTypes == null) {
            fileTypes = FileType.containedFiletypes(viewManager, localFilesOnly);
            try {
                recordFileTypes.put(viewManager.getPi(), fileTypes);
            } catch (ConcurrentModificationException e) {
                logger.warn("Concurrent modification occured when updating record file types cache: {}", e.toString());
            }
        }
        return fileTypes;
    }

    public Collection<FileType> getFileTypesForPage(PhysicalElement page, boolean localFilesOnly)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {

        Pair<String, Integer> key = Pair.of(page.getPi(), page.getOrder());
        Collection<FileType> fileTypes = pageFileTypes.get(key);
        if (fileTypes == null) {
            fileTypes = FileType.containedFiletypes(page, localFilesOnly);
            try {
                pageFileTypes.put(key, fileTypes);
            } catch (ConcurrentModificationException e) {
                logger.warn("Concurrent modification occured when updating page file types cache: {}", e.toString());
            }
        }
        return fileTypes;
    }

    public AccessPermission getPermissionForRecord(ViewManager viewManager, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {

        try {
            Map<String, AccessPermission> permissionMap = recordPermissions.computeIfAbsent(viewManager.getPi(), s -> new HashMap<>());
            AccessPermission permission = permissionMap.get(privilege);
            if (permission == null) {
                permission = checkAccessPermissionForRecord(viewManager, privilege, request);
                permissionMap.put(privilege, permission);
            }
            return permission;
        } catch (ConcurrentModificationException e) {
            logger.warn("Concurrent modification occured when updating record permissions cache: {}", e.toString());
            return checkAccessPermissionForRecord(viewManager, privilege, request);
        }
    }

    public AccessPermission getPermissionForPage(PhysicalElement page, String privilege, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException, RecordNotFoundException {
        try {
            Pair<String, Integer> key = Pair.of(page.getPi(), page.getOrder());
            Map<String, AccessPermission> permissionMap = pagePermissions.computeIfAbsent(key, s -> new HashMap<>());
            AccessPermission permission = permissionMap.get(privilege);
            if (permission == null) {
                permission = checkAccessPermissionForPage(page, privilege, request);
                permissionMap.put(privilege, permission);
            }
            return permission;
        } catch (ConcurrentModificationException e) {
            logger.warn("Concurrent modification occured when updating page permissions cache: {}", e.toString());
            return checkAccessPermissionForPage(page, privilege, request);
        }
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
