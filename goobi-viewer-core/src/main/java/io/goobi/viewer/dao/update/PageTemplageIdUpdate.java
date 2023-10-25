package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.Objects;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;

public class PageTemplageIdUpdate implements IModelUpdate {

    @SuppressWarnings("unchecked")
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        int updates = 0;
        //rename TEMPLATEID column to page_template_id if the latter column has no entries
        if (dao.columnsExists("cms_pages", "TEMPLATEID")) {
            boolean newColumnHasEntries = dao.getNativeQueryResults("SELECT page_template_id FROM cms_pages").stream().anyMatch(Objects::nonNull);
            if (!newColumnHasEntries) {
                dao.executeUpdate("ALTER TABLE cms_pages DROP page_template_id;");
                try {
                    dao.executeUpdate("ALTER TABLE cms_pages RENAME COLUMN TEMPLATEID page_template_id;");
                    if (!dao.columnsExists("cms_pages", "page_template_id")) {
                        dao.executeUpdate("ALTER TABLE cms_pages CHANGE TEMPLATEID page_template_id bigint(20)");
                    }
                } catch (DAOException e) {
                    //exception is  not reliable. Ignore for now and check result of operation later
                }
                if (!dao.columnsExists("cms_pages", "page_template_id")) {
                    dao.executeUpdate("ALTER TABLE cms_pages ADD COLUMN page_template_id bigint(20);");
                }
                updates += 1;
            }
        }

        //remove references to cms-page-templates from components which belong to a cms_page
        updates += dao.executeUpdate("UPDATE cms_components SET owning_template_id = NULL WHERE owning_page_id IS NOT NULL;");
        return updates > 0;
    }

}
