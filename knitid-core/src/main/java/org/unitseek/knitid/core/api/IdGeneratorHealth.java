package org.unitseek.knitid.core.api;

import java.time.Duration;

/**
 * Optional interface implemented by generators that expose health details.
 *
 * @since 1.0.0
 */
public interface IdGeneratorHealth {

    /**
     * Returns a point-in-time health snapshot.
     *
     * @param clockBackwardsWindow window used to count recent clock-backwards
     *        events; {@code null} selects the implementation default
     * @return health snapshot
     */
    IdGeneratorHealthSnapshot health(Duration clockBackwardsWindow);
}
