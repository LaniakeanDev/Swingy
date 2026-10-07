package fr._42.swingy.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import fr._42.swingy.model.enums.Direction;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

    /* ================================================================== */
    /*  fromString — happy paths                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("fromString")
    class FromString {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "north, NORTH",
                "south, SOUTH",
                "east,  EAST",
                "west,  WEST",
        })
        void acceptsLowercase(String input, Direction expected) {
            assertThat(Direction.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "NORTH, NORTH",
                "SOUTH, SOUTH",
                "EAST,  EAST",
                "WEST,  WEST",
        })
        void acceptsUppercase(String input, Direction expected) {
            assertThat(Direction.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "North, NORTH",
                "SoUtH, SOUTH",
        })
        void acceptsMixedCase(String input, Direction expected) {
            assertThat(Direction.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\" {0} \" is trimmed")
        @ValueSource(strings = {" north", "north ", "  north  ", "\tnorth\n"})
        void trimsWhitespace(String input) {
            assertThat(Direction.fromString(input)).isEqualTo(Direction.NORTH);
        }

        @Test
        void nullReturnsNull() {
            assertThat(Direction.fromString(null)).isNull();
        }

        @ParameterizedTest(name = "\"{0}\" is not a direction")
        @ValueSource(strings = {
                "", " ", "n", "no", "nort", "northeast", "up",
                "north-east", "0", "NORTH1",
        })
        void rejectsUnknownInput(String input) {
            assertThat(Direction.fromString(input)).isNull();
        }

        @Test
        void everyEnumConstantIsParseable() {
            // Round-trip: for every Direction, fromString(d.name()) works.
            for (Direction d : Direction.values()) {
                assertThat(Direction.fromString(d.name())).isEqualTo(d);
                assertThat(Direction.fromString(d.name().toLowerCase())).isEqualTo(d);
            }
        }
    }
}