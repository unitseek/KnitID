package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdContext;

/**
 * Optional output formatter, applied after validation.
 */
public interface IdFormatter {

    String format(String raw, IdContext context);
}
