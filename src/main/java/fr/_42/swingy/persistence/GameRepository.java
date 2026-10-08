package fr._42.swingy.persistence;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hand-rolled, human-readable persistence for Swingy.
 *
 * <p>Format: a flat sequence of {@code key = value} lines, grouped into
 * {@code [section]} blocks. Keys are dot-separated paths that encode
 * structure and list indices (e.g. {@code hero.0.artifact.1.name}). Lines
 * beginning with {@code #} and blank lines are ignored. Values that contain
 * special characters are escaped with backslashes.</p>
 *
 * <p>This deliberately avoids any third-party JSON/YAML library so the
 * project complies with the "no external libraries" rule; the only
 * dependency beyond the JDK is the {@code javax.validation} API, which the
 * subject explicitly allows.</p>
 */
public class GameRepository {

    /* ------------------------------------------------------------------ */
    /*  Format constants                                                   */
    /* ------------------------------------------------------------------ */

    private static final String HEADER =
            "# swingy save file — human-readable; safe to inspect but do not\n" +
            "# edit while the game is running (writes are atomic).\n" +
            "version = 1\n";

    private static final String ROSTER_SECTION   = "roster";
    private static final String SESSIONS_SECTION = "sessions";

    /* ------------------------------------------------------------------ */
    /*  State                                                              */
    /* ------------------------------------------------------------------ */

    private final Path saveFile;

    public GameRepository(String fileName) {
        this.saveFile = Paths.get(fileName).toAbsolutePath();
    }

    /* ================================================================== */
    /*  Public API                                                         */
    /* ================================================================== */

    public GameState load() {
        if (!Files.exists(saveFile) || isEmpty(saveFile)) {
            return emptyState();
        }

        String raw;
        try {
            raw = Files.readString(saveFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositoryException("Failed to read save file: " + saveFile, e);
        }

        Map<String, String> kv = parse(raw);

        GameState state = new GameState();
        state.roster   = readRoster(kv);
        state.sessions = readSessions(kv);

        // Drop sessions whose hero is no longer in the roster.
        state.sessions.keySet().removeIf(name -> findHero(state.roster, name) == null);

        return state;
    }

    public void save(GameState state) {
        Path temp = saveFile.resolveSibling(saveFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(saveFile.toAbsolutePath().getParent());
            Files.writeString(temp, render(state), StandardCharsets.UTF_8);
            Files.move(temp, saveFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RepositoryException("Failed to write save file: " + saveFile, e);
        }
    }

    /* ================================================================== */
    /*  Parsing: text -> flat key/value map                                */
    /* ================================================================== */

    /**
     * Parses the raw file into a flat {@code key -> value} map.
     *
     * <p>The parser is deliberately forgiving: malformed lines are skipped
     * with a warning on stderr rather than aborting the load. This matches
     * the previous behaviour, where a single bad hero/villain entry was
     * skipped without discarding the rest of the file.</p>
     */
    private Map<String, String> parse(String raw) {
        Map<String, String> out = new TreeMap<>();
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(raw))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    continue;                       // section header — no data
                }
                int eq = trimmed.indexOf('=');
                if (eq < 0) {
                    System.err.println("[GameRepository] skipping line " + lineNo
                            + " (no '='): " + trimmed);
                    skipped++;
                    continue;
                }
                String key   = trimmed.substring(0, eq).trim();
                String value = unescape(trimmed.substring(eq + 1).trim());
                if (key.isEmpty()) {
                    System.err.println("[GameRepository] skipping line " + lineNo
                            + " (empty key)");
                    skipped++;
                    continue;
                }
                out.put(key, value);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to parse save data", e);
        }

        // If the file was non-empty but contained not a single recognizable
        // line, it isn't a swingy save file. Refuse rather than silently
        // presenting the player with an empty roster.
        if (out.isEmpty() && skipped > 0) {
            throw new RepositoryException(
                    "Save file " + saveFile + " is not in swingy format "
                    + "(" + skipped + " unrecognized line(s))");
        }
        return out;
    }

    /* ================================================================== */
    /*  Reading the roster                                                 */
    /* ================================================================== */

    private List<Hero> readRoster(Map<String, String> kv) {
        List<Hero> heroes = new ArrayList<>();

        // Find every index i for which "hero.i.name" exists.
        List<Integer> indices = indicesFor(kv, "hero.", ".name");

        for (int i : indices) {
            try {
                heroes.add(readHero(kv, i));
            } catch (Exception e) {
                System.err.println("[GameRepository] Skipping malformed hero #" + i
                        + ": " + e.getMessage());
            }
        }
        return heroes;
    }

    private Hero readHero(Map<String, String> kv, int idx) {
        String prefix = "hero." + idx + ".";
        String name    = requireText(kv.get(prefix + "name"), "hero name");
        HeroClass hc   = parseHeroClass(kv.get(prefix + "class"));
        int level      = parseInt(kv.get(prefix + "level"), "level");
        long xp        = parseLong(kv.get(prefix + "experience"), "experience");
        int hp         = parseInt(kv.get(prefix + "hp"), "currentHitPoints");

        if (level <= 0) {
            throw new IllegalArgumentException("level must be > 0, got " + level);
        }
        if (xp < 0) {
            throw new IllegalArgumentException("experience must be >= 0, got " + xp);
        }

        Position position = null;
        String xs = kv.get(prefix + "x");
        String ys = kv.get(prefix + "y");
        if (xs != null && ys != null) {
            int x = parseInt(xs, "x");
            int y = parseInt(ys, "y");
            if (x >= 0 && y >= 0) {
                position = new Position(x, y);
            }
        }

        List<Artifact> artifacts = readArtifacts(kv, prefix);

        return new Hero.HeroBuilder()
                .name(name)
                .heroClass(hc)
                .level(level)
                .experience(xp)
                .currentHitPoints(hp)
                .position(position)
                .artifacts(artifacts)
                .build();
    }

    private List<Artifact> readArtifacts(Map<String, String> kv, String heroPrefix) {
        List<Artifact> out = new ArrayList<>();
        List<Integer> idxs = indicesFor(kv, heroPrefix + "artifact.", ".name");
        for (int i : idxs) {
            String p = heroPrefix + "artifact." + i + ".";
            try {
                ArtifactType type = parseArtifactType(kv.get(p + "type"));
                int value = parseInt(kv.get(p + "value"), "artifact value");
                String name = requireText(kv.get(p + "name"), "artifact name");
                out.add(new Artifact(type, value, name));
            } catch (RuntimeException e) {
                // Re-throw with positional context — the caller (readRoster)
                // will catch this and reject the *whole hero*, because losing
                // an artifact silently corrupts the hero's loadout.
                throw new IllegalArgumentException(
                        "artifact #" + i + " is malformed: " + e.getMessage(), e);
            }
        }
        return out;
    }

    /* ================================================================== */
    /*  Reading the sessions                                               */
    /* ================================================================== */

    private Map<String, GameState.Session> readSessions(Map<String, String> kv) {
        Map<String, GameState.Session> sessions = new LinkedHashMap<>();

        List<Integer> idxs = indicesFor(kv, "session.", ".hero");
        for (int i : idxs) {
            String prefix = "session." + i + ".";
            String heroName = kv.get(prefix + "hero");

            if (heroName == null || heroName.isBlank()) {
                System.err.println("[GameRepository] Skipping session #" + i
                        + ": blank hero key");
                continue;
            }
            int mapSize;
            try {
                mapSize = parseInt(kv.get(prefix + "mapSize"), "mapSize");
            } catch (Exception e) {
                System.err.println("[GameRepository] Skipping session for '"
                        + heroName + "': " + e.getMessage());
                continue;
            }
            if (mapSize <= 0) {
                System.err.println("[GameRepository] Skipping session for '" + heroName
                        + "': invalid mapSize " + mapSize);
                continue;
            }

            List<Villain> villains = readVillains(kv, prefix);
            sessions.put(heroName, new GameState.Session(mapSize, villains));
        }
        return sessions;
    }

    private List<Villain> readVillains(Map<String, String> kv, String sessionPrefix) {
        List<Villain> out = new ArrayList<>();
        List<Integer> idxs = indicesFor(kv, sessionPrefix + "villain.", ".name");
        for (int i : idxs) {
            String p = sessionPrefix + "villain." + i + ".";
            try {
                String name = requireText(kv.get(p + "name"), "villain name");
                int hp      = parseInt(kv.get(p + "hp"), "villain hp");
                int atk     = parseInt(kv.get(p + "attack"), "villain attack");
                int def     = parseInt(kv.get(p + "defense"), "villain defense");
                int x       = parseInt(kv.get(p + "x"), "villain x");
                int y       = parseInt(kv.get(p + "y"), "villain y");

                if (hp  <= 0) throw new IllegalArgumentException("hp must be > 0, got " + hp);
                if (atk <= 0) throw new IllegalArgumentException("attack must be > 0, got " + atk);
                if (def <  0) throw new IllegalArgumentException("defense must be >= 0, got " + def);

                out.add(new Villain(name, hp, atk, def, new Position(x, y)));
            } catch (Exception e) {
                System.err.println("[GameRepository] Skipping malformed villain #"
                        + i + " in " + sessionPrefix + ": " + e.getMessage());
            }
        }
        return out;
    }

    /**
     * Scans the flat map for keys of the shape {@code <prefix><N><suffix>}
     * and returns every distinct integer N, sorted ascending.
     */
    private static List<Integer> indicesFor(Map<String, String> kv,
                                            String prefix, String suffix) {
        List<Integer> out = new ArrayList<>();
        for (String key : kv.keySet()) {
            if (!key.startsWith(prefix) || !key.endsWith(suffix)) {
                continue;
            }
            String middle = key.substring(prefix.length(),
                    key.length() - suffix.length());
            try {
                out.add(Integer.parseInt(middle));
            } catch (NumberFormatException ignored) {
                // Not an indexed key (e.g. an unrelated "hero.count" line).
            }
        }
        out.sort(Integer::compareTo);
        return out;
    }

    /* ================================================================== */
    /*  Rendering: GameState -> text                                       */
    /* ================================================================== */

    private String render(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER);

        sb.append('\n').append('[').append(ROSTER_SECTION).append(']').append('\n');
        List<Hero> roster = (state.roster == null) ? List.of() : state.roster;
        for (int i = 0; i < roster.size(); i++) {
            renderHero(sb, i, roster.get(i));
        }

        sb.append('\n').append('[').append(SESSIONS_SECTION).append(']').append('\n');
        Map<String, GameState.Session> sessions =
                (state.sessions == null) ? Map.of() : state.sessions;
        int s = 0;
        for (Map.Entry<String, GameState.Session> e : sessions.entrySet()) {
            renderSession(sb, s++, e.getKey(), e.getValue());
        }

        return sb.toString();
    }

    private void renderHero(StringBuilder sb, int i, Hero h) {
        String p = "hero." + i + ".";
        line(sb, p + "name",       h.getName());
        line(sb, p + "class",      h.getHeroClass().name());
        line(sb, p + "level",      Integer.toString(h.getLevel()));
        line(sb, p + "experience", Long.toString(h.getExperience()));
        line(sb, p + "hp",         Integer.toString(h.getCurrentHitPoints()));
        Position pos = h.getPosition();
        if (pos != null) {
            line(sb, p + "x", Integer.toString(pos.getX()));
            line(sb, p + "y", Integer.toString(pos.getY()));
        }
        List<Artifact> artifacts = h.getArtifacts();
        for (int a = 0; a < artifacts.size(); a++) {
            Artifact art = artifacts.get(a);
            String ap = p + "artifact." + a + ".";
            line(sb, ap + "type",  art.getType().name());
            line(sb, ap + "value", Integer.toString(art.getValue()));
            line(sb, ap + "name",  art.getName());
        }
        sb.append('\n');        // blank line between heroes for readability
    }

    private void renderSession(StringBuilder sb, int i,
                               String heroName, GameState.Session session) {
        String p = "session." + i + ".";
        line(sb, p + "hero",    heroName);
        line(sb, p + "mapSize", Integer.toString(session.mapSize));

        List<Villain> villains = (session.villains == null)
                ? List.of() : session.villains;
        for (int v = 0; v < villains.size(); v++) {
            Villain vil = villains.get(v);
            String vp = p + "villain." + v + ".";
            line(sb, vp + "name",    vil.getName());
            line(sb, vp + "hp",      Integer.toString(vil.getHitPoints()));
            line(sb, vp + "attack",  Integer.toString(vil.getAttack()));
            line(sb, vp + "defense", Integer.toString(vil.getDefense()));
            line(sb, vp + "x",       Integer.toString(vil.getPosition().getX()));
            line(sb, vp + "y",       Integer.toString(vil.getPosition().getY()));
        }
        sb.append('\n');
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append(" = ").append(escape(value)).append('\n');
    }

    /* ================================================================== */
    /*  Escaping / unescaping                                              */
    /* ================================================================== */

    /**
     * Escapes the few characters that would otherwise break the flat
     * {@code key = value} format: backslash, newline, carriage return, and
     * the literal sequence " =" (which we escape by backslashing the '=').
     *
     * <p>Because the parser splits on the <em>first</em> '=', we only need
     * to worry about a leading '=' in a value — everything after the first
     * '=' is kept verbatim. So in practice only backslash and newlines
     * need escaping for correctness. We still escape any '=' at the very
     * start of the value for symmetry.</p>
     */
    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default   -> out.append(c);
            }
        }
        // A leading '=' is harmless because we split on the first '=', but
        // escaping it keeps the format unambiguous for hand-editing tools.
        if (out.length() > 0 && out.charAt(0) == '=') {
            out.insert(0, '\\');
        }
        return out.toString();
    }

    private static String unescape(String value) {
        if (value == null || value.indexOf('\\') < 0) return value;
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n'  -> out.append('\n');
                    case 'r'  -> out.append('\r');
                    case '\\' -> out.append('\\');
                    case '='  -> out.append('=');
                    default   -> out.append(next);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /* ================================================================== */
    /*  Small parsing helpers                                              */
    /* ================================================================== */

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " is blank");
        }
        return value;
    }

    private static int parseInt(String value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " is missing");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    what + " is not a valid integer: '" + value + "'");
        }
    }

    private static long parseLong(String value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " is missing");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    what + " is not a valid long: '" + value + "'");
        }
    }

    private static HeroClass parseHeroClass(String name) {
        if (name == null) throw new IllegalArgumentException("hero class is null");
        try {
            return HeroClass.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown hero class '" + name + "'");
        }
    }

    private static ArtifactType parseArtifactType(String name) {
        if (name == null) throw new IllegalArgumentException("artifact type is null");
        try {
            return ArtifactType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown artifact type '" + name + "'");
        }
    }

    private static boolean isEmpty(Path file) {
        try {
            return Files.size(file) == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static Hero findHero(List<Hero> roster, String name) {
        if (name == null) return null;
        for (Hero h : roster) {
            if (name.equals(h.getName())) return h;
        }
        return null;
    }

    private static GameState emptyState() {
        GameState state = new GameState();
        state.roster   = new ArrayList<>();
        state.sessions = new LinkedHashMap<>();
        return state;
    }

    /* ================================================================== */
    /*  Persisted state (unchanged public shape)                           */
    /* ================================================================== */

    public static class GameState {

        public List<Hero> roster = new ArrayList<>();

        public Map<String, Session> sessions = new LinkedHashMap<>();

        public static class Session {
            public int mapSize;
            public List<Villain> villains = new ArrayList<>();

            public Session() {}

            public Session(int mapSize, List<Villain> villains) {
                this.mapSize = mapSize;
                this.villains = (villains != null) ? villains : new ArrayList<>();
            }
        }
    }
}