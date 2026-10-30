/**
 * TypeID generator: type-prefixed, lower-case UUIDv7 identifiers.
 *
 * <p>This optional module follows TypeID Specification v0.3.0. A TypeID is a
 * lower-case snake_case prefix, an underscore, and a UUIDv7 encoded as 26
 * lower-case Crockford Base32 characters. Prefixes are registered explicitly;
 * the module supports multiple prefixes and selects one through
 * {@link org.unitseek.knitid.core.api.IdContext#prefix()}.</p>
 *
 * <p>The module depends on {@code knitid-generator-uuid7} and therefore brings
 * the UUIDv7 factory onto the classpath. Use {@code knitid-spring-boot-starter-all}
 * when all built-in generators are desired. The built-in parser only recognizes
 * prefixed forms; suffix-only IDs produced with an empty prefix require a
 * custom parser.</p>
 *
 * @see <a href="https://github.com/jetify-com/typeid/tree/main/spec">TypeID Specification v0.3.0</a>
 * @since 1.0.0
 */
package org.unitseek.knitid.generator.typeid;
