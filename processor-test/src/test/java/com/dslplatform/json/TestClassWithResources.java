package com.dslplatform.json;

import org.junit.Test;

import javax.annotation.processing.Processor;
import java.util.Collection;
import java.util.Collections;

public class TestClassWithResources {

    protected Collection<Processor> getProcessors() {
        return Collections.<Processor>singletonList(new ResourcesProcessor());
    }

    @Test
    public void dummyTest() {
        // TODO: Assert something happened during compilation
    }
}
