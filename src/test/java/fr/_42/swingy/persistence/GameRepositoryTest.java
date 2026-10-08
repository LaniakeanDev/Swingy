package fr._42.swingy.persistence;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;
import fr._42.swingy.persistence.GameRepository.GameState;
import fr._42.swingy.persistence.GameRepository.GameState.Session;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRepositoryTest {

    @TempDir Path tempDir;

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private Path saveFile() {
        return tempDir.resolve("heroes.json");
    }

    private GameRepository repo() {
        return new GameRepository(saveFile().toString());
    }

    private static Hero minimalHero(String name) {
        return new Hero.HeroBuilder()
                .name(name)
                .heroClass(HeroClass.CONTACT_AGENT)
                .build();
    }

    private static Hero fullHero() {
        return new Hero.HeroBuilder()
                .name("Aria")
                .heroClass(HeroClass.CONTACT_AGENT)
                .level(3)
                .experience(2500)
                .currentHitPoints(42)
                .position(new Position(5, 6))
                .artifacts(List.of(
                        new Artifact(ArtifactType.WEAPON, 7, "Runeblade"),
                        new Artifact(ArtifactType.ARMOR,  5, "Chainmail Vest"),
                        new Artifact(ArtifactType.HELM,   3, "Leather Cap")))
                .build();
    }

    private static Villain rat(int x, int y) {
        return new Villain("Giant Rat", 20, 3, 1, new Position(x, y));
    }

    private static Villain orc(int x, int y) {
        return new Villain("Orc Warrior", 70, 10, 5, new Position(x, y));
    }

    /** Build a state with a roster but no sessions. */
    private static GameState rosterOnly(List<Hero> roster) {
        GameState s = new GameState();
        s.roster = new ArrayList<>(roster);
        s.sessions = new LinkedHashMap<>();
        return s;
    }

    /** Build a state with a single session for the given hero. */
    private static GameState session(
            List<Hero> roster, String heroName, int mapSize, List<Villain> villains) {
        GameState s = rosterOnly(roster);
        s.sessions.put(heroName, new Session(mapSize, new ArrayList<>(villains)));
        return s;
    }

    /* ================================================================== */
    /*  Hero round-trip                                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("hero round-trip")
    class HeroRoundTrip {

        @Test
        void fullHeroSurvivesSaveLoad() {
            GameRepository repo = repo();
            Hero original = fullHero();

            repo.save(rosterOnly(List.of(original)));
            GameState state = repo.load();

            assertThat(state.roster).hasSize(1);
            Hero h = state.roster.get(0);

            assertThat(h.getName()).isEqualTo("Aria");
            assertThat(h.getHeroClass()).isEqualTo(HeroClass.CONTACT_AGENT);
            assertThat(h.getLevel()).isEqualTo(3);
            assertThat(h.getExperience()).isEqualTo(2500);
            assertThat(h.getCurrentHitPoints()).isEqualTo(42);
            assertThat(h.getPosition()).isEqualTo(new Position(5, 6));
            assertThat(h.getArtifacts())
                    .extracting(Artifact::getType, Artifact::getValue, Artifact::getName)
                    .containsExactly(
                            Tuple.tuple(ArtifactType.WEAPON, 7, "Runeblade"),
                            Tuple.tuple(ArtifactType.ARMOR,  5, "Chainmail Vest"),
                            Tuple.tuple(ArtifactType.HELM,   3, "Leather Cap"));
        }

        @Test
        void derivedStatsMatchAfterReload() {
            GameRepository repo = repo();
            Hero original = fullHero();
            int atk = original.getAttack();
            int def = original.getDefense();
            int hp  = original.getHitPoints();

            repo.save(rosterOnly(List.of(original)));
            Hero h = repo.load().roster.get(0);

            assertThat(h.getAttack()).isEqualTo(atk);
            assertThat(h.getDefense()).isEqualTo(def);
            assertThat(h.getHitPoints()).isEqualTo(hp);
        }

        @Test
        void heroWithNoPositionReloadsWithNullPosition() {
            Hero h = minimalHero("Aria");
            assertThat(h.getPosition()).isNull();

            repo().save(rosterOnly(List.of(h)));
            assertThat(repo().load().roster.get(0).getPosition()).isNull();
        }

        @Test
        void heroWithNoArtifactsReloadsEmpty() {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            assertThat(repo().load().roster.get(0).getArtifacts()).isEmpty();
        }

        @Test
        void multipleHeroesPreserveOrder() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(
                    minimalHero("Aria"),
                    minimalHero("Bronn"),
                    minimalHero("Cass"))));

            assertThat(repo.load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria", "Bronn", "Cass");
        }

        @Test
        void saveTwiceOverwritesRatherThanAppends() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));
            repo.save(rosterOnly(List.of(minimalHero("Bronn"))));

            assertThat(repo.load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Bronn");
        }

        @Test
        void savingEmptyRosterProducesEmptyState() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));
            repo.save(rosterOnly(List.of()));

            GameState state = repo.load();
            assertThat(state.roster).isEmpty();
            assertThat(state.sessions).isEmpty();
            assertThat(saveFile()).exists();
        }

        @Test
        void everyHeroClassRoundTrips() {
            List<Hero> heroes = new ArrayList<>();
            for (HeroClass hc : HeroClass.values()) {
                heroes.add(new Hero.HeroBuilder()
                        .name("H" + hc.name().replace("_", ""))
                        .heroClass(hc)
                        .build());
            }
            GameRepository repo = repo();
            repo.save(rosterOnly(heroes));

            assertThat(repo.load().roster)
                    .extracting(Hero::getHeroClass)
                    .containsExactly(HeroClass.values());
        }

        @Test
        void everyArtifactTypeRoundTrips() {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .artifacts(List.of(
                            new Artifact(ArtifactType.WEAPON, 1, "W"),
                            new Artifact(ArtifactType.ARMOR,  2, "A"),
                            new Artifact(ArtifactType.HELM,   3, "H")))
                    .build();
            repo().save(rosterOnly(List.of(h)));

            assertThat(repo().load().roster.get(0).getArtifacts())
                    .extracting(Artifact::getType)
                    .containsExactly(ArtifactType.WEAPON, ArtifactType.ARMOR, ArtifactType.HELM);
        }

        @Test
        void artifactNameWithSpacesRoundTrips() {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .artifacts(List.of(new Artifact(ArtifactType.HELM, 14, "Crown of Ages")))
                    .build();
            repo().save(rosterOnly(List.of(h)));

            assertThat(repo().load().roster.get(0).getArtifacts())
                    .extracting(Artifact::getName)
                    .containsExactly("Crown of Ages");
        }

        @ParameterizedTest(name = "name=\"{0}\"")
        @ValueSource(strings = {"Aria", "Bo B", "Ann Marie Smith", "X Y Z"})
        void validHeroNamesRoundTrip(String name) {
            Hero h = new Hero.HeroBuilder()
                    .name(name).heroClass(HeroClass.CONTACT_AGENT).build();
            repo().save(rosterOnly(List.of(h)));

            assertThat(repo().load().roster.get(0).getName()).isEqualTo(name);
        }
    }

    /* ================================================================== */
    /*  Villain round-trip (per session)                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("villain round-trip")
    class VillainRoundTrip {

        private List<Villain> villainsOf(GameState state, String hero) {
            Session s = state.sessions.get(hero);
            return (s == null) ? null : s.villains;
        }

        @Test
        void singleVillainSurvivesSaveLoad() {
            Villain original = orc(7, 8);

            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(original)));
            GameState loaded = repo().load();

            List<Villain> villains = villainsOf(loaded, "Aria");
            assertThat(villains).hasSize(1);
            Villain v = villains.get(0);
            assertThat(v.getName()).isEqualTo("Orc Warrior");
            assertThat(v.getHitPoints()).isEqualTo(70);
            assertThat(v.getAttack()).isEqualTo(10);
            assertThat(v.getDefense()).isEqualTo(5);
            assertThat(v.getPosition()).isEqualTo(new Position(7, 8));
        }

        @Test
        void multipleVillainsPreserveOrder() {
            List<Villain> villains = List.of(rat(2, 4), orc(7, 8), rat(9, 1));

            repo().save(session(List.of(fullHero()), "Aria", 11, villains));
            GameState loaded = repo().load();

            assertThat(villainsOf(loaded, "Aria"))
                    .extracting(Villain::getName,
                                Villain::getPosition,
                                Villain::getHitPoints,
                                Villain::getAttack,
                                Villain::getDefense)
                    .containsExactly(
                            Tuple.tuple("Giant Rat",   new Position(2, 4), 20, 3,  1),
                            Tuple.tuple("Orc Warrior", new Position(7, 8), 70, 10, 5),
                            Tuple.tuple("Giant Rat",   new Position(9, 1), 20, 3,  1));
        }

        @Test
        void emptyVillainListIsPreservedNotCollapsed() {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of()));
            GameState loaded = repo().load();

            Session s = loaded.sessions.get("Aria");
            assertThat(s).isNotNull();
            assertThat(s.villains).isEmpty();
            assertThat(s.mapSize).isEqualTo(11);
        }

        @Test
        void villainAtBorderRoundTrips() {
            Villain v = rat(0, 0);
            repo().save(session(List.of(fullHero()), "Aria", 5, List.of(v)));

            assertThat(villainsOf(repo().load(), "Aria").get(0).getPosition())
                    .isEqualTo(new Position(0, 0));
        }

        @Test
        void villainNameWithSpacesRoundTrips() {
            Villain v = new Villain("Minotaur Lord", 160, 17, 12, new Position(3, 3));
            repo().save(session(List.of(fullHero()), "Aria", 20, List.of(v)));

            assertThat(villainsOf(repo().load(), "Aria").get(0).getName())
                    .isEqualTo("Minotaur Lord");
        }

        @Test
        void manyVillainsSurviveInOrder() {
            List<Villain> villains = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                villains.add(rat(i % 10, i / 10));
            }
            repo().save(session(List.of(fullHero()), "Aria", 20, villains));

            assertThat(villainsOf(repo().load(), "Aria"))
                    .hasSize(15)
                    .extracting(Villain::getPosition)
                    .containsExactlyElementsOf(
                            villains.stream().map(Villain::getPosition).toList());
        }

        @Test
        void eachHeroOwnsItsOwnVillainList() {
            GameState s = new GameState();
            s.roster = new ArrayList<>(List.of(
                    minimalHero("Aria"), minimalHero("Bronn")));
            s.sessions.put("Aria",  new Session(11, List.of(rat(2, 4), orc(7, 8))));
            s.sessions.put("Bronn", new Session(9,  List.of(rat(1, 1))));

            repo().save(s);
            GameState loaded = repo().load();

            assertThat(villainsOf(loaded, "Aria"))
                    .extracting(Villain::getName)
                    .containsExactly("Giant Rat", "Orc Warrior");
            assertThat(villainsOf(loaded, "Bronn"))
                    .extracting(Villain::getName)
                    .containsExactly("Giant Rat");
            assertThat(villainsOf(loaded, "Aria"))
                    .isNotSameAs(villainsOf(loaded, "Bronn"));
        }
    }

    /* ================================================================== */
    /*  Session map                                                        */
    /* ================================================================== */

    @Nested
    @DisplayName("session map")
    class SessionMap {

        @Test
        void mapSizeSurvives() {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(1, 1))));
            GameState loaded = repo().load();

            Session s = loaded.sessions.get("Aria");
            assertThat(s).isNotNull();
            assertThat(s.mapSize).isEqualTo(11);
        }

        @Test
        void noSessionsMeansEmptyMap() {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            GameState loaded = repo().load();

            assertThat(loaded.sessions).isEmpty();
        }

        @Test
        void sessionWithDifferentMapSizesRoundTrips() {
            for (int size : new int[]{9, 11, 13, 15, 17}) {
                GameRepository r = repo();
                r.save(session(List.of(fullHero()), "Aria", size, List.of()));
                assertThat(r.load().sessions.get("Aria").mapSize).isEqualTo(size);
            }
        }

        @Test
        void sessionWhoseHeroIsNotInRosterIsDropped() {
            // "Ghost" is not on the roster, so its session is discarded.
            repo().save(session(List.of(fullHero()), "Ghost", 11, List.of()));

            GameState loaded = repo().load();
            assertThat(loaded.sessions).isEmpty();
            assertThat(loaded.roster).extracting(Hero::getName).containsExactly("Aria");
        }

        @Test
        void multipleSessionsAreAllPreservedAndIndependent() {
            GameState s = new GameState();
            s.roster = new ArrayList<>(List.of(
                    minimalHero("Aria"), minimalHero("Bronn"), minimalHero("Cass")));
            s.sessions.put("Aria",  new Session(9,  List.of(rat(1, 1))));
            s.sessions.put("Bronn", new Session(11, List.of(orc(2, 2), rat(3, 3))));
            s.sessions.put("Cass",  new Session(13, List.of()));

            repo().save(s);
            GameState loaded = repo().load();

            assertThat(loaded.sessions).containsOnlyKeys("Aria", "Bronn", "Cass");
            assertThat(loaded.sessions.get("Aria").villains).hasSize(1);
            assertThat(loaded.sessions.get("Bronn").villains).hasSize(2);
            assertThat(loaded.sessions.get("Cass").villains).isEmpty();
            assertThat(loaded.sessions.get("Aria").mapSize).isEqualTo(9);
            assertThat(loaded.sessions.get("Bronn").mapSize).isEqualTo(11);
            assertThat(loaded.sessions.get("Cass").mapSize).isEqualTo(13);
        }

        @Test
        void sessionOrderIsStableAcrossSaveLoad() {
            GameState s = new GameState();
            s.roster = new ArrayList<>(List.of(
                    minimalHero("Zed"), minimalHero("Aria"), minimalHero("Milo")));
            s.sessions.put("Zed",  new Session(9,  List.of()));
            s.sessions.put("Aria", new Session(11, List.of()));
            s.sessions.put("Milo", new Session(13, List.of()));

            repo().save(s);
            GameState loaded = repo().load();

            assertThat(loaded.sessions.keySet())
                    .containsExactly("Zed", "Aria", "Milo");
        }
    }

    /* ================================================================== */
    /*  File state handling                                                */
    /* ================================================================== */

    @Nested
    @DisplayName("file state")
    class FileState {

        @Test
        void missingFileLoadsEmptyState() {
            GameState loaded = repo().load();
            assertThat(loaded.roster).isEmpty();
            assertThat(loaded.sessions).isEmpty();
        }

        @Test
        void emptyFileLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "");
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void whitespaceOnlyFileLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "   \n\t\n");

            GameState loaded = repo().load();
            assertThat(loaded.roster).isEmpty();
            assertThat(loaded.sessions).isEmpty();
        }

        @Test
        void missingParentDirectoryIsCreatedOnSave() {
            Path nested = tempDir.resolve("a/b/c/heroes.json");
            GameRepository repo = new GameRepository(nested.toString());
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));

            assertThat(nested).exists();
            assertThat(repo.load().roster).hasSize(1);
        }

        @Test
        void saveProducesReadableUtf8() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String content = Files.readString(saveFile(), StandardCharsets.UTF_8);
            assertThat(content).contains("Aria");
        }

       @Test
        void saveFileIsUtf8TextWithExpectedHeader() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(2, 4))));
            String text = Files.readString(saveFile(), StandardCharsets.UTF_8);
            assertThat(text).startsWith("# swingy save file");
            assertThat(text).contains("[roster]");
            assertThat(text).contains("[sessions]");
        }

        @Test
        void savingDoesNotLeaveTempFileBehind() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));

            try (var entries = Files.list(tempDir)) {
                assertThat(entries.map(p -> p.getFileName().toString()))
                        .as("no .tmp leftovers")
                        .noneMatch(n -> n.endsWith(".tmp"));
            }
        }

        @Test
        void repositoryInNestedDirDoesNotBlowUpOnSave() {
            Path nested = tempDir.resolve("nested").resolve("heroes.json");
            new GameRepository(nested.toString())
                    .save(rosterOnly(List.of(minimalHero("Aria"))));
            assertThat(nested).exists();
        }
    }

    /* ================================================================== */
    /*  Corruption / robustness                                            */
    /* ================================================================== */

    @Nested
    @DisplayName("corrupt input")
    class Corruption {

        /** A minimal, valid roster entry for one hero named Aria. */
        private static String validAriaBlock() {
            return """
                    hero.0.name = Aria
                    hero.0.class = CONTACT_AGENT
                    hero.0.level = 1
                    hero.0.experience = 0
                    hero.0.hp = 300
                    """;
        }

        @Test
        void totallyGarbageFileIsRejected() throws IOException {
            Files.writeString(saveFile(), "{ this is not valid json");
            assertThatThrownBy(() -> repo().load())
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("not in swingy format");
        }

        @Test
        void goodHeroesSurviveAroundBadHero() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    hero.0.name = Aria
                    hero.0.class = CONTACT_AGENT
                    hero.0.level = 1
                    hero.0.experience = 0
                    hero.0.hp = 300

                    hero.1.name = Bronn
                    hero.1.class = NOT_A_CLASS
                    hero.1.level = 1
                    hero.1.experience = 0
                    hero.1.hp = 300

                    [sessions]
                    """);

            assertThat(repo().load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }

        @Test
        void goodVillainsSurviveAroundBadVillain() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    hero.0.name = Aria
                    hero.0.class = CONTACT_AGENT
                    hero.0.level = 1
                    hero.0.experience = 0
                    hero.0.hp = 300

                    [sessions]
                    session.0.hero = Aria
                    session.0.mapSize = 11
                    session.0.villain.0.name = Giant Rat
                    session.0.villain.0.hp = 20
                    session.0.villain.0.attack = 3
                    session.0.villain.0.defense = 1
                    session.0.villain.0.x = 2
                    session.0.villain.0.y = 4
                    session.0.villain.1.name = Broken
                    session.0.villain.1.hp = -1
                    session.0.villain.1.attack = 3
                    session.0.villain.1.defense = 1
                    session.0.villain.1.x = 3
                    session.0.villain.1.y = 3
                    session.0.villain.2.name = Orc
                    session.0.villain.2.hp = 70
                    session.0.villain.2.attack = 10
                    session.0.villain.2.defense = 5
                    session.0.villain.2.x = 7
                    session.0.villain.2.y = 8
                    """);

            GameState loaded = repo().load();
            assertThat(loaded.sessions).containsKey("Aria");
            assertThat(loaded.sessions.get("Aria").villains)
                    .extracting(Villain::getName)
                    .containsExactly("Giant Rat", "Orc");
        }

        @Test
        void sessionWithInvalidMapSizeIsDropped() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    """ + validAriaBlock() + """
                    [sessions]
                    session.0.hero = Aria
                    session.0.mapSize = 0
                    """);

            GameState loaded = repo().load();
            assertThat(loaded.sessions).isEmpty();
            assertThat(loaded.roster).extracting(Hero::getName).containsExactly("Aria");
        }

        @Test
        void oneGoodSessionSurvivesAroundBadSession() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    """ + validAriaBlock() + """
                    hero.1.name = Bronn
                    hero.1.class = CONTACT_AGENT
                    hero.1.level = 1
                    hero.1.experience = 0
                    hero.1.hp = 300

                    [sessions]
                    session.0.hero = Aria
                    session.0.mapSize = 11
                    session.1.hero = Bronn
                    session.1.mapSize = -1
                    """);

            GameState loaded = repo().load();
            assertThat(loaded.sessions).containsOnlyKeys("Aria");
            assertThat(loaded.sessions.get("Aria").mapSize).isEqualTo(11);
        }

        @Test
        void unknownHeroClassIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    hero.0.name = Aria
                    hero.0.class = NOT_A_CLASS
                    hero.0.level = 1
                    hero.0.experience = 0
                    hero.0.hp = 10

                    [sessions]
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void negativeLevelIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    hero.0.name = Aria
                    hero.0.class = CONTACT_AGENT
                    hero.0.level = -1
                    hero.0.experience = 0
                    hero.0.hp = 10

                    [sessions]
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void unknownArtifactTypeIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    hero.0.name = Aria
                    hero.0.class = CONTACT_AGENT
                    hero.0.level = 1
                    hero.0.experience = 0
                    hero.0.hp = 10
                    hero.0.artifact.0.type = NOT_A_TYPE
                    hero.0.artifact.0.value = 5
                    hero.0.artifact.0.name = Mystery

                    [sessions]
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void malformedLineIsSkippedNotFatal() throws IOException {
            // A line without '=' should be skipped; the rest should load.
            Files.writeString(saveFile(), """
                    version = 1

                    [roster]
                    this line has no equals sign
                    """ + validAriaBlock() + """
                    [sessions]
                    """);

            assertThat(repo().load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }
    }

    /* ================================================================== */
    /*  Format contract                                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("on-disk format")
    class Format {

        private String readText() throws IOException {
            return Files.readString(saveFile(), StandardCharsets.UTF_8);
        }

        @Test
        void fileStartsWithCommentHeaderAndVersion() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String text = readText();

            assertThat(text).startsWith("# swingy save file");
            assertThat(text).contains("version = 1");
        }

        @Test
        void rosterSectionIsPresent() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            assertThat(readText()).contains("[roster]");
        }

        @Test
        void sessionsSectionIsPresentEvenWhenEmpty() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            assertThat(readText()).contains("[sessions]");
        }

        @Test
        void heroFieldsAreEncodedWithDottedKeys() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String text = readText();

            assertThat(text).contains("hero.0.name = Aria");
            assertThat(text).contains("hero.0.class = CONTACT_AGENT");
            assertThat(text).contains("hero.0.level = 3");
            assertThat(text).contains("hero.0.experience = 2500");
            assertThat(text).contains("hero.0.hp = 42");
            assertThat(text).contains("hero.0.x = 5");
            assertThat(text).contains("hero.0.y = 6");
        }

        @Test
        void artifactsAreEncodedAsIndexedTriples() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String text = readText();

            assertThat(text).contains("hero.0.artifact.0.type = WEAPON");
            assertThat(text).contains("hero.0.artifact.0.value = 7");
            assertThat(text).contains("hero.0.artifact.0.name = Runeblade");
            assertThat(text).contains("hero.0.artifact.1.type = ARMOR");
            assertThat(text).contains("hero.0.artifact.2.type = HELM");
        }

        @Test
        void missingPositionIsOmittedNotEncodedAsNull() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String text = readText();

            assertThat(text).doesNotContain("hero.0.x");
            assertThat(text).doesNotContain("hero.0.y");
        }

        @Test
        void sessionUsesHeroNameAsItsIdentity() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(2, 4))));
            String text = readText();

            assertThat(text).contains("session.0.hero = Aria");
            assertThat(text).contains("session.0.mapSize = 11");
            assertThat(text).contains("session.0.villain.0.name = Giant Rat");
            assertThat(text).contains("session.0.villain.0.hp = 20");
            assertThat(text).contains("session.0.villain.0.x = 2");
        }

        @Test
        void emptyVillainListProducesNoVillainLines() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of()));
            String text = readText();

            assertThat(text).contains("session.0.hero = Aria");
            assertThat(text).contains("session.0.mapSize = 11");
            assertThat(text).doesNotContain("session.0.villain.");
        }

        @Test
        void multipleHeroesGetSequentialIndices() throws IOException {
            repo().save(rosterOnly(List.of(
                    minimalHero("Aria"), minimalHero("Bronn"), minimalHero("Cass"))));
            String text = readText();

            assertThat(text).contains("hero.0.name = Aria");
            assertThat(text).contains("hero.1.name = Bronn");
            assertThat(text).contains("hero.2.name = Cass");
        }

        @Test
        void commentsAreToleratedOnLoad() throws IOException {
            // Hand-edit: inject a comment and a blank line, then reload.
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String original = readText();
            String edited = original.replace(
                    "[roster]",
                    "[roster]\n# this hero is the player's main\n");
            Files.writeString(saveFile(), edited, StandardCharsets.UTF_8);

            assertThat(repo().load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }

        @Test
        void unknownSectionsAreIgnored() throws IOException {
            // Forward-compat: a future version adds [settings]. Old code
            // should ignore it rather than choke.
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String original = readText();
            Files.writeString(saveFile(),
                    original + "\n[settings]\nvolume = 7\n",
                    StandardCharsets.UTF_8);

            assertThat(repo().load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }

        @Test
        void savingWithSameNameAcrossDifferentFilesIsIsolated() {
            GameRepository a = new GameRepository(tempDir.resolve("a.txt").toString());
            GameRepository b = new GameRepository(tempDir.resolve("b.txt").toString());

            a.save(rosterOnly(List.of(minimalHero("Aria"))));
            b.save(rosterOnly(List.of(minimalHero("Bronn"))));

            assertThat(a.load().roster).extracting(Hero::getName).containsExactly("Aria");
            assertThat(b.load().roster).extracting(Hero::getName).containsExactly("Bronn");
        }

        @Test
        void escapingRoundTripsSpecialCharacters() {
            // An artifact name with a backslash and a hero name with a space —
            // both go through escape() on save and unescape() on load.
            Hero h = new Hero.HeroBuilder()
                    .name("Ann Marie")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .artifacts(List.of(
                            new Artifact(ArtifactType.WEAPON, 7, "Blade\\of\\Doom")))
                    .build();
            repo().save(rosterOnly(List.of(h)));
            Hero loaded = repo().load().roster.get(0);

            assertThat(loaded.getName()).isEqualTo("Ann Marie");
            assertThat(loaded.getArtifacts().get(0).getName())
                    .isEqualTo("Blade\\of\\Doom");
        }
    }

    /* ================================================================== */
    /*  I/O failure modes                                                  */
    /* ================================================================== */

    @Nested
    @DisplayName("I/O failures")
    class IoFailures {

        @Test
        void unwritableDestinationThrowsRepositoryException() throws IOException {
            Path blocker = tempDir.resolve("blocker");
            Files.writeString(blocker, "x");
            Path target = blocker.resolve("heroes.json");

            GameRepository repo = new GameRepository(target.toString());
            assertThatThrownBy(() -> repo.save(rosterOnly(List.of(minimalHero("Aria")))))
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("Failed to write");
        }

        @Test
        void unreadableSaveFileThrowsRepositoryException() throws IOException {
            Path dir = tempDir.resolve("heroes.json");
            Files.createDirectories(dir);

            GameRepository repo = new GameRepository(dir.toString());
            assertThatThrownBy(repo::load)
                    .isInstanceOf(RepositoryException.class);
        }
    }
}