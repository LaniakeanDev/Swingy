package fr._42.swingy.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;
import fr._42.swingy.persistence.dto.ArtifactDto;
import fr._42.swingy.persistence.dto.HeroDto;
import fr._42.swingy.persistence.dto.VillainDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class GameRepository {

    private static final int CURRENT_VERSION = 1;

    private final Path saveFile;
    private final ObjectMapper mapper;

    public GameRepository(String fileName) {
        this.saveFile = Paths.get(fileName).toAbsolutePath();

        SimpleModule module = new SimpleModule();
        module.addSerializer(Hero.class,    new HeroSerializer());
        module.addDeserializer(Hero.class,  new HeroDeserializer());
        module.addSerializer(Villain.class,    new VillainSerializer());
        module.addDeserializer(Villain.class,  new VillainDeserializer());

        this.mapper = new ObjectMapper()
                .registerModule(module)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }


    /* ------------------------------------------------------------------ */
    /*  Public API                                                         */
    /* ------------------------------------------------------------------ */

    public GameState load() {
        if (!Files.exists(saveFile) || isEmpty(saveFile)) {
            return emptyState();
        }

        JsonNode root;
        try {
            root = mapper.readTree(saveFile.toFile());
        } catch (IOException e) {
            throw new RepositoryException("Failed to read save file: " + saveFile, e);
        }

        int version = root.path("version").asInt(0);
        if (version != CURRENT_VERSION) {
            throw new RepositoryException(
                    "Unsupported save-file version " + version
                    + " (expected " + CURRENT_VERSION + ")");
        }

        GameState state = new GameState();
        state.version  = version;
        state.session  = readSession(root.path("session"));
        state.roster   = readRoster(root.path("roster"));
        state.villains = readVillains(root.path("villains"));
        return state;
    }

    public void save(GameState state) {
        Path temp = saveFile.resolveSibling(saveFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(saveFile.toAbsolutePath().getParent());
            mapper.writeValue(temp.toFile(), state);
            Files.move(temp, saveFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RepositoryException("Failed to write save file: " + saveFile, e);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Reading helpers                                                    */
    /* ------------------------------------------------------------------ */

    private GameState.Session readSession(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String activeHero = node.path("activeHero").asText(null);
        int mapSize = node.path("mapSize").asInt(0);
        if (activeHero == null || activeHero.isBlank() || mapSize <= 0) {
            System.err.println("[GameRepository] Ignoring malformed session block");
            return null;
        }
        return new GameState.Session(activeHero, mapSize);
    }

    private List<Hero> readRoster(JsonNode node) {
        List<Hero> heroes = new ArrayList<>();
        if (node == null || !node.isArray()) return heroes;
        for (JsonNode heroNode : node) {
            try {
                heroes.add(mapper.treeToValue(heroNode, Hero.class));
            } catch (Exception e) {
                System.err.println("[GameRepository] Skipping malformed hero: "
                        + e.getMessage());
            }
        }
        return heroes;
    }

    private List<Villain> readVillains(JsonNode node) {
        List<Villain> villains = new ArrayList<>();
        if (node == null || !node.isArray()) return villains;
        for (JsonNode villainNode : node) {
            try {
                villains.add(mapper.treeToValue(villainNode, Villain.class));
            } catch (Exception e) {
                System.err.println("[GameRepository] Skipping malformed villain: "
                        + e.getMessage());
            }
        }
        return villains;
    }

    /* ------------------------------------------------------------------ */
    /*  Domain <-> DTO conversion                                          */
    /* ------------------------------------------------------------------ */

    private static HeroDto toDto(Hero h) {
        HeroDto d = new HeroDto();
        d.name = h.getName();
        d.heroClass = h.getHeroClass().name();
        d.level = h.getLevel();
        d.experience = h.getExperience();
        d.currentHitPoints = h.getCurrentHitPoints();
        Position p = h.getPosition();
        d.x = (p != null) ? p.getX() : null;
        d.y = (p != null) ? p.getY() : null;
        d.artifacts = h.getArtifacts().stream().map(GameRepository::toDto).toList();
        return d;
    }

    private static ArtifactDto toDto(Artifact a) {
        ArtifactDto d = new ArtifactDto();
        d.type = a.getType().name();
        d.value = a.getValue();
        d.name = a.getName();
        return d;
    }

    private static VillainDto toDto(Villain v) {
        VillainDto d = new VillainDto();
        d.name = v.getName();
        d.hp = v.getHitPoints();
        d.attack = v.getAttack();
        d.defense = v.getDefense();
        d.x = v.getPosition().getX();
        d.y = v.getPosition().getY();
        return d;
    }

    private static Hero fromDto(HeroDto d) {
        String name = requireText(d.name, "hero name");
        HeroClass hc = parseHeroClass(d.heroClass);
        if (d.level <= 0) throw new IllegalArgumentException("level must be > 0, got " + d.level);
        if (d.experience < 0) throw new IllegalArgumentException("experience must be >= 0");
        Position position = (d.x == null || d.y == null || d.x < 0 || d.y < 0)
                ? null : new Position(d.x, d.y);
        List<Artifact> artifacts = new ArrayList<>();
        if (d.artifacts != null) {
            for (ArtifactDto ad : d.artifacts) artifacts.add(fromDto(ad));
        }
        return new Hero.HeroBuilder()
                .name(name)
                .heroClass(hc)
                .level(d.level)
                .experience(d.experience)
                .currentHitPoints(d.currentHitPoints)
                .position(position)
                .artifacts(artifacts)
                .build();
    }

    private static Artifact fromDto(ArtifactDto d) {
        ArtifactType type = parseArtifactType(d.type);
        String name = requireText(d.name, "artifact name");
        return new Artifact(type, d.value, name);
    }

    private static Villain fromDto(VillainDto d) {
        String name = requireText(d.name, "villain name");
        if (d.hp <= 0) {
            throw new IllegalArgumentException("villain hp must be > 0, got " + d.hp);
        }
        if (d.attack <= 0) {
            throw new IllegalArgumentException("villain attack must be > 0, got " + d.attack);
        }
        if (d.defense < 0) {
            throw new IllegalArgumentException("villain defense must be >= 0, got " + d.defense);
        }
        return new Villain(name, d.hp, d.attack, d.defense, new Position(d.x, d.y));
    }
    /* ------------------------------------------------------------------ */
    /*  Jackson bindings                                                   */
    /* ------------------------------------------------------------------ */

    private static final class HeroSerializer extends JsonSerializer<Hero> {
        @Override
        public void serialize(Hero h, JsonGenerator gen, SerializerProvider sp)
                throws IOException {
            gen.writeObject(toDto(h));
        }
    }

    private static final class HeroDeserializer extends JsonDeserializer<Hero> {
        @Override
        public Hero deserialize(JsonParser p, DeserializationContext ctx)
                throws IOException {
            return fromDto(p.readValueAs(HeroDto.class));
        }
    }

    private static final class VillainSerializer extends JsonSerializer<Villain> {
        @Override
        public void serialize(Villain v, JsonGenerator gen, SerializerProvider sp)
                throws IOException {
            gen.writeObject(toDto(v));
        }
    }

    private static final class VillainDeserializer extends JsonDeserializer<Villain> {
        @Override
        public Villain deserialize(JsonParser p, DeserializationContext ctx)
                throws IOException {
            return fromDto(p.readValueAs(VillainDto.class));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Field validators and parsers                                       */
    /* ------------------------------------------------------------------ */

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " is blank");
        }
        return value;
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

    private static GameState emptyState() {
        GameState state = new GameState();
        state.version  = CURRENT_VERSION;
        state.session  = null;
        state.roster   = new ArrayList<>();
        state.villains = new ArrayList<>();
        return state;
    }




    /**
     * Everything the repository persists. A plain mutable POJO — this is a
     * serialization artifact, not a domain object.
     *
     * <p>Contract: {@code roster} and {@code villains} are always mutable,
     * freshly-allocated lists — never {@code List.of()} — because the
     * controller adds heroes to the roster during a session.</p>
     *
     * <p>Must be {@code static} so it can be instantiated without an
     * enclosing {@code GameRepository} instance.</p>
     */
    public static class GameState {

        public int version = 1;
        public Session session;             // null when there is no active session
        public List<Hero> roster = new ArrayList<>();
        public List<Villain> villains = new ArrayList<>();

        /** Active hero name and the size of the map they're playing on. */
        public static class Session {
            public String activeHero;
            public int mapSize;

            public Session() {}

            public Session(String activeHero, int mapSize) {
                this.activeHero = activeHero;
                this.mapSize = mapSize;
            }
        }
    }

}