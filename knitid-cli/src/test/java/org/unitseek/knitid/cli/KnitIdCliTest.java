package org.unitseek.knitid.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnitIdCliTest {

    @Test
    void printsHelpAndVersion() {
        Run help = run("--help");
        assertThat(help.code()).isZero();
        assertThat(help.out()).contains("KnitID CLI 1.0.0").contains("gen").contains("parse");

        Run version = run("--version");
        assertThat(version.code()).isZero();
        assertThat(version.out().trim()).isEqualTo("knitid 1.0.0");
    }

    @Test
    void generatesBatchOfUuid7() {
        Run run = run("gen", "--type", "UUID7", "--count", "2");
        assertThat(run.code()).isZero();
        String[] lines = run.out().trim().split("\\R");
        assertThat(lines).hasSize(2);
        for (String line : lines) {
            assertThat(UUID.fromString(line).version()).isEqualTo(7);
        }
    }

    @Test
    void parsesGeneratedUuid7() {
        Run generated = run("gen", "--type", "UUID7");
        assertThat(generated.code()).isZero();
        String id = generated.out().trim();

        Run parsed = run("parse", id);
        assertThat(parsed.code()).isZero();
        assertThat(parsed.out()).contains("type=UUID7").contains("version=7").contains("timestamp=");
    }

    @Test
    void emitsJsonForGeneration() {
        Run run = run("gen", "--type", "TYPEID", "--prefix", "order", "--format", "json");
        assertThat(run.code()).isZero();
        assertThat(run.out()).startsWith("[{\"type\":\"TYPEID\",\"id\":\"order_").contains("\"}]");
    }

    @Test
    void rejectsUnsupportedType() {
        Run run = run("gen", "--type", "BIZ");
        assertThat(run.code()).isEqualTo(CliException.USAGE);
        assertThat(run.err()).contains("CLI gen supports");
    }

    private static Run run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = KnitIdCli.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private record Run(int code, String out, String err) {
    }
}
