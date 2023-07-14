package io.goobi.viewer.dao.update;

import java.sql.SQLException;

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
            boolean newColumnHasEntries = dao.getNativeQueryResults("SELECT page_template_id FROM cms_pages").stream().anyMatch(o -> o != null);
            if (!newColumnHasEntries) {
                dao.executeUpdate("ALTER TABLE cms_pages DROP page_template_id;");
                dao.executeUpdate("ALTER TABLE cms_pages CHANGE COLUMN `TEMPLATEID` page_template_id BIGINT(20);");
                updates += 1;
            }
        }

        //remove references to cms-page-templates from components which belong to a cms_page
        updates += dao.executeUpdate("UPDATE cms_components SET owning_template_id = NULL WHERE owning_page_id IS NOT NULL;");
        return updates > 0;
    }

}
