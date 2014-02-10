package com.heren.i0.core;

import java.lang.annotation.Annotation;

public interface ContainerCreator<AnnotationType extends Annotation, ConfigurationType> extends FacetEnabler {
    ServletContainer create(AnnotationType annotation, ConfigurationType configuration);
}
