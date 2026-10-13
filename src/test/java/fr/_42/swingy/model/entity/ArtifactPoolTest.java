package fr._42.swingy.model.entity;

import fr._42.swingy.model.enums.ArtifactType;
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

class ArtifactPoolTest {

    /* ================================================================== */
    /*  Tier name sets — mirror ArtifactPool exactly                       */
    /* ================================================================== */

    private static final Set<String> WEAK_NAMES = Set.of(
            "Knife missile",
            "Combat Sarcophagus",
            "Cortex Cowl",
            "CREWS",
            "Ghost Cloak"
    );

    private static final Set<String> MID_NAMES = Set.of(
            "Monofilament Warp",
            "Gelfield Suit",
            "MindHalo",
            "Gridfire",
            "Displacer"
    );

    private static final Set<String> STRONG_NAMES = Set.of(
            "Effector",
            "Dragonscale",
            "Youth Therapy",
            "Nanohole Bomb",
            "Mind Protection",
            "Idiran Helmet"
    );

    /* ================================================================== */
    /*  Catalog invariants                                                 */
    /* ================================================================== */

    @Nested
    @DisplayName("catalog")
    class Catalog {

        @Test
        void allReturnsEveryArtifact() {
            // 5 weak + 5 mid + 6 strong
            assertThat(ArtifactPool.all()).hasSize(16);
        }

        @Test
        void everyArtifactHasPositiveValue() {
            assertThat(ArtifactPool.all()).allMatch(a -> a.getValue() > 0);
        }

        @Test
        void everyArtifactHasNonBlankName() {
            assertThat(ArtifactPool.all())
                    .extracting(Artifact::getName)
                    .doesNotContainNull()
                    .allMatch(n -> !n.isBlank());
        }

        @Test
        void everyArtifactTypeIsRepresented() {
            Set<ArtifactType> types = ArtifactPool.all().stream()
                    .map(Artifact::getType)
                    .collect(Collectors.toSet());
            assertThat(types).containsExactlyInAnyOrder(ArtifactType.values());
        }

        @Test
        void noDuplicateNames() {
            List<String> names = ArtifactPool.all().stream().map(Artifact::getName).toList();
            assertThat(new HashSet<>(names)).hasSameSizeAs(names);
        }

        @Test
        void noNameContainsSaveFileDelimiters() {
            // Artifact's own constructor enforces this, but the pool is
            // where a well-meaning addition would go wrong first.
            assertThat(ArtifactPool.all())
                    .extracting(Artifact::getName)
                    .allMatch(n -> !n.contains(":") && !n.contains(";") && !n.contains("|"));
        }

        @Test
        void strongTierIsStrictlyStrongerThanWeakTier() {
            // Weakest strong artifact vs strongest weak artifact.
            // A regression here means the tier boundaries don't reflect power.
            int weakestStrongValue = ArtifactPool.all().stream()
                    .filter(a -> STRONG_NAMES.contains(a.getName()))
                    .mapToInt(Artifact::getValue).min().orElseThrow();
            int strongestWeakValue = ArtifactPool.all().stream()
                    .filter(a -> WEAK_NAMES.contains(a.getName()))
                    .mapToInt(Artifact::getValue).max().orElseThrow();

            assertThat(weakestStrongValue).isGreaterThan(strongestWeakValue);
        }

        @Test
        void tierNamesMatchThePoolContents() {
            // Guards against the test and the pool drifting apart again.
            Set<String> poolNames = ArtifactPool.all().stream()
                    .map(Artifact::getName)
                    .collect(Collectors.toSet());

            Set<String> expected = new HashSet<>();
            expected.addAll(WEAK_NAMES);
            expected.addAll(MID_NAMES);
            expected.addAll(STRONG_NAMES);

            assertThat(poolNames).isEqualTo(expected);
        }
    }

    /* ================================================================== */
    /*  rollForPower — tier boundaries                                     */
    /* ================================================================== */

    @Nested
    @DisplayName("rollForPower")
    class RollForPower {

        @ParameterizedTest(name = "villain power {0} -> WEAK")
        @ValueSource(ints = {1, 2, 3})
        void lowPowerPicksFromWeakTier(int power) {
            for (int seed = 0; seed < 50; seed++) {
                Artifact a = ArtifactPool.rollForPower(new Random(seed), power);
                assertThat(WEAK_NAMES).as("seed %d", seed).contains(a.getName());
            }
        }

        @ParameterizedTest(name = "villain power {0} -> MID")
        @ValueSource(ints = {4, 5, 6, 7})
        void midPowerPicksFromMidTier(int power) {
            for (int seed = 0; seed < 50; seed++) {
                Artifact a = ArtifactPool.rollForPower(new Random(seed), power);
                assertThat(MID_NAMES).as("seed %d", seed).contains(a.getName());
            }
        }

        @ParameterizedTest(name = "villain power {0} -> STRONG")
        @ValueSource(ints = {8, 9, 15, 100})
        void highPowerPicksFromStrongTier(int power) {
            for (int seed = 0; seed < 50; seed++) {
                Artifact a = ArtifactPool.rollForPower(new Random(seed), power);
                assertThat(STRONG_NAMES).as("seed %d", seed).contains(a.getName());
            }
        }

        @Test
        void tierTransitionAtPower4() {
            // 3 -> weak, 4 -> mid.
            assertThat(WEAK_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), 3).getName());
            assertThat(MID_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), 4).getName());
        }

        @Test
        void tierTransitionAtPower8() {
            assertThat(MID_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), 7).getName());
            assertThat(STRONG_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), 8).getName());
        }

        @Test
        void deterministicForSameSeed() {
            Artifact a = ArtifactPool.rollForPower(new Random(42), 5);
            Artifact b = ArtifactPool.rollForPower(new Random(42), 5);
            assertThat(a.getName()).isEqualTo(b.getName());
            assertThat(a.getValue()).isEqualTo(b.getValue());
            assertThat(a.getType()).isEqualTo(b.getType());
        }

        @Test
        void everyTemplateInTierIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(ArtifactPool.rollForPower(rng, 1).getName());
            }
            assertThat(seen).isEqualTo(WEAK_NAMES);
        }

        @Test
        void everyMidTemplateIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(ArtifactPool.rollForPower(rng, 5).getName());
            }
            assertThat(seen).isEqualTo(MID_NAMES);
        }

        @Test
        void everyStrongTemplateIsReachable() {
            Set<String> seen = new HashSet<>();
            Random rng = new Random(0);
            for (int i = 0; i < 500; i++) {
                seen.add(ArtifactPool.rollForPower(rng, 10).getName());
            }
            assertThat(seen).isEqualTo(STRONG_NAMES);
        }

        @Test
        void overflowPowerFallsBackToStrongTier() {
            // Documented behaviour: power above the strong tier's range
            // still returns a strong-tier artifact.
            assertThat(STRONG_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), Integer.MAX_VALUE).getName());
        }
    }
}