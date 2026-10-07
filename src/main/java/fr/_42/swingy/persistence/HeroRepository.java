package fr._42.swingy.persistence;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr._42.swingy.model.map.Position;

/**
 * Persists heroes to a plain-text file. One hero per line, fields
 * separated by '|', artifacts inlined as type:value pairs.
 *
 * This class has no domain logic — it only serializes and deserializes.
 * All game rules live in the model/controller layers.
 */
public class HeroRepository {

    private static final String FIELD_SEP      = "|";
    private static final String ARTIFACT_SEP   = ",";
    private static final String ARTIFACT_KV    = ":";
    private static final int    EXPECTED_FIELDS = 8; // 7 hero fields + artifacts column

    private final Path saveFile;

    public HeroRepository(String fileName) {
        this.saveFile = Paths.get(fileName).toAbsolutePath();
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Loads all heroes from disk. Missing file is not an error — it simply
     * means a fresh install with no saved heroes yet.
     */
    public List<Hero> loadAll() {
        if (!Files.exists(saveFile)) {
            return new ArrayList<>();
        }

        List<Hero> heroes = new ArrayList<>();
        // try-with-resources (introduced in Java 7). 
        // The parentheses declare resources that Java will automatically close when the block exits
        try (BufferedReader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // skip blanks and comments
                }
                try {
                    heroes.add(deserialize(line));
                } catch (RuntimeException e) {
                    // Corrupt line: warn and skip, don't nuke the whole save file.
                    System.err.printf(
                        "[HeroRepository] Skipping malformed line %d in %s: %s%n",
                        lineNumber, saveFile.getFileName(), e.getMessage()
                    );
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to read save file: " + saveFile, e);
        }
        return heroes;
    }

    /**
     * Overwrites the save file with the given heroes. Atomic: writes to a
     * temp file first, then moves it into place.
     */
    public void saveAll(List<Hero> heroes) {
        Path temp = saveFile.resolveSibling(saveFile.getFileName() + ".tmp");
        try {
            // make sure the folders exist
            Files.createDirectories(saveFile.toAbsolutePath().getParent());
            
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write("# Swingy save file — do not edit by hand");
                writer.newLine();
                for (Hero hero : heroes) {
                    writer.write(serialize(hero));
                    writer.newLine();
                }
            }
            Files.move(temp, saveFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RepositoryException("Failed to write save file: " + saveFile, e);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Serialization                                                      */
    /* ------------------------------------------------------------------ */

    private String serialize(Hero hero) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitize(hero.getName())).append(FIELD_SEP);
        sb.append(hero.getHeroClass().name()).append(FIELD_SEP);
        sb.append(hero.getLevel()).append(FIELD_SEP);
        sb.append(hero.getExperience()).append(FIELD_SEP);
        sb.append(hero.getCurrentHitPoints()).append(FIELD_SEP);

        Position pos = hero.getPosition();
        if (pos != null) {
            sb.append(pos.getX()).append(FIELD_SEP);
            sb.append(pos.getY()).append(FIELD_SEP);
        } else {
            sb.append("-1").append(FIELD_SEP);
            sb.append("-1").append(FIELD_SEP);
        }

        List<Artifact> artifacts = hero.getArtifacts();
        for (int i = 0; i < artifacts.size(); i++) {
            Artifact a = artifacts.get(i);
            sb.append(a.getType().name())
            .append(ARTIFACT_KV)
            .append(a.getValue())
            .append(ARTIFACT_KV)
            .append(a.getName());
            if (i < artifacts.size() - 1) {
                sb.append(ARTIFACT_SEP);
            }
        }
        return sb.toString();
    }

    private Hero deserialize(String line) {
        String[] parts = line.split("\\" + FIELD_SEP, -1);
        if (parts.length < EXPECTED_FIELDS) {
            throw new IllegalArgumentException(
                "expected " + EXPECTED_FIELDS + " fields, got " + parts.length);
        }

        String     name       = parts[0];
        HeroClass  heroClass  = HeroClass.valueOf(parts[1]);
        int        level      = parsePositiveInt(parts[2], "level");
        long       experience = parseNonNegativeLong(parts[3], "experience");
        int        currentHp  = parseNonNegativeInt(parts[4], "hitPoints");   // ← new
        int x = Integer.parseInt(parts[5]);
        int  y = Integer.parseInt(parts[6]);
        List<Artifact> artifacts = parseArtifacts(parts[7]);

        Position position = (x < 0 || y < 0) ? null : new Position(x, y);

        return new Hero.HeroBuilder()
                .name(name)
                .heroClass(heroClass)
                .level(level)
                .experience(experience)
                .currentHitPoints(currentHp)
                .position(position)
                .artifacts(artifacts)
                .build();
    }

    private List<Artifact> parseArtifacts(String field) {
        if (field == null || field.isBlank()) {
            return Collections.emptyList();
        }
        List<Artifact> artifacts = new ArrayList<>();
        for (String token : field.split(ARTIFACT_SEP)) {
            // Split into at most 3 pieces so names containing ':' still work
            String[] parts = token.split(ARTIFACT_KV, 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("bad artifact token: '" + token + "'");
            }
            ArtifactType type = ArtifactType.valueOf(parts[0]);
            int value         = Integer.parseInt(parts[1]);
            String name       = parts[2];
            artifacts.add(new Artifact(type, value, name));
        }
        return artifacts;
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /** Prevent delimiter injection — a name with '|' would corrupt the format. */
    private String sanitize(String name) {
        return name.replace(FIELD_SEP, "_").replace("\n", " ").replace("\r", " ");
    }

    private int parsePositiveInt(String s, String field) {
        int v = Integer.parseInt(s);
        if (v <= 0) throw new IllegalArgumentException(field + " must be > 0, got " + v);
        return v;
    }

    private long parseNonNegativeLong(String s, String field) {
        long v = Long.parseLong(s);
        if (v < 0) throw new IllegalArgumentException(field + " must be >= 0, got " + v);
        return v;
    }

    private int parseNonNegativeInt(String s, String field) {
        int v = Integer.parseInt(s);
        if (v < 0) {
            throw new IllegalArgumentException(field + " must be >= 0, got " + v);
        }
        return v;
    }
}
