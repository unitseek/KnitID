package org.unitseek.knitid.core.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified identifier generator contract.
 *
 * <p>Implementations must be thread-safe. Some implementations may block
 * (for example KSID with the WAIT clock-backwards strategy); such behaviour
 * must be documented by the implementation.</p>
 */
public interface IdGenerator {

    IdType type();

    String generate();

    String generate(IdContext context);

    default List<String> generateBatch(int size) {
        return generateBatch(size, null);
    }

    default List<String> generateBatch(int size, IdContext context) {
        IdGeneratorConstants.checkBatchSize(size);
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(context == null ? generate() : generate(context));
        }
        return result;
    }
}
