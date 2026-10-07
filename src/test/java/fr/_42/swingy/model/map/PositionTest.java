package fr._42.swingy.model.map;

import fr._42.swingy.model.enums.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PositionTest {

    /* ================================================================== */
    /*  Construction & getters                                             */
    /* ================================================================== */

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void storesXAndYAsGiven() {
            Position p = new Position(7, -3);
            assertThat(p.getX()).isEqualTo(7);
            assertThat(p.getY()).isEqualTo(-3);
        }

        @Test
        void allowsNegativeCoordinates() {
            // Negative coords are legal — getNextPosition() can produce them,
            // and GameMap.isInside() is the guard, not Position itself.
            assertThat(new Position(-1, -1).getX()).isEqualTo(-1);
        }

        @Test
        void allowsZeroCoordinates() {
            Position origin = new Position(0, 0);
            assertThat(origin.getX()).isZero();
            assertThat(origin.getY()).isZero();
        }

        @Test
        void allowsIntegerExtremes() {
            Position p = new Position(Integer.MAX_VALUE, Integer.MIN_VALUE);
            assertThat(p.getX()).isEqualTo(Integer.MAX_VALUE);
            assertThat(p.getY()).isEqualTo(Integer.MIN_VALUE);
        }
    }

    /* ================================================================== */
    /*  translate()                                                        */
    /* ================================================================== */

    @Nested
    @DisplayName("translate")
    class Translate {

        @ParameterizedTest(name = "{0} by {1} from (3,3) -> ({2},{3})")
        @CsvSource({
            // direction,  steps, expectedX, expectedY
            "NORTH,        1,     3,         2",
            "SOUTH,        1,     3,         4",
            "EAST,         1,     4,         3",
            "WEST,         1,     2,         3",
        })
        void oneStepInEachDirection(Direction dir, int steps, int ex, int ey) {
            assertThat(new Position(3, 3).translate(dir, steps))
                .isEqualTo(new Position(ex, ey));
        }

        @ParameterizedTest(name = "{0} by {1}")
        @CsvSource({
            "NORTH,  5,   3,  -2",
            "SOUTH,  5,   3,   8",
            "EAST,   5,   8,   3",
            "WEST,   5,  -2,   3",
        })
        void multipleSteps(Direction dir, int steps, int ex, int ey) {
            assertThat(new Position(3, 3).translate(dir, steps))
                .isEqualTo(new Position(ex, ey));
        }

        @ParameterizedTest
        @EnumSource(Direction.class)
        void zeroStepsReturnsSameCoordinates(Direction dir) {
            assertThat(new Position(3, 3).translate(dir, 0))
                .isEqualTo(new Position(3, 3));
        }

        @ParameterizedTest
        @EnumSource(Direction.class)
        void negativeStepsGoOppositeDirection(Direction dir) {
            Position start = new Position(3, 3);
            Position forward = start.translate(dir, 1);
            Position backward = start.translate(dir, -1);

            // The two neighbours must be distinct and symmetric.
            assertThat(forward).isNotEqualTo(backward);
            // And going back from `forward` by -1 returns to start.
            assertThat(forward.translate(dir, -1)).isEqualTo(start);
        }

        @ParameterizedTest
        @EnumSource(Direction.class)
        void translatingIsCommutativeAcrossDirections(Direction dir) {
            Position start = new Position(5, 5);
            // Moving `dir` twice by 1 must equal moving `dir` once by 2.
            assertThat(start.translate(dir, 1).translate(dir, 1))
                .isEqualTo(start.translate(dir, 2));
        }

        @Test
        void returnsNewInstanceAndDoesNotMutateReceiver() {
            Position original = new Position(3, 3);
            Position moved = original.translate(Direction.EAST, 1);

            assertThat(moved).isNotSameAs(original);
            assertThat(original).isEqualTo(new Position(3, 3));
            assertThat(moved).isEqualTo(new Position(4, 3));
        }

        @Test
        void oppositeDirectionsCancelOut() {
            Position start = new Position(2, 5);
            Position roundTrip = start
                    .translate(Direction.NORTH, 3)
                    .translate(Direction.SOUTH, 3);
            assertThat(roundTrip).isEqualTo(start);
        }

        @Test
        void fourDirectionsAroundStartAreDistinct() {
            Position start = new Position(0, 0);
            Set<Position> neighbours = new HashSet<>();
            for (Direction d : Direction.values()) {
                neighbours.add(start.translate(d, 1));
            }
            assertThat(neighbours)
                .hasSize(4)
                .doesNotContain(start);
        }

        @Test
        void canWalkOffTheMapGrid() {
            // Position itself has no concept of bounds — GameMap does.
            assertThat(new Position(0, 0).translate(Direction.NORTH, 1))
                .isEqualTo(new Position(0, -1));
        }
    }

    /* ================================================================== */
    /*  equals / hashCode                                                  */
    /* ================================================================== */

    @Nested
    @DisplayName("equals and hashCode")
    class Equality {

        @Test
        void equalCoordinatesAreEqual() {
            assertThat(new Position(1, 2)).isEqualTo(new Position(1, 2));
        }

        @Test
        void equalCoordinatesShareHashCode() {
            assertThat(new Position(1, 2))
                .hasSameHashCodeAs(new Position(1, 2));
        }

        @Test
        void differsOnX() {
            assertThat(new Position(1, 2)).isNotEqualTo(new Position(9, 2));
        }

        @Test
        void differsOnY() {
            assertThat(new Position(1, 2)).isNotEqualTo(new Position(1, 9));
        }

        @Test
        void xAndYAreNotInterchangeable() {
            // Guards against a buggy equals that compares {x,y} to {y,x}.
            assertThat(new Position(1, 2)).isNotEqualTo(new Position(2, 1));
        }

        @Test
        void reflexive() {
            Position p = new Position(4, 4);
            assertThat(p).isEqualTo(p);
        }

        @Test
        void symmetric() {
            Position a = new Position(4, 5);
            Position b = new Position(4, 5);
            assertThat(a.equals(b)).isEqualTo(b.equals(a));
        }

        @Test
        void transitive() {
            Position a = new Position(4, 5);
            Position b = new Position(4, 5);
            Position c = new Position(4, 5);
            assertThat(a).isEqualTo(b);
            assertThat(b).isEqualTo(c);
            assertThat(a).isEqualTo(c);
        }

        @Test
        void notEqualToNull() {
            assertThat(new Position(1, 2)).isNotEqualTo(null);
        }

        @Test
        void notEqualToUnrelatedType() {
            assertThat(new Position(1, 2)).isNotEqualTo("(1,2)");
        }

        @Test
        void hashSetDeduplicatesEqualPositions() {
            Set<Position> set = new HashSet<>();
            set.add(new Position(1, 1));
            set.add(new Position(1, 1));
            set.add(new Position(1, 2));
            assertThat(set).hasSize(2);
        }

        @Test
        void usableAsHashMapKey() {
            var map = new java.util.HashMap<Position, String>();
            map.put(new Position(3, 4), "north-east corner");
            assertThat(map).containsEntry(new Position(3, 4), "north-east corner");
        }
    }

    /* ================================================================== */
    /*  toString()                                                         */
    /* ================================================================== */

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        void producesCompactCoordinateString() {
            assertThat(new Position(3, 4)).hasToString("(3,4)");
        }

        @Test
        void handlesNegativeCoordinates() {
            assertThat(new Position(-1, -2)).hasToString("(-1,-2)");
        }

        @Test
        void handlesOrigin() {
            assertThat(new Position(0, 0)).hasToString("(0,0)");
        }
    }
}