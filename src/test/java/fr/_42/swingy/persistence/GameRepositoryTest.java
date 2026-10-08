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
import java.util.List;
import java.util.Map;

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

    /** Build a state with a roster but no active hero and no sessions. */
    private static GameState rosterOnly(List<Hero> roster) {
        GameState s = new GameState();
        s.roster = new ArrayList<>(roster);
        s.activeHero = null;
        s.sessions = new java.util.LinkedHashMap<>();
        return s;
    }

    /** Build a state with a single active session for the given hero. */
    private static GameState session(
            List<Hero> roster, String activeHero, int mapSize, List<Villain> villains) {
        GameState s = rosterOnly(roster);
        s.activeHero = activeHero;
        s.sessions.put(activeHero, new Session(mapSize, new ArrayList<>(villains)));
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
            assertThat(state.activeHero).isNull();
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
            assertThat(loaded.activeHero).isEqualTo("Aria");
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
            s.activeHero = "Aria";
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
    /*  Session header and multi-session map                               */
    /* ================================================================== */

    @Nested
    @DisplayName("session header")
    class SessionHeader {

        @Test
        void activeHeroAndMapSizeSurvive() {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(1, 1))));
            GameState loaded = repo().load();

            assertThat(loaded.activeHero).isEqualTo("Aria");
            Session s = loaded.sessions.get("Aria");
            assertThat(s).isNotNull();
            assertThat(s.mapSize).isEqualTo(11);
        }

        @Test
        void absentSessionMeansNoActiveHeroAndNoSessions() {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            GameState loaded = repo().load();

            assertThat(loaded.activeHero).isNull();
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
            assertThat(loaded.activeHero).isNull();
            assertThat(loaded.roster).extracting(Hero::getName).containsExactly("Aria");
        }

        @Test
        void activeHeroPointingAtMissingRosterEntryIsCleared() {
            // Roster has Aria, but activeHero says Bronn. Sessions are empty.
            GameState s = rosterOnly(List.of(fullHero()));
            s.activeHero = "Bronn";

            repo().save(s);
            GameState loaded = repo().load();

            assertThat(loaded.activeHero).isNull();
            assertThat(loaded.roster).extracting(Hero::getName).containsExactly("Aria");
        }

        @Test
        void multipleSessionsAreAllPreservedAndIndependent() {
            GameState s = new GameState();
            s.roster = new ArrayList<>(List.of(
                    minimalHero("Aria"), minimalHero("Bronn"), minimalHero("Cass")));
            s.activeHero = "Bronn";
            s.sessions.put("Aria",  new Session(9,  List.of(rat(1, 1))));
            s.sessions.put("Bronn", new Session(11, List.of(orc(2, 2), rat(3, 3))));
            s.sessions.put("Cass",  new Session(13, List.of()));

            repo().save(s);
            GameState loaded = repo().load();

            assertThat(loaded.activeHero).isEqualTo("Bronn");
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
            assertThat(loaded.activeHero).isNull();
            assertThat(loaded.sessions).isEmpty();
        }

        @Test
        void emptyFileLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "");
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void whitespaceOnlyFileIsRejectedAsMalformedJson() throws IOException {
            Files.writeString(saveFile(), "   \n\t\n");
            assertThatThrownBy(() -> repo().load())
                    .isInstanceOf(RepositoryException.class);
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
        void saveFileIsValidJson() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(2, 4))));

            new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(saveFile().toFile());
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

        @Test
        void malformedJsonIsRejected() throws IOException {
            Files.writeString(saveFile(), "{ this is not valid json");
            assertThatThrownBy(() -> repo().load())
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("Failed to read");
        }

        @Test
        void wrongVersionIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 99,
                      "activeHero": null,
                      "roster": [],
                      "sessions": {}
                    }
                    """);
            assertThatThrownBy(() -> repo().load())
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("version");
        }

        @Test
        void goodHeroesSurviveAroundBadHero() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": null,
                      "roster": [
                        {
                          "name": "Aria",
                          "heroClass": "CONTACT_AGENT",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        },
                        {
                          "name": "Bronn",
                          "heroClass": "NOT_A_CLASS",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        }
                      ],
                      "sessions": {}
                    }
                    """);

            assertThat(repo().load().roster)
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }

        @Test
        void goodVillainsSurviveAroundBadVillain() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": "Aria",
                      "roster": [
                        {
                          "name": "Aria",
                          "heroClass": "CONTACT_AGENT",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        }
                      ],
                      "sessions": {
                        "Aria": {
                          "mapSize": 11,
                          "villains": [
                            { "name": "Giant Rat", "hp": 20, "attack": 3, "defense": 1, "x": 2, "y": 4 },
                            { "name": "Broken",    "hp": -1, "attack": 3, "defense": 1, "x": 3, "y": 3 },
                            { "name": "Orc",       "hp": 70, "attack": 10, "defense": 5, "x": 7, "y": 8 }
                          ]
                        }
                      }
                    }
                    """);

            assertThat(repo().load().sessions.get("Aria").villains)
                    .extracting(Villain::getName)
                    .containsExactly("Giant Rat", "Orc");
        }

        @Test
        void sessionWithInvalidMapSizeIsDropped() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": "Aria",
                      "roster": [
                        {
                          "name": "Aria",
                          "heroClass": "CONTACT_AGENT",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        }
                      ],
                      "sessions": {
                        "Aria": { "mapSize": 0, "villains": [] }
                      }
                    }
                    """);

            GameState loaded = repo().load();
            assertThat(loaded.sessions).isEmpty();
            // activeHero no longer has a session, so it's cleared.
            assertThat(loaded.activeHero).isEqualTo("Aria");
        }

        @Test
        void oneGoodSessionSurvivesAroundBadSession() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": "Aria",
                      "roster": [
                        {
                          "name": "Aria",
                          "heroClass": "CONTACT_AGENT",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        },
                        {
                          "name": "Bronn",
                          "heroClass": "CONTACT_AGENT",
                          "level": 1,
                          "experience": 0,
                          "currentHitPoints": 300,
                          "x": null, "y": null,
                          "artifacts": []
                        }
                      ],
                      "sessions": {
                        "Aria":  { "mapSize": 11, "villains": [] },
                        "Bronn": { "mapSize": -1, "villains": [] }
                      }
                    }
                    """);

            GameState loaded = repo().load();
            assertThat(loaded.sessions).containsOnlyKeys("Aria");
            assertThat(loaded.sessions.get("Aria").mapSize).isEqualTo(11);
        }

        @Test
        void unknownHeroClassIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": null,
                      "roster": [
                        { "name": "Aria", "heroClass": "NOT_A_CLASS", "level": 1,
                          "experience": 0, "currentHitPoints": 10, "x": null, "y": null,
                          "artifacts": [] }
                      ],
                      "sessions": {}
                    }
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void negativeLevelIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": null,
                      "roster": [
                        { "name": "Aria", "heroClass": "CONTACT_AGENT", "level": -1,
                          "experience": 0, "currentHitPoints": 10, "x": null, "y": null,
                          "artifacts": [] }
                      ],
                      "sessions": {}
                    }
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void unknownArtifactTypeIsRejected() throws IOException {
            Files.writeString(saveFile(), """
                    {
                      "version": 1,
                      "activeHero": null,
                      "roster": [
                        { "name": "Aria", "heroClass": "CONTACT_AGENT", "level": 1,
                          "experience": 0, "currentHitPoints": 10, "x": null, "y": null,
                          "artifacts": [
                            { "type": "NOT_A_TYPE", "value": 5, "name": "Mystery" }
                          ] }
                      ],
                      "sessions": {}
                    }
                    """);
            assertThat(repo().load().roster).isEmpty();
        }

        @Test
        void artifactNameWithHyphensRoundTrips() throws IOException {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .artifacts(List.of(
                            new Artifact(ArtifactType.WEAPON, 7, "Blade-of-Doom")))
                    .build();
            repo().save(rosterOnly(List.of(h)));

            assertThat(repo().load().roster.get(0).getArtifacts())
                    .extracting(Artifact::getName)
                    .containsExactly("Blade-of-Doom");
        }
    }

    /* ================================================================== */
    /*  Format contract                                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("on-disk format")
    class Format {

        private com.fasterxml.jackson.databind.JsonNode readTree() throws IOException {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(saveFile().toFile());
        }

        @Test
        void topLevelFieldsAreVersionActiveHeroRosterSessions() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            var names = new ArrayList<String>();
            readTree().fieldNames().forEachRemaining(names::add);

            assertThat(names)
                    .containsExactlyInAnyOrder(
                            "version", "activeHero", "roster", "sessions");
        }

        @Test
        void versionIsOne() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            assertThat(readTree().path("version").asInt()).isEqualTo(1);
        }

        @Test
        void activeHeroIsNullWhenNoSession() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            assertThat(readTree().path("activeHero").isNull()).isTrue();
        }

        @Test
        void sessionsIsAnObjectKeyedByHeroName() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(2, 4))));
            var sessions = readTree().path("sessions");

            assertThat(sessions.isObject()).isTrue();
            assertThat(sessions.has("Aria")).isTrue();
            var aria = sessions.path("Aria");
            assertThat(aria.path("mapSize").asInt()).isEqualTo(11);
            assertThat(aria.path("villains").isArray()).isTrue();
            assertThat(aria.path("villains")).hasSize(1);
        }

        @Test
        void sessionsIsEmptyObjectWhenNoSessions() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            var sessions = readTree().path("sessions");

            assertThat(sessions.isObject()).isTrue();
            assertThat(sessions).isEmpty();
        }

        @Test
        void missingPositionIsEncodedAsNull() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            var hero = readTree().path("roster").get(0);

            assertThat(hero.path("x").isNull()).isTrue();
            assertThat(hero.path("y").isNull()).isTrue();
        }

        @Test
        void emptyVillainListIsEncodedAsEmptyArray() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of()));
            var villains = readTree().path("sessions").path("Aria").path("villains");

            assertThat(villains.isArray()).isTrue();
            assertThat(villains).isEmpty();
        }

        @Test
        void artifactsAreEncodedAsArrayOfObjects() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            var artifacts = readTree().path("roster").get(0).path("artifacts");

            assertThat(artifacts.isArray()).isTrue();
            assertThat(artifacts).hasSize(3);
            assertThat(artifacts.get(0).path("type").asText()).isEqualTo("WEAPON");
            assertThat(artifacts.get(0).path("value").asInt()).isEqualTo(7);
            assertThat(artifacts.get(0).path("name").asText()).isEqualTo("Runeblade");
        }

        @Test
        void savingWithSameNameAcrossDifferentFilesIsIsolated() {
            GameRepository a = new GameRepository(tempDir.resolve("a.json").toString());
            GameRepository b = new GameRepository(tempDir.resolve("b.json").toString());

            a.save(rosterOnly(List.of(minimalHero("Aria"))));
            b.save(rosterOnly(List.of(minimalHero("Bronn"))));

            assertThat(a.load().roster).extracting(Hero::getName).containsExactly("Aria");
            assertThat(b.load().roster).extracting(Hero::getName).containsExactly("Bronn");
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