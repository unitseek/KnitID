package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdContext;

/**
 * Optional identifier validator.
 */
public interface IdValidator {

    void validate(String id, IdContext context);
}
