package org.unitseek.knitid.samples;

import java.time.Clock;
import java.util.Set;

import org.unitseek.knitid.core.random.SecureRandomProvider;
import org.unitseek.knitid.generator.ksuid.KsuidGenerator;
import org.unitseek.knitid.generator.typeid.TypeIdConfig;
import org.unitseek.knitid.generator.typeid.TypeIdGenerator;
import org.unitseek.knitid.generator.uuid7.Uuid7Generator;

/**
 * Plain-Java native-image smoke entry point.
 *
 * <p>This intentionally avoids starting a Spring context so the native check
 * validates the KnitID generators and their dependencies without depending on
 * Spring AOT/native-image internals.</p>
 */
public final class NativeSmokeMain {

    private NativeSmokeMain() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        SecureRandomProvider random = new SecureRandomProvider();
        String uuid7 = new Uuid7Generator(clock, random).generate();
        String ksuid = new KsuidGenerator(clock, random).generate();
        String typeid = new TypeIdGenerator(clock, random,
                new TypeIdConfig(Set.of("sample"), "sample", false)).generate();
        System.out.println("uuid7  " + uuid7);
        System.out.println("ksuid  " + ksuid);
        System.out.println("typeid " + typeid);
        if (uuid7.length() != 36 || ksuid.length() != 27 || !typeid.startsWith("sample_")) {
            throw new IllegalStateException("native smoke generated an invalid ID");
        }
    }
}
