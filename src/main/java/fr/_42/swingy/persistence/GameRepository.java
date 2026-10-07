package fr._42.swingy.persistence;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;

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
import java.util.Optional;

/**
 * Persists the full game state: the hero roster, the currently active hero,
 * and the villains on that hero's map.
 *
 * <p>Format (one record per line, fields separated by '|'):</p>
 * <pre>
 *   # comment
 *   VERSION|1
 *   S|&lt;activeHeroName&gt;|&lt;mapSize&gt;        # session header (optional if no session)
 *   H|&lt;name&gt;|&lt;class&gt;|&lt;level&gt;|&lt;xp&gt;|&lt;hp&gt;|&lt;x&gt;|&lt;y&gt;|&lt;artifacts&gt;
 *   V|&lt;name&gt;|&lt;hp&gt;|&lt;atk&gt;|&lt;def&gt;|&lt;x&gt;|&lt;y&gt;
 * </pre>
 *
 * <p>The file is written atomically: to a temp file, then moved into place.</p>
 */
public class GameRepository {

    private static final String FIELD_SEP     = "|";
    private static final String ARTIFACT_SEP  = ",";
    private static final String ARTIFACT_KV   = ":";
    private static final int    HERO_FIELDS   = 9;  // H + 8 hero fields
    private static final int    VILLAIN_FIELDS = 7; // V + 6 villain fields
    private static final int    SESSION_FIELDS = 3; // S + 2 session fields
    private static final int    VERSION        = 1;

    private final Path saveFile;

    public GameRepository(String fileName) {
        this.saveFile = Paths.get(fileName).toAbsolutePath();
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Everything we persist. `session` is empty for a fresh install or a
     * save written before the first move — the controller then centers the
     * hero on a freshly-generated map.
     */
    public record GameState(
            List<Hero> roster,
            Optional<String> activeHeroName,
            Optional<Integer> mapSize,
            List<Villain> villains
    ) {}

    public GameState load() {
        if (!Files.exists(saveFile)) {
            return new GameState(new ArrayList<>(), Optional.empty(),
                                 Optional.empty(), new ArrayList<>());
        }

        List<Hero> heroes = new ArrayList<>();
        List<Villain> villains = new ArrayList<>();
        String activeHeroName = null;
        Integer mapSize = null;
        boolean versionSeen = false;

        try (BufferedReader reader = Files.newBufferedReader(saveFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                try {
                    String[] parts = line.split("\\" + FIELD_SEP, -1);
                    switch (parts[0]) {
                        case "VERSION" -> {
                            int fileVersion = Integer.parseInt(parts[1]);
                            if (fileVersion != VERSION) {
                                throw new IllegalArgumentException(
                                    "unsupported save-file version " + fileVersion
                                    + " (expected " + VERSION + ")");
                            }
                            versionSeen = true;
                        }
                        case "S" -> {
                            if (parts.length != SESSION_FIELDS) {
                                throw new IllegalArgumentException(
                                    "session record needs " + SESSION_FIELDS + " fields");
                            }
                            activeHeroName = parts[1];
                            mapSize = Integer.parseInt(parts[2]);
                        }
                        case "H" -> heroes.add(parseHero(parts));
                        case "V" -> villains.add(parseVillain(parts));
                        default -> throw new IllegalArgumentException(
                            "unknown record type '" + parts[0] + "'");
                    }
                } catch (RuntimeException e) {
                    System.err.printf(
                        "[GameRepository] Skipping malformed line %d in %s: %s%n",
                        lineNumber, saveFile.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to read save file: " + saveFile, e);
        }

        if (!versionSeen && (!heroes.isEmpty() || !villains.isEmpty())) {
            // Old unversioned file: warn but don't fail.
            System.err.println("[GameRepository] Warning: save file has no VERSION line; "
                    + "assuming version " + VERSION);
        }

        return new GameState(
                heroes,
                Optional.ofNullable(activeHeroName),
                Optional.ofNullable(mapSize),
                villains);
    }

    public void save(GameState state) {
        Path temp = saveFile.resolveSibling(saveFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(saveFile.toAbsolutePath().getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write("# Swingy save file — do not edit by hand");
                writer.newLine();
                writer.write("VERSION" + FIELD_SEP + VERSION);
                writer.newLine();

                if (state.activeHeroName().isPresent() && state.mapSize().isPresent()) {
                    writer.write("S" + FIELD_SEP
                            + sanitize(state.activeHeroName().get()) + FIELD_SEP
                            + state.mapSize().get());
                    writer.newLine();
                }

                for (Hero h : state.roster()) {
                    writer.write(serializeHero(h));
                    writer.newLine();
                }
                for (Villain v : state.villains()) {
                    writer.write(serializeVillain(v));
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
    private String serializeHero(Hero hero) {
        StringBuilder sb = new StringBuilder("H");
        sb.append(FIELD_SEP).append(sanitize(hero.getName()));
        sb.append(FIELD_SEP).append(hero.getHeroClass().name());
        sb.append(FIELD_SEP).append(hero.getLevel());
        sb.append(FIELD_SEP).append(hero.getExperience());
        sb.append(FIELD_SEP).append(hero.getCurrentHitPoints());

        Position pos = hero.getPosition();
        if (pos != null) {
            sb.append(FIELD_SEP).append(pos.getX());
            sb.append(FIELD_SEP).append(pos.getY());
        } else {
            sb.append(FIELD_SEP).append("-1");
            sb.append(FIELD_SEP).append("-1");
        }

        // Artifacts column — always preceded by a separator, even if empty.
        sb.append(FIELD_SEP);

        List<Artifact> artifacts = hero.getArtifacts();
        for (int i = 0; i < artifacts.size(); i++) {
            Artifact a = artifacts.get(i);
            sb.append(a.getType().name()).append(ARTIFACT_KV)
            .append(a.getValue()).append(ARTIFACT_KV)
            .append(a.getName());
            if (i < artifacts.size() - 1) sb.append(ARTIFACT_SEP);
        }
        return sb.toString();
    }
    private Hero parseHero(String[] parts) {
        if (parts.length != HERO_FIELDS) {
            throw new IllegalArgumentException(
                "hero record needs " + HERO_FIELDS + " fields, got " + parts.length);
        }
        String     name      = parts[1];
        HeroClass  heroClass = HeroClass.valueOf(parts[2]);
        int        level     = parsePositiveInt(parts[3], "level");
        long       xp        = parseNonNegativeLong(parts[4], "experience");
        int        hp        = parseNonNegativeInt(parts[5], "hitPoints");
        int        x         = Integer.parseInt(parts[6]);
        int        y         = Integer.parseInt(parts[7]);
        List<Artifact> artifacts = parseArtifacts(parts[8]);

        Position position = (x < 0 || y < 0) ? null : new Position(x, y);

        return new Hero.HeroBuilder()
                .name(name)
                .heroClass(heroClass)
                .level(level)
                .experience(xp)
                .currentHitPoints(hp)
                .position(position)
                .artifacts(artifacts)
                .build();
    }

    private String serializeVillain(Villain v) {
        Position p = v.getPosition();
        return "V" + FIELD_SEP + sanitize(v.getName())
             + FIELD_SEP + v.getHitPoints()
             + FIELD_SEP + v.getAttack()
             + FIELD_SEP + v.getDefense()
             + FIELD_SEP + p.getX()
             + FIELD_SEP + p.getY();
    }

    private Villain parseVillain(String[] parts) {
        if (parts.length != VILLAIN_FIELDS) {
            throw new IllegalArgumentException(
                "villain record needs " + VILLAIN_FIELDS + " fields, got " + parts.length);
        }
        String name = parts[1];
        int hp    = parsePositiveInt(parts[2], "villain hitPoints");
        int atk   = parsePositiveInt(parts[3], "villain attack");
        int def   = parseNonNegativeInt(parts[4], "villain defense");
        int x     = Integer.parseInt(parts[5]);
        int y     = Integer.parseInt(parts[6]);
        return new Villain(name, hp, atk, def, new Position(x, y));
    }

    private List<Artifact> parseArtifacts(String field) {
        if (field == null || field.isBlank()) return Collections.emptyList();
        List<Artifact> artifacts = new ArrayList<>();
        for (String token : field.split(ARTIFACT_SEP)) {
            String[] parts = token.split(ARTIFACT_KV, 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("bad artifact token: '" + token + "'");
            }
            artifacts.add(new Artifact(
                    ArtifactType.valueOf(parts[0]),
                    Integer.parseInt(parts[1]),
                    parts[2]));
        }
        return artifacts;
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

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
        if (v < 0) throw new IllegalArgumentException(field + " must be >= 0, got " + v);
        return v;
    }
}