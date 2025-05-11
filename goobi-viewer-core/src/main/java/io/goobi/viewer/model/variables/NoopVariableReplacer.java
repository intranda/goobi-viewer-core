package io.goobi.viewer.model.variables;

import java.util.Collections;

public class NoopVariableReplacer extends VariableReplacer {

    public NoopVariableReplacer() {
        super(Collections.emptyMap());
    }

}
