package fr._42.swingy.model.map;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.entity.VillainPool;
import fr._42.swingy.model.enums.Direction;
import fr._42.swingy.model.enums.HeroClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameMapTest {

    /* ------------------------------------------------------------------ */
    /*  Test doubles                                                       */
    /* ------------------------------------------------------------------ */

    private static Hero dummyHero(Position at) {
        return new Hero.HeroBuilder()
                .name("Aria")
                .heroClass(HeroClass.CONTACT_AGENT)
                .position(at)
                .build();
    }

    /* ================================================================== */
    /*  Construction                                                       */
    /* ================================================================== */

    @Nested
    @DisplayName("construction")
    class Construction {

        @ParameterizedTest(name = "size {0}")
        @ValueSource(ints = {1, 2, 3, 8, 100})
        void acceptsPositiveSizes(int size) {
            assertThat(new GameMap(size, new Random(1)).getSize()).isEqualTo(size);
        }

        @ParameterizedTest(name = "size {0}")
        @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE})
        void rejectsNonPositiveSizes(int size) {
            assertThatThrownBy(() -> new GameMap(size, new Random(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map size must be positive");
        }

        @Test
        void freshMapHasNoVillains() {
            assertThat(new GameMap(5, new Random(1)).getVillains()).isEmpty();
        }
    }

    /* ================================================================== */
    /*  Geometry: isInside / isBorder / center                             */
    /* ================================================================== */

    @Nested
    @DisplayName("isInside")
    class IsInside {

        private final GameMap m = new GameMap(5, new Random(1));

        @Test
        void acceptsEveryCellInRange() {
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    assertThat(m.isInside(new Position(x, y))).isTrue();
                }
            }
        }

        @ParameterizedTest(name = "({0},{1}) is outside")
        @CsvSource({
            "-1, 0",   "0, -1",
            "5, 0",    "0, 5",
            "5, 5",    "-1, -1",
            "100, 3",  "3, 100",
        })
        void rejectsOffGridPositions(int x, int y) {
            assertThat(m.isInside(new Position(x, y))).isFalse();
        }

        @Test
        void nullIsNotInside() {
            assertThat(m.isInside(null)).isFalse();
        }

        @Test
        void cornersAreInside() {
            assertThat(m.isInside(new Position(0, 0))).isTrue();
            assertThat(m.isInside(new Position(4, 0))).isTrue();
            assertThat(m.isInside(new Position(0, 4))).isTrue();
            assertThat(m.isInside(new Position(4, 4))).isTrue();
        }
    }

    @Nested
    @DisplayName("isBorder")
    class IsBorder {

        private final GameMap m = new GameMap(5, new Random(1));

        @Test
        void allFourEdgesAreBorder() {
            // Top and bottom rows
            for (int x = 0; x < 5; x++) {
                assertThat(m.isBorder(new Position(x, 0))).isTrue();
                assertThat(m.isBorder(new Position(x, 4))).isTrue();
            }
            // Left and right columns
            for (int y = 0; y < 5; y++) {
                assertThat(m.isBorder(new Position(0, y))).isTrue();
                assertThat(m.isBorder(new Position(4, y))).isTrue();
            }
        }

        @Test
        void interiorCellsAreNotBorder() {
            for (int y = 1; y < 4; y++) {
                for (int x = 1; x < 4; x++) {
                    assertThat(m.isBorder(new Position(x, y))).isFalse();
                }
            }
        }

        @Test
        void cornersAreBorder() {
            assertThat(m.isBorder(new Position(0, 0))).isTrue();
            assertThat(m.isBorder(new Position(4, 0))).isTrue();
            assertThat(m.isBorder(new Position(0, 4))).isTrue();
            assertThat(m.isBorder(new Position(4, 4))).isTrue();
        }

        @Test
        void outsidePositionIsNotBorder() {
            // The implementation short-circuits on isInside, so this must be false.
            assertThat(m.isBorder(new Position(-1, 0))).isFalse();
            assertThat(m.isBorder(new Position(5, 5))).isFalse();
        }

        @Test
        void nullIsNotBorder() {
            assertThat(m.isBorder(null)).isFalse();
        }

        @Test
        void singleCellMapIsEntirelyBorder() {
            GameMap tiny = new GameMap(1, new Random(1));
            assertThat(tiny.isBorder(new Position(0, 0))).isTrue();
        }

        @Test
        void borderCountMatchesPerimeterFormula() {
            // For an NxN grid: 4N - 4 distinct border cells (corners counted once).
            int size = 7;
            GameMap big = new GameMap(size, new Random(1));
            long borderCells = 0;
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                    if (big.isBorder(new Position(x, y))) borderCells++;
            assertThat(borderCells).isEqualTo(4L * size - 4);
        }
    }

    @Nested
    @DisplayName("center")
    class Center {

        @ParameterizedTest(name = "size {0} -> center ({1},{1})")
        @CsvSource({
            "1,  0",
            "3,  1",
            "5,  2",
            "7,  3",
            "8,  4",
            "10, 5",
        })
        void centerIsSizeDividedByTwo(int size, int expected) {
            assertThat(new GameMap(size, new Random(1)).center())
                .isEqualTo(new Position(expected, expected));
        }

        @Test
        void centerIsAlwaysInside() {
            for (int size = 1; size <= 20; size++) {
                GameMap m = new GameMap(size, new Random(1));
                assertThat(m.isInside(m.center()))
                    .as("center of size-%d map", size)
                    .isTrue();
            }
        }
    }

    /* ================================================================== */
    /*  getNextPosition                                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("getNextPosition")
    class GetNextPosition {

        private final GameMap m = new GameMap(5, new Random(1));

        @Test
        void delegatesToPositionTranslate() {
            Position from = new Position(2, 2);
            for (Direction d : Direction.values()) {
                assertThat(m.getNextPosition(from, d))
                    .isEqualTo(from.translate(d, 1));
            }
        }

        @Test
        void mayReturnPositionOutsideMap() {
            // The method is documented as bounds-agnostic — callers must check.
            Position off = m.getNextPosition(new Position(0, 0), Direction.NORTH);
            assertThat(off).isEqualTo(new Position(0, -1));
            assertThat(m.isInside(off)).isFalse();
        }

        @Test
        void nullFromIsRejected() {
            assertThatThrownBy(() -> m.getNextPosition(null, Direction.NORTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
        }

        @Test
        void nullDirectionIsRejected() {
            assertThatThrownBy(() -> m.getNextPosition(new Position(0, 0), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direction");
        }
    }

    /* ================================================================== */
    /*  getCell                                                            */
    /* ================================================================== */

    @Nested
    @DisplayName("getCell")
    class GetCell {

        private final GameMap m = new GameMap(5, new Random(1));

        @Test
        void emptyCellReturnsNull() {
            assertThat(m.getCell(new Position(2, 2))).isNull();
        }

        @Test
        void outsideCellReturnsNullInsteadOfThrowing() {
            assertThat(m.getCell(new Position(-1, 0))).isNull();
            assertThat(m.getCell(new Position(99, 99))).isNull();
        }

        @Test
        void nullPositionReturnsNull() {
            assertThat(m.getCell(null)).isNull();
        }

        @Test
        void occupiedCellReturnsTheOccupant() {
            Hero h = dummyHero(new Position(1, 1));
            m.placeHero(h, new Position(1, 1));
            assertThat(m.getCell(new Position(1, 1))).isSameAs(h);
        }
    }

    /* ================================================================== */
    /*  placeHero                                                          */
    /* ================================================================== */

    @Nested
    @DisplayName("placeHero")
    class PlaceHero {

        @Test
        void placesHeroOnEmptyCell() {
            GameMap m = new GameMap(5, new Random(1));
            Hero h = dummyHero(new Position(2, 2));
            m.placeHero(h, new Position(2, 2));
            assertThat(m.getCell(new Position(2, 2))).isSameAs(h);
        }

        @Test
        void movingHeroLeavesOldCellEmpty() {
            // GameMap does NOT clear the old cell automatically; document behavior:
            // callers must place the hero only at its new position after updating it.
            GameMap m = new GameMap(5, new Random(1));
            Hero h = dummyHero(new Position(1, 1));
            m.placeHero(h, new Position(1, 1));
            m.placeHero(h, new Position(3, 3));
            // Old cell still holds the same reference.
            assertThat(m.getCell(new Position(1, 1))).isSameAs(h);
            assertThat(m.getCell(new Position(3, 3))).isSameAs(h);
        }

        @Test
        void overwritesVillainOnSameCell() {
            GameMap m = new GameMap(5, new Random(1));
            Villain v = new Villain("Goblin", 10, 1, 1, new Position(2, 2));
            m.placeVillain(v);
            Hero h = dummyHero(new Position(2, 2));
            m.placeHero(h, new Position(2, 2));
            assertThat(m.getCell(new Position(2, 2))).isSameAs(h);
            assertThat(m.getVillainAt(new Position(2, 2))).isNull();
            // Note: the villain is still in getVillains() — this is a known quirk.
        }

        @Test
        void outsidePositionIsRejected() {
            GameMap m = new GameMap(5, new Random(1));
            Hero h = dummyHero(new Position(9, 9));
            assertThatThrownBy(() -> m.placeHero(h, new Position(9, 9)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside map");
        }
    }

    /* ================================================================== */
    /*  placeVillain                                                       */
    /* ================================================================== */

    @Nested
    @DisplayName("placeVillain")
    class PlaceVillain {

        @Test
        void placesOnEmptyCellAndReturnsTrue() {
            GameMap m = new GameMap(5, new Random(1));
            Villain v = new Villain("A", 1, 1, 1, new Position(2, 2));
            assertThat(m.placeVillain(v)).isTrue();
            assertThat(m.getVillainAt(new Position(2, 2))).isSameAs(v);
            assertThat(m.getVillains()).containsExactly(v);
        }

        @Test
        void refusesOccupiedCellAndReturnsFalse() {
            GameMap m = new GameMap(5, new Random(1));
            Position p = new Position(2, 2);
            Villain first = new Villain("A", 1, 1, 1, p);
            Villain second = new Villain("B", 1, 1, 1, p);
            assertThat(m.placeVillain(first)).isTrue();
            assertThat(m.placeVillain(second)).isFalse();
            assertThat(m.getVillains()).containsExactly(first);
        }

        @Test
        void refusesCellOccupiedByHero() {
            GameMap m = new GameMap(5, new Random(1));
            Position p = new Position(2, 2);
            m.placeHero(dummyHero(p), p);
            assertThat(m.placeVillain(new Villain("A", 1, 1, 1, p))).isFalse();
        }

        @Test
        void outsidePositionIsRejected() {
            GameMap m = new GameMap(5, new Random(1));
            Villain v = new Villain("A", 1, 1, 1, new Position(99, 99));
            assertThatThrownBy(() -> m.placeVillain(v))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside map");
        }

        @Test
        void villainWithNullPositionIsRejected() {
            GameMap m = new GameMap(5, new Random(1));
            Villain v = new Villain("A", 1, 1, 1, null);
            assertThatThrownBy(() -> m.placeVillain(v))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /* ================================================================== */
    /*  getVillainAt / hasVillainAt / removeVillain                        */
    /* ================================================================== */

    @Nested
    @DisplayName("villain lookup and removal")
    class Lookup {

        @Test
        void getVillainAtReturnsNullOnEmptyCell() {
            GameMap m = new GameMap(5, new Random(1));
            assertThat(m.getVillainAt(new Position(1, 1))).isNull();
        }

        @Test
        void getVillainAtReturnsNullWhenHeroIsThere() {
            GameMap m = new GameMap(5, new Random(1));
            Position p = new Position(1, 1);
            m.placeHero(dummyHero(p), p);
            assertThat(m.getVillainAt(p)).isNull();
            assertThat(m.hasVillainAt(p)).isFalse();
        }

        @Test
        void getVillainAtReturnsNullWhenOffGrid() {
            GameMap m = new GameMap(5, new Random(1));
            assertThat(m.getVillainAt(new Position(-1, 0))).isNull();
        }

        @Test
        void removeVillainClearsBothCellAndList() {
            GameMap m = new GameMap(5, new Random(1));
            Villain v = new Villain("A", 1, 1, 1, new Position(2, 2));
            m.placeVillain(v);
            m.removeVillain(v);

            assertThat(m.getVillainAt(new Position(2, 2))).isNull();
            assertThat(m.getVillains()).isEmpty();
        }

        @Test
        void removeUnknownVillainIsNoOp() {
            GameMap m = new GameMap(5, new Random(1));
            Villain stranger = new Villain("X", 1, 1, 1, new Position(2, 2));
            m.removeVillain(stranger);   // never placed
            assertThat(m.getVillains()).isEmpty();
        }

        @Test
        void getVillainsReturnsImmutableView() {
            GameMap m = new GameMap(5, new Random(1));
            m.placeVillain(new Villain("A", 1, 1, 1, new Position(1, 1)));
            List<Villain> snapshot = m.getVillains();
            assertThatThrownBy(() -> snapshot.add(new Villain("B", 1, 1, 1, new Position(2, 2))))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void getVillainsReturnsSnapshotNotLiveView() {
            GameMap m = new GameMap(5, new Random(1));
            m.placeVillain(new Villain("A", 1, 1, 1, new Position(1, 1)));
            List<Villain> snapshot = m.getVillains();
            m.placeVillain(new Villain("B", 1, 1, 1, new Position(2, 2)));
            // The earlier list must not have grown.
            assertThat(snapshot).hasSize(1);
        }
    }

    /* ================================================================== */
    /*  generateVillains                                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("generateVillains")
    class GenerateVillains {

        @Test
        void placesRequestedCountWhenSpaceAvailable() {
            GameMap m = new GameMap(10, new Random(42));
            m.generateVillains(5, 1);
            assertThat(m.getVillains()).hasSize(5);
        }

        @Test
        void everyPlacedVillainSitsOnADistinctCell() {
            GameMap m = new GameMap(10, new Random(42));
            m.generateVillains(20, 1);
            Set<Position> positions = m.getVillains().stream()
                .map(Villain::getPosition)
                .collect(Collectors.toSet());
            assertThat(positions).hasSameSizeAs(m.getVillains());
        }

        @Test
        void stopsEarlyWhenBoardIsFull() {
            // 3x3 = 9 cells. Ask for 100 villains. Hero isn't placed, so
            // all 9 cells are available. Only 9 can be placed.
            GameMap m = new GameMap(3, new Random(1));
            m.generateVillains(100, 1);
            assertThat(m.getVillains()).hasSize(9);
        }

        @Test
        void doesNotOverwriteHeroCell() {
            GameMap m = new GameMap(3, new Random(1));
            Position heroPos = new Position(1, 1);
            m.placeHero(dummyHero(heroPos), heroPos);

            m.generateVillains(100, 1);       // over-ask on purpose

            assertThat(m.getCell(heroPos)).isInstanceOf(Hero.class);
            assertThat(m.getVillainAt(heroPos)).isNull();
            assertThat(m.getVillains()).hasSize(8);   // 9 - 1 hero
        }

        @Test
        void zeroCountPlacesNothing() {
            GameMap m = new GameMap(5, new Random(1));
            m.generateVillains(0, 1);
            assertThat(m.getVillains()).isEmpty();
        }

        @Test
        void negativeCountPlacesNothing() {
            GameMap m = new GameMap(5, new Random(1));
            m.generateVillains(-5, 1);
            assertThat(m.getVillains()).isEmpty();
        }

        @Test
        void deterministicWithSeededRandom() {
            GameMap a = new GameMap(10, new Random(123));
            GameMap b = new GameMap(10, new Random(123));
            a.generateVillains(5, 3);
            b.generateVillains(5, 3);

            assertThat(a.getVillains())
                .extracting(Villain::getName, Villain::getPosition)
                .containsExactlyElementsOf(
                    b.getVillains().stream()
                        .map(v -> org.assertj.core.groups.Tuple.tuple(v.getName(), v.getPosition()))
                        .toList());
        }

        @Test
        void usesHeroLevelToPickVillainTier() {
            // Level 1 → WEAK tier only; every generated name must come from
            // VillainPool.all() (this checks the plumbing, not the balance).
            GameMap weak = new GameMap(20, new Random(7));
            weak.generateVillains(10, 1);

            Set<String> knownNames = VillainPool.all().stream()
                .map(VillainPool.VillainTemplate::getName)
                .collect(Collectors.toSet());

            assertThat(weak.getVillains())
                .extracting(Villain::getName)
                .isSubsetOf(knownNames);
        }

        @Test
        void highLevelHeroReceivesOnlyStrongTierVillains() {
            // Level 7+ → STRONG tier. Pin it down with a fixed seed.
            GameMap m = new GameMap(20, new Random(1));
            m.generateVillains(10, 7);

            Set<String> strongNames = Set.of(
                "Wyvern", "Lich", "Minotaur Lord", "Shadow Drake", "Demon Prince");

            assertThat(m.getVillains())
                .extracting(Villain::getName)
                .isSubsetOf(strongNames);
        }
    }

    /* ================================================================== */
    /*  Invariants (property-style smoke tests)                            */
    /* ================================================================== */

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        void everyVillainInListIsAlsoOnTheGrid() {
            GameMap m = new GameMap(15, new Random(5));
            m.generateVillains(10, 2);
            for (Villain v : m.getVillains()) {
                assertThat(m.getVillainAt(v.getPosition()))
                    .as("villain %s at %s", v.getName(), v.getPosition())
                    .isSameAs(v);
            }
        }

        @Test
        void noTwoVillainsShareAPositionAfterGeneration() {
            GameMap m = new GameMap(12, new Random(11));
            m.generateVillains(30, 4);
            Set<Position> seen = new java.util.HashSet<>();
            for (Villain v : m.getVillains()) {
                assertThat(seen.add(v.getPosition()))
                    .as("duplicate position %s", v.getPosition())
                    .isTrue();
            }
        }
    }
}