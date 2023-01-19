package io.extremum.model.tools.mapper.model;

import io.extremum.sharedmodels.basic.StringOrObject;

public class CompensationWithChange extends TestBasicModel {
    private StringOrObject<Change> parameters;

    public StringOrObject<Change> getParameters() {
        return parameters;
    }

    public void setParameters(StringOrObject<Change> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "CompensationWithChange{" +
                "parameters=" + parameters +
                ", uuid=" + uuid +
                '}';
    }
}
