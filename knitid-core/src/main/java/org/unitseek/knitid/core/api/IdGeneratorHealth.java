package org.unitseek.knitid.core.api;

import java.time.Duration;

/**
 * Optional interface implemented by generators that expose health details.
 */
public interface IdGeneratorHealth {

    /**
     * @param clockBackwardsWindow window used to count recent clock-backwards
     * events
     */
    IdGeneratorHealthSnapshot health(Duration clockBackwardsWindow);
}
