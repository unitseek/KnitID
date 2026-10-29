package org.unitseek.knitid.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import org.unitseek.knitid.core.exception.IdConfigurationException;

/**
 * Produces actionable startup failure messages for configuration errors.
 */
public class KnitIdFailureAnalyzer extends AbstractFailureAnalyzer<IdConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, IdConfigurationException cause) {
        String description = cause.getMessage();
        String action = "Check the error code/message, fix the referenced knitid.* property, "
                + "and verify the required generator/worker module is on the classpath.";
        return new FailureAnalysis(description, action, cause);
    }
}
