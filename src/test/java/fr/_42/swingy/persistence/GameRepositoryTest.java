package fr._42.swingy.persistence;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRepositoryTest {

    @TempDir Path tempDir;

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private Path saveFile() {
        return tempDir.resolve("heroes.txt");
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

    private static GameRepository.GameState rosterOnly(List<Hero> roster) {
        return new GameRepository.GameState(roster, Optional.empty(), Optional.empty(), List.of());
    }

    private static GameRepository.GameState session(
            List<Hero> roster, String activeHero, int mapSize, List<Villain> villains) {
        return new GameRepository.GameState(
                roster, Optional.of(activeHero), Optional.of(mapSize), villains);
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
            GameRepository.GameState state = repo.load();

            assertThat(state.roster()).hasSize(1);
            Hero h = state.roster().get(0);

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
            // Attack/defense/HP are computed from level + artifacts, so a
            // reload that misses any of those would show up here.
            GameRepository repo = repo();
            Hero original = fullHero();
            int atk = original.getAttack();
            int def = original.getDefense();
            int hp  = original.getHitPoints();

            repo.save(rosterOnly(List.of(original)));
            Hero h = repo.load().roster().get(0);

            assertThat(h.getAttack()).isEqualTo(atk);
            assertThat(h.getDefense()).isEqualTo(def);
            assertThat(h.getHitPoints()).isEqualTo(hp);
        }

        @Test
        void heroWithNoPositionReloadsWithNullPosition() {
            Hero h = minimalHero("Aria");
            assertThat(h.getPosition()).isNull();

            repo().save(rosterOnly(List.of(h)));
            assertThat(repo().load().roster().get(0).getPosition()).isNull();
        }

        @Test
        void heroWithNoArtifactsReloadsEmpty() {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            assertThat(repo().load().roster().get(0).getArtifacts()).isEmpty();
        }

        @Test
        void multipleHeroesPreserveOrder() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(
                    minimalHero("Aria"),
                    minimalHero("Bronn"),
                    minimalHero("Cass"))));

            assertThat(repo.load().roster())
                    .extracting(Hero::getName)
                    .containsExactly("Aria", "Bronn", "Cass");
        }

        @Test
        void saveTwiceOverwritesRatherThanAppends() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));
            repo.save(rosterOnly(List.of(minimalHero("Bronn"))));

            assertThat(repo.load().roster())
                    .extracting(Hero::getName)
                    .containsExactly("Bronn");
        }

        @Test
        void savingEmptyRosterProducesEmptyState() {
            GameRepository repo = repo();
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));
            repo.save(rosterOnly(List.of()));

            GameRepository.GameState state = repo.load();
            assertThat(state.roster()).isEmpty();
            assertThat(state.activeHeroName()).isEmpty();
            assertThat(state.villains()).isEmpty();
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

            assertThat(repo.load().roster())
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

            assertThat(repo().load().roster().get(0).getArtifacts())
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

            assertThat(repo().load().roster().get(0).getArtifacts())
                    .extracting(Artifact::getName)
                    .containsExactly("Crown of Ages");
        }

        @ParameterizedTest(name = "name=\"{0}\"")
        @ValueSource(strings = {"Aria", "Bo B", "Ann Marie Smith", "X Y Z"})
        void validHeroNamesRoundTrip(String name) {
            Hero h = new Hero.HeroBuilder()
                    .name(name).heroClass(HeroClass.CONTACT_AGENT).build();
            repo().save(rosterOnly(List.of(h)));

            assertThat(repo().load().roster().get(0).getName()).isEqualTo(name);
        }
    }

    /* ================================================================== */
    /*  Villain round-trip                                                 */
    /* ================================================================== */

    @Nested
    @DisplayName("villain round-trip")
    class VillainRoundTrip {

        @Test
        void singleVillainSurvivesSaveLoad() {
            Villain original = orc(7, 8);

            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(original)));
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.villains()).hasSize(1);
            Villain v = loaded.villains().get(0);
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
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.villains())
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
            // An empty map is a valid session state — do not conflate with
            // "no session".
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of()));
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.villains()).isEmpty();
            assertThat(loaded.activeHeroName()).contains("Aria");
            assertThat(loaded.mapSize()).contains(11);
        }

        @Test
        void villainAtBorderRoundTrips() {
            Villain v = rat(0, 0);
            repo().save(session(List.of(fullHero()), "Aria", 5, List.of(v)));

            assertThat(repo().load().villains().get(0).getPosition())
                    .isEqualTo(new Position(0, 0));
        }

        @Test
        void villainNameWithSpacesRoundTrips() {
            Villain v = new Villain("Minotaur Lord", 160, 17, 12, new Position(3, 3));
            repo().save(session(List.of(fullHero()), "Aria", 20, List.of(v)));

            assertThat(repo().load().villains().get(0).getName())
                    .isEqualTo("Minotaur Lord");
        }

        @Test
        void manyVillainsSurviveInOrder() {
            List<Villain> villains = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                villains.add(rat(i % 10, i / 10));
            }
            repo().save(session(List.of(fullHero()), "Aria", 20, villains));

            assertThat(repo().load().villains())
                    .hasSize(15)
                    .extracting(Villain::getPosition)
                    .containsExactlyElementsOf(
                            villains.stream().map(Villain::getPosition).toList());
        }
    }

    /* ================================================================== */
    /*  Session header                                                     */
    /* ================================================================== */

    @Nested
    @DisplayName("session header")
    class SessionHeader {

        @Test
        void activeHeroAndMapSizeSurvive() {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(1, 1))));
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.activeHeroName()).contains("Aria");
            assertThat(loaded.mapSize()).contains(11);
        }

        @Test
        void absentSessionMeansNoActiveHeroAndNoMapSize() {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.activeHeroName()).isEmpty();
            assertThat(loaded.mapSize()).isEmpty();
            assertThat(loaded.villains()).isEmpty();
        }

        @Test
        void sessionWithDifferentMapSizesRoundTrips() {
            for (int size : new int[]{9, 11, 13, 15, 17}) {
                GameRepository r = repo();
                r.save(session(List.of(fullHero()), "Aria", size, List.of()));
                assertThat(r.load().mapSize()).contains(size);
            }
        }

        @Test
        void sessionWithoutMatchingRosterEntryIsStillLoaded() {
            // The repository persists names, not references. If the file says
            // the active hero is "Ghost" but no such hero exists in the roster,
            // the load still succeeds — the controller is responsible for
            // reconciling the two (see GameController.run).
            repo().save(session(List.of(fullHero()), "Ghost", 11, List.of()));
            GameRepository.GameState loaded = repo().load();

            assertThat(loaded.activeHeroName()).contains("Ghost");
            assertThat(loaded.roster()).extracting(Hero::getName).containsExactly("Aria");
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
            GameRepository.GameState loaded = repo().load();
            assertThat(loaded.roster()).isEmpty();
            assertThat(loaded.activeHeroName()).isEmpty();
            assertThat(loaded.mapSize()).isEmpty();
            assertThat(loaded.villains()).isEmpty();
        }

        @Test
        void emptyFileLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "");
            assertThat(repo().load().roster()).isEmpty();
        }

        @Test
        void fileWithOnlyCommentsLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "# header\n# another\n");
            assertThat(repo().load().roster()).isEmpty();
        }

        @Test
        void fileWithOnlyBlankLinesLoadsEmptyState() throws IOException {
            Files.writeString(saveFile(), "\n\n   \n\t\n");
            assertThat(repo().load().roster()).isEmpty();
        }

        @Test
        void missingParentDirectoryIsCreatedOnSave() {
            Path nested = tempDir.resolve("a/b/c/heroes.txt");
            GameRepository repo = new GameRepository(nested.toString());
            repo.save(rosterOnly(List.of(minimalHero("Aria"))));

            assertThat(nested).exists();
            assertThat(repo.load().roster()).hasSize(1);
        }

        @Test
        void saveProducesReadableUtf8() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String content = Files.readString(saveFile(), StandardCharsets.UTF_8);
            assertThat(content).contains("Aria");
        }

        @Test
        void saveWritesHeaderComment() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));

            List<String> lines = Files.readAllLines(saveFile());
            assertThat(lines).isNotEmpty();
            assertThat(lines.get(0)).startsWith("#");
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
            Path nested = tempDir.resolve("nested").resolve("heroes.txt");
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
        void malformedLineIsSkippedNotFatal() throws IOException {
            Files.writeString(saveFile(), "garbage line with no pipes\n");
            assertThat(repo().load().roster()).isEmpty();
        }
    @Test
    void goodHeroLinesSurviveAroundBadLines() throws IOException {
        GameRepository repo = repo();
        repo.save(rosterOnly(List.of(minimalHero("Aria"), minimalHero("Bronn"))));
        String good = Files.readString(saveFile());

        // Replace the second hero's level with a non-numeric value.
        // Level is at field index 3 in the H|... record. The full field
        // pattern for a minimal hero is:
        //   H|Bronn|CONTACT_AGENT|1|0|300|-1|-1|
        // Substring-replace just the level digit that follows Bronn's class.
        String corrupted = good.replace(
                "H|Bronn|CONTACT_AGENT|1|",
                "H|Bronn|CONTACT_AGENT|X|");
        Files.writeString(saveFile(), corrupted);

        assertThat(repo.load().roster())
                .extracting(Hero::getName)
                .containsExactly("Aria");
    }

        @ParameterizedTest(name = "line=\"{0}\"")
        @ValueSource(strings = {
                "H|Aria",                                             // too few fields
                "H|Aria|CONTACT_AGENT|3|2500|42|5",                   // 8 fields (one short)
                "H|Aria|CONTACT_AGENT|3|2500|42|5|6|WEAPON",          // bad artifact token
                "H|Aria|NOT_A_CLASS|3|2500|42|5|6|",                  // bad hero class
                "H|Aria|CONTACT_AGENT|x|2500|42|5|6|",                // non-numeric level
                "H|Aria|CONTACT_AGENT|0|2500|42|5|6|",                // level < 1
                "H|Aria|CONTACT_AGENT|3|-1|42|5|6|",                  // negative xp
                "H|Aria|CONTACT_AGENT|3|2500|-5|5|6|",                // negative HP
        })
        void malformedHeroVariantsAreSkipped(String badLine) throws IOException {
            Files.writeString(saveFile(),
                    "VERSION|1\n" + badLine + "\n");
            assertThat(repo().load().roster()).isEmpty();
        }

        @ParameterizedTest(name = "villain line=\"{0}\"")
        @ValueSource(strings = {
                "V|Giant Rat",                                  // too few
                "V|Giant Rat|20|3|1|2",                         // 6 fields (one short)
                "V|Giant Rat|-5|3|1|2|4",                       // negative hp
                "V|Giant Rat|20|0|1|2|4",                       // zero attack
                "V|Giant Rat|20|3|-1|2|4",                      // negative defense
                "V|Giant Rat|20|3|1|x|4",                       // non-numeric x
        })
        void malformedVillainVariantsAreSkipped(String badLine) throws IOException {
            Files.writeString(saveFile(),
                    "VERSION|1\n"
                  + "S|Aria|11\n"
                  + "H|Aria|CONTACT_AGENT|1|0|10|0|0|\n"
                  + badLine + "\n");

            GameRepository.GameState loaded = repo().load();
            assertThat(loaded.roster()).hasSize(1);   // hero survived
            assertThat(loaded.villains()).isEmpty();  // villain was dropped
        }

        @Test
        void goodVillainsSurviveAroundBadVillainLines() throws IOException {
            Files.writeString(saveFile(),
                    "VERSION|1\n"
                  + "S|Aria|11\n"
                  + "H|Aria|CONTACT_AGENT|1|0|10|0|0|\n"
                  + "V|Giant Rat|20|3|1|2|4\n"
                  + "V|garbage\n"
                  + "V|Orc Warrior|70|10|5|7|8\n");

            GameRepository.GameState loaded = repo().load();
            assertThat(loaded.villains())
                    .extracting(Villain::getName)
                    .containsExactly("Giant Rat", "Orc Warrior");
        }

        @Test
        void artifactTokenWithWrongArityIsSkipped() throws IOException {
            Files.writeString(saveFile(),
                    "VERSION|1\n"
                  + "H|Aria|CONTACT_AGENT|1|0|10|0|0|WEAPON:7\n");
            assertThat(repo().load().roster()).isEmpty();
        }

        @Test
        void unknownRecordTypeIsSkipped() throws IOException {
            Files.writeString(saveFile(),
                    "VERSION|1\n"
                  + "Q|whatever|something\n"
                  + "H|Aria|CONTACT_AGENT|1|0|10|0|0|\n");

            assertThat(repo().load().roster())
                    .extracting(Hero::getName)
                    .containsExactly("Aria");
        }

        @Test
        void unsupportedVersionRejectsThatLineButOthersLoad() throws IOException {
            // The VERSION mismatch throws inside the per-line try, so it is
            // skipped like any other malformed record. The remaining lines
            // still parse.
            Files.writeString(saveFile(),
                    "VERSION|99\n"
                  + "H|Aria|CONTACT_AGENT|1|0|10|0|0|\n");

            GameRepository.GameState loaded = repo().load();
            assertThat(loaded.roster()).hasSize(1);
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

            assertThat(repo().load().roster().get(0).getArtifacts())
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

        @Test
        void firstLineIsHeaderComment() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));

            List<String> lines = Files.readAllLines(saveFile());
            assertThat(lines).isNotEmpty();
            assertThat(lines.get(0)).startsWith("#");
        }

        @Test
        void secondLineIsVersionRecord() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));

            List<String> lines = Files.readAllLines(saveFile());
            assertThat(lines.get(1)).isEqualTo("VERSION|1");
        }

        @Test
        void sessionRecordComesBeforeHeroRecords() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(rat(2, 4))));

            List<String> lines = Files.readAllLines(saveFile());
            int sessionIdx = indexOfLineStartingWith(lines, "S|");
            int firstHeroIdx = indexOfLineStartingWith(lines, "H|");
            int firstVillainIdx = indexOfLineStartingWith(lines, "V|");

            assertThat(sessionIdx).isGreaterThan(0);
            assertThat(firstHeroIdx).isGreaterThan(sessionIdx);
            assertThat(firstVillainIdx).isGreaterThan(firstHeroIdx);
        }

        @Test
        void heroLineHasNineFieldsIncludingPrefix() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String heroLine = lineStartingWith(saveFile(), "H|");
            assertThat(heroLine.split("\\|", -1)).hasSize(9);
        }

        @Test
        void villainLineHasSevenFieldsIncludingPrefix() throws IOException {
            repo().save(session(List.of(fullHero()), "Aria", 11, List.of(orc(7, 8))));
            String villainLine = lineStartingWith(saveFile(), "V|");
            assertThat(villainLine.split("\\|", -1)).hasSize(7);
        }

        @Test
        void missingPositionIsEncodedAsMinusOne() throws IOException {
            repo().save(rosterOnly(List.of(minimalHero("Aria"))));
            String heroLine = lineStartingWith(saveFile(), "H|");
            String[] parts = heroLine.split("\\|", -1);

            assertThat(parts[6]).isEqualTo("-1");
            assertThat(parts[7]).isEqualTo("-1");
        }

        @Test
        void artifactsAreCommaSeparated() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String heroLine = lineStartingWith(saveFile(), "H|");
            String artifactField = heroLine.split("\\|", -1)[8];

            assertThat(artifactField.split(",")).hasSize(3);
        }

        @Test
        void eachArtifactIsEncodedAsTypeColonValueColonName() throws IOException {
            repo().save(rosterOnly(List.of(fullHero())));
            String heroLine = lineStartingWith(saveFile(), "H|");
            String artifactField = heroLine.split("\\|", -1)[8];

            assertThat(artifactField)
                    .contains("WEAPON:7:Runeblade")
                    .contains("ARMOR:5:Chainmail Vest")
                    .contains("HELM:3:Leather Cap");
        }

        @Test
        void savingWithSameNameAcrossDifferentFilesIsIsolated() {
            GameRepository a = new GameRepository(tempDir.resolve("a.txt").toString());
            GameRepository b = new GameRepository(tempDir.resolve("b.txt").toString());

            a.save(rosterOnly(List.of(minimalHero("Aria"))));
            b.save(rosterOnly(List.of(minimalHero("Bronn"))));

            assertThat(a.load().roster()).extracting(Hero::getName).containsExactly("Aria");
            assertThat(b.load().roster()).extracting(Hero::getName).containsExactly("Bronn");
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
            Path target = blocker.resolve("heroes.txt");

            GameRepository repo = new GameRepository(target.toString());
            assertThatThrownBy(() -> repo.save(rosterOnly(List.of(minimalHero("Aria")))))
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("Failed to write");
        }

        @Test
        void unreadableSaveFileThrowsRepositoryException() throws IOException {
            Path dir = tempDir.resolve("heroes.txt");
            Files.createDirectories(dir);

            GameRepository repo = new GameRepository(dir.toString());
            assertThatThrownBy(repo::load)
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("Failed to read");
        }
    }

    /* ================================================================== */
    /*  Helpers                                                            */
    /* ================================================================== */

    private static int indexOfLineStartingWith(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) return i;
        }
        return -1;
    }

    private static String lineStartingWith(Path file, String prefix) throws IOException {
        List<String> lines = Files.readAllLines(file);
        for (String line : lines) {
            if (line.startsWith(prefix)) return line;
        }
        throw new AssertionError("No line starting with '" + prefix + "' in " + file);
    }
}