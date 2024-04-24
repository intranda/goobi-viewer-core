package io.goobi.viewer.model.view.visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.ViewManager;

public class RecordType {

    private final String name;
    private final PageType mainPageType;
    private final boolean showMediaFilesOnMetadataPage;
    private final boolean showTOCOnMetadataPage;
    private final List<RecordTypeCondition> conditions;

    public RecordType(String name, PageType mainPageType, boolean showMediaFilesOnMetadataPage, boolean showTOCOnMetadataPage,
            RecordTypeCondition... conditions) {
        super();
        this.name = name;
        this.mainPageType = mainPageType;
        this.showMediaFilesOnMetadataPage = showMediaFilesOnMetadataPage;
        this.showTOCOnMetadataPage = showTOCOnMetadataPage;
        this.conditions = new ArrayList<>(Arrays.asList(conditions));
    }

    public boolean matches(ViewManager viewManager) {
        return this.conditions.stream().allMatch(c -> c.matches(viewManager));
    }

    public String getName() {
        return name;
    }

    public PageType getMainPageType() {
        return mainPageType;
    }

    public boolean isShowMediaFilesOnMetadataPage() {
        return showMediaFilesOnMetadataPage;
    }

    public boolean isShowTOCOnMetadataPage() {
        return showTOCOnMetadataPage;
    }

    public List<RecordTypeCondition> getConditions() {
        return conditions;
    }

}
