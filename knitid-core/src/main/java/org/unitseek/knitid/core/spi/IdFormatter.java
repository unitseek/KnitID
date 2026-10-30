package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdContext;

/**
 * Optional output formatter, applied after validation.
 *
 * <p><strong>Reserved extension point:</strong> this SPI is not wired into the
 * 1.0.0 generation pipeline.</p>
 *
 * @since 1.0.0
 */
public interface IdFormatter {

    /**
     * Formats an identifier.
     *
     * @param raw generated identifier
     * @param context generation context, possibly {@code null}
     * @return formatted identifier
     */
    String format(String raw, IdContext context);
}
