package de.nova.lifehearts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ReleaseRegressionTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    @Test
    void metadataAndPublicCommandsMatchRelease() {
        YamlConfiguration plugin =
                YamlConfiguration.loadConfiguration(RESOURCES.resolve("plugin.yml").toFile());
        assertEquals("LifeHeartsPro", plugin.getString("name"));
        assertEquals("2.5.0", plugin.getString("version"));
        assertEquals("de.nova.lifehearts.LifeHeartsPlugin", plugin.getString("main"));
        assertEquals("ABC_Nova", plugin.getString("author"));
        for (String command : List.of("lifeheartspro", "revive", "heart", "team", "lives")) {
            assertNotNull(plugin.getConfigurationSection("commands." + command), command);
        }
        for (String permission :
                List.of(
                        "lifeheartspro.admin",
                        "lifeheartspro.reload",
                        "lifeheartspro.revive",
                        "lifeheartspro.heart.give",
                        "lifeheartspro.team.create")) {
            assertNotNull(plugin.getConfigurationSection("permissions." + permission), permission);
        }
    }

    @Test
    void configurationContainsBoundedStorageAndTeamSettings() {
        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(RESOURCES.resolve("config.yml").toFile());
        assertTrue(config.getInt("storage.flush-delay-ticks") > 0);
        assertTrue(config.getInt("teams.name.min-length") > 0);
        assertTrue(
                config.getInt("teams.name.max-length") >= config.getInt("teams.name.min-length"));
        assertTrue(config.getLong("actionbar.update-interval") >= 10L);
        assertTrue(config.getLong("teams.glowing.sync-interval-ticks") >= 20L);
    }

    @Test
    void updateVersionsAreComparedNumerically() {
        assertTrue(UpdateChecker.compareVersions("2.5.0", "2.4.4") > 0);
        assertTrue(UpdateChecker.compareVersions("2.10.0", "2.9.9") > 0);
        assertEquals(0, UpdateChecker.compareVersions("2.5", "2.5.0"));
        assertTrue(UpdateChecker.compareVersions("26.3", "26.2") > 0);
    }

    @Test
    void everyTranslationContainsTheEnglishKeySet() {
        Set<String> expected = deepKeys(RESOURCES.resolve("lang/en_us.yml"));
        assertFalse(expected.isEmpty());
        for (String locale : List.of("de_eu", "es_es", "fr_fr")) {
            assertEquals(expected, deepKeys(RESOURCES.resolve("lang/" + locale + ".yml")), locale);
        }
    }

    @Test
    void releaseSourcesContainNoLocalPathsOrDecompilerMarkers() throws IOException {
        List<String> forbidden =
                List.of(
                        "decompiled" + " with cfr",
                        "c:\\users\\" + "patri",
                        "documents\\" + "codex",
                        "app" + "data",
                        "_mqt" + "teo");
        try (Stream<Path> paths = Files.walk(Path.of("src"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String text =
                        Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                for (String token : forbidden) {
                    assertFalse(text.contains(token), path + " contains " + token);
                }
            }
        }
    }

    private static Set<String> deepKeys(Path file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        return new TreeSet<>(yaml.getKeys(true));
    }
}
