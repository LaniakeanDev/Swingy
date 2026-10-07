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

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactPoolTest {

    /* ================================================================== */
    /*  Catalog invariants                                                 */
    /* ================================================================== */

    @Nested
    @DisplayName("catalog")
    class Catalog {

        @Test
        void allReturnsEveryArtifact() {
            assertThat(ArtifactPool.all()).hasSize(16);   // 5 weak + 5 mid + 6 strong
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
                    .collect(java.util.stream.Collectors.toSet());
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
            Set<String> weak = Set.of(
                    "Rusty Dagger", "Wooden Shield", "Leather Cap",
                    "Cracked Wand", "Torn Cloak");
            Set<String> strong = Set.of(
                    "Runeblade", "Dragonscale", "Crown of Ages",
                    "Void Scepter", "Aegis of Dawn", "Helm of Kings");

            int weakestStrongValue = ArtifactPool.all().stream()
                    .filter(a -> strong.contains(a.getName()))
                    .mapToInt(Artifact::getValue).min().orElseThrow();
            int strongestWeakValue = ArtifactPool.all().stream()
                    .filter(a -> weak.contains(a.getName()))
                    .mapToInt(Artifact::getValue).max().orElseThrow();

            assertThat(weakestStrongValue).isGreaterThan(strongestWeakValue);
        }
    }

    /* ================================================================== */
    /*  rollForPower — tier boundaries                                     */
    /* ================================================================== */

    @Nested
    @DisplayName("rollForPower")
    class RollForPower {

        private static final Set<String> WEAK_NAMES = Set.of(
                "Rusty Dagger", "Wooden Shield", "Leather Cap",
                "Cracked Wand", "Torn Cloak");
        private static final Set<String> MID_NAMES = Set.of(
                "Steel Sword", "Chainmail Vest", "Iron Helm",
                "Oak Staff", "Studded Boots");
        private static final Set<String> STRONG_NAMES = Set.of(
                "Runeblade", "Dragonscale", "Crown of Ages",
                "Void Scepter", "Aegis of Dawn", "Helm of Kings");

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
        void overflowPowerFallsBackToStrongTier() {
            // Documented behaviour: power above the strong tier's range
            // still returns a strong-tier artifact.
            assertThat(STRONG_NAMES).contains(
                    ArtifactPool.rollForPower(new Random(1), Integer.MAX_VALUE).getName());
        }
    }
}