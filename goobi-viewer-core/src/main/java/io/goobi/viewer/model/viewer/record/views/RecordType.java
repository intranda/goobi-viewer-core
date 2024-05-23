package io.goobi.viewer.model.viewer.record.views;

import io.goobi.viewer.model.viewer.ViewManager;

public enum RecordType {

    RECORD,
    ANCHOR,
    GROUP;

    public static RecordType fromViewManager(ViewManager vm) {
        if (vm.getTopStructElement().isAnchor()) {
            return ANCHOR;
        } else if (vm.getTopStructElement().isGroup()) {
            return GROUP;
        } else {
            return RECORD;
        }
    }

}
