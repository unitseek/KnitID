/**
 * KSUID (K-Sortable Unique ID) generator.
 *
 * <p>This optional module provides a 27-character Base62 identifier compatible
 * with the KSUID format: a 32-bit timestamp in seconds since 2014-05-13T16:53:20Z
 * followed by 128 random bits. It is independent of WorkerId assignment and has
 * no monotonic guarantee within the same second.</p>
 *
 * <p>Use {@code knitid-generator-ksuid}; it is not included in the plain
 * {@code knitid-spring-boot-starter}. Configuration uses the
 * {@code knitid.generators.<name>.ksuid} block.</p>
 *
 * @see <a href="https://github.com/segmentio/ksuid">KSUID specification</a>
 * @since 1.0.0
 */
package org.unitseek.knitid.generator.ksuid;
