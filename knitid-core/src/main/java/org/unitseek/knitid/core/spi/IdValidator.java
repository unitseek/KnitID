package org.unitseek.knitid.core.spi;

import org.unitseek.knitid.core.api.IdContext;

/**
 * Optional identifier validator.
 *
 * <p><strong>Reserved extension point:</strong> this SPI is not wired into the
 * 1.0.0 generation pipeline.</p>
 *
 * @since 1.0.0
 */
public interface IdValidator {

    /**
     * Validates an identifier.
     *
     * @param id generated identifier
     * @param context generation context, possibly {@code null}
     * @throws org.unitseek.knitid.core.exception.IdValidationException if the
     *         identifier is invalid
     */
    void validate(String id, IdContext context);
}
