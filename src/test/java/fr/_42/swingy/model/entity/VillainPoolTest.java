package fr._42.swingy.model.entity;

import fr._42.swingy.model.map.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class VillainPoolTest {

    /* ================================================================== */
    /*  Tier name sets — mirror VillainPool exactly                        */
    /* ================================================================== */

    private static final Set<String> WEAK_NAMES = Set.of(
            "Affront Soldier",
            "Querl Xoralundra",
            "Fwi-Song",
            "Mr. First",
            "The Ethnarch"
    );

    private static final Set<String> MID_NAMES = Set.of(
            "Bora Horza Gobuchul",
            "Major Quilan",
            "The Chairmaker",
            "Chelgrian priest",
            "Xoxarle"
    );

    private static final Set<String> STRONG_NAMES = Set.of(
            "Eccentric Mind",
            "Emperor-Regent Nicosar",
            "Idiran Special Ops",
            "Interesting Times Gang",
            "Excession"
    );

    /* ================================================================== */
    /*  Catalog invariants                                                 */
    /* ================================================================== */

    @Nested
    @DisplayName("catalog")
    class Catalog {

        @Test
        void allReturnsEveryTemplate() {
            // 5 weak + 5 mid + 5 strong
            assertThat(VillainPool.all()).hasSize(15);
        }

        @Test
        void everyTemplateHasNonBlankName() {
            assertThat(VillainPool.all())
                    .extracting(VillainPool.VillainTemplate::getName)
                    .doesNotContainNull()
                    .allMatch(n -> !n.isBlank());
        }

        @Test
        void everyTemplateHasPositiveHitPoints() {
            assertThat(VillainPool.all())
                    .allMatch(t -> t.getHitPoints() > 0);
        }

        @Test
        void everyTemplateHasPositiveAttack() {
            assertThat(VillainPool.all())
                    .allMatch(t -> t.getAttack() > 0);
        }

        @Test
        void everyTemplateHasNonNegativeDefense() {
            assertThat(VillainPool.all())
                    .allMatch(t -> t.getDefense() >= 0);
        }

        @Test
        void noDuplicateNames() {
            List<String> names = VillainPool.all().stream()
                    .map(VillainPool.VillainTemplate::getName)
                    .toList();
            assertThat(new HashSet<>(names)).hasSameSizeAs(names);
        }

        @Test
        void strongTierIsStrictlyStrongerThanWeak() {
            // Sanity: every strong villain should out-hit every weak one.
            // If a mid or strong entry has lower attack than a weak entry,
            // the tiering is inconsistent.
            int weakestStrongAttack = VillainPool.all().stream()
                    .filter(t -> STRONG_NAMES.contains(t.getName()))
                    .mapToInt(VillainPool.VillainTemplate::getAttack)
                    .min().orElseThrow();
            int strongestWeakAttack = VillainPool.all().stream()
                    .filter(t -> WEAK_NAMES.contains(t.getName()))
                    .mapToInt(VillainPool.VillainTemplate::getAttack)
                    .max().orElseThrow();

            assertThat(weakestStrongAttack).isGreaterThan(strongestWeakAttack);
        }

        @Test
        void tierNamesMatchThePoolContents() {
            // Guards against the test and the pool drifting apart again.
            Set<String> poolNames = VillainPool.all().stream()
                    .map(VillainPool.VillainTemplate::getName)
                    .collect(Collectors.toSet());

            Set<String> expected = new HashSet<>();
            expected.addAll(WEAK_NAMES);
            expected.addAll(MID_NAMES);
            expected.addAll(STRONG_NAMES);

            assertThat(poolNames).isEqualTo(expected);
        }
    }

    /* ================================================================== */
    /*  rollForHeroLevel                                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("rollForHeroLevel")
    class RollForHeroLevel {

        @Test
        void producesVillainAtRequestedPosition() {
            Position p = new Position(4, 5);
            Villain v = VillainPool.rollForHeroLevel(new Random(1), 1, p);
            assertThat(v.getPosition()).isEqualTo(p);
        }

        @ParameterizedTest(name = "hero level {0} -> WEAK tier")
        @ValueSource(ints = {1, 2, 3})
        void lowLevelsPickFromWeakTier(int heroLevel) {
            for (int seed = 0; seed < 50; seed++) {
                Villain v = VillainPool.rollForHeroLevel(
                        new Random(seed), heroLevel, new Position(0, 0));
                assertThat(WEAK_NAMES)
                        .as("seed %d", seed)
                        .contains(v.getName());
            }
        }

        @ParameterizedTest(name = "hero level {0} -> MID tier")
        @ValueSource(ints = {4, 5, 6})
        void midLevelsPickFromMidTier(int heroLevel) {
            for (int seed = 0; seed < 50; seed++) {
                Villain v = VillainPool.rollForHeroLevel(
                        new Random(seed), heroLevel, new Position(0, 0));
                assertThat(MID_NAMES)
                        .as("seed %d", seed)
                        .contains(v.getName());
            }
        }

        @ParameterizedTest(name = "hero level {0} -> STRONG tier")
        @ValueSource(ints = {7, 10, 100})
        void highLevelsPickFromStrongTier(int heroLevel) {
            for (int seed = 0; seed < 50; seed++) {
                Villain v = VillainPool.rollForHeroLevel(
                        new Random(seed), heroLevel, new Position(0, 0));
                assertThat(STRONG_NAMES)
                        .as("seed %d", seed)
                        .contains(v.getName());
            }
        }

        @Test
        void tierTransitionAtLevel4() {
            // Level 3 -> weak, level 4 -> mid. The boundary is the bug-prone spot.
            int seed = 1;
            Villain at3 = VillainPool.rollForHeroLevel(new Random(seed), 3, new Position(0, 0));
            Villain at4 = VillainPool.rollForHeroLevel(new Random(seed), 4, new Position(0, 0));

            assertThat(WEAK_NAMES).contains(at3.getName());
            assertThat(MID_NAMES).contains(at4.getName());
        }

        @Test
        void tierTransitionAtLevel7() {
            int seed = 1;
            Villain at6 = VillainPool.rollForHeroLevel(new Random(seed), 6, new Position(0, 0));
            Villain at7 = VillainPool.rollForHeroLevel(new Random(seed), 7, new Position(0, 0));

            assertThat(MID_NAMES).contains(at6.getName());
            assertThat(STRONG_NAMES).contains(at7.getName());
        }

        @Test
        void deterministicForSameSeed() {
            Position p = new Position(3, 3);
            Villain a = VillainPool.rollForHeroLevel(new Random(42), 5, p);
            Villain b = VillainPool.rollForHeroLevel(new Random(42), 5, p);

            assertThat(a.getName()).isEqualTo(b.getName());
            assertThat(a.getHitPoints()).isEqualTo(b.getHitPoints());
            assertThat(a.getAttack()).isEqualTo(b.getAttack());
        }

        @Test
        void everyWeakTemplateIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(VillainPool.rollForHeroLevel(rng, 1, new Position(0, 0)).getName());
            }
            assertThat(seen).isEqualTo(WEAK_NAMES);
        }

        @Test
        void everyMidTemplateIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(VillainPool.rollForHeroLevel(rng, 5, new Position(0, 0)).getName());
            }
            assertThat(seen).isEqualTo(MID_NAMES);
        }

        @Test
        void everyStrongTemplateIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(VillainPool.rollForHeroLevel(rng, 10, new Position(0, 0)).getName());
            }
            assertThat(seen).isEqualTo(STRONG_NAMES);
        }

        @Test
        void producedVillainMatchesTemplateStats() {
            // Roll and verify the created villain carries the template's numbers.
            Villain v = VillainPool.rollForHeroLevel(new Random(1), 1, new Position(0, 0));
            VillainPool.VillainTemplate template = VillainPool.all().stream()
                    .filter(t -> t.getName().equals(v.getName()))
                    .findFirst().orElseThrow();

            assertThat(v.getHitPoints()).isEqualTo(template.getHitPoints());
            assertThat(v.getAttack()).isEqualTo(template.getAttack());
            assertThat(v.getDefense()).isEqualTo(template.getDefense());
        }
    }

    /* ================================================================== */
    /*  VillainTemplate                                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("VillainTemplate")
    class TemplateTests {

        @Test
        void toVillainUsesRequestedPosition() {
            VillainPool.VillainTemplate t = VillainPool.all().get(0);
            Position p = new Position(9, 9);
            Villain v = t.toVillain(p);

            assertThat(v.getPosition()).isEqualTo(p);
            assertThat(v.getName()).isEqualTo(t.getName());
            assertThat(v.getHitPoints()).isEqualTo(t.getHitPoints());
            assertThat(v.getAttack()).isEqualTo(t.getAttack());
            assertThat(v.getDefense()).isEqualTo(t.getDefense());
        }

        @Test
        void toVillainProducesIndependentInstances() {
            VillainPool.VillainTemplate t = VillainPool.all().get(0);
            Villain a = t.toVillain(new Position(0, 0));
            Villain b = t.toVillain(new Position(1, 1));

            assertThat(a).isNotSameAs(b);
            a.takeDamage(9999);
            assertThat(b.getHitPoints()).isEqualTo(t.getHitPoints());
        }
    }
}