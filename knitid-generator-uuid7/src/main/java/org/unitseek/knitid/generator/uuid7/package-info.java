/**
 * RFC 9562 UUIDv7 generator.
 *
 * <p>UUIDv7 combines a 48-bit Unix millisecond timestamp with 74 random bits.
 * In monotonic mode the random portion is incremented within the same
 * millisecond. This module is optional and is not included in the plain
 * {@code knitid-spring-boot-starter}.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9562">RFC 9562</a>
 * @since 1.0.0
 */
package org.unitseek.knitid.generator.uuid7;
