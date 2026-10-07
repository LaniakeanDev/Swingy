package fr._42.swingy.enums;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import fr._42.swingy.model.enums.HeroClass;

import static org.assertj.core.api.Assertions.assertThat;

class HeroClassTest {

    /* ================================================================== */
    /*  fromString                                                         */
    /* ================================================================== */

    @Nested
    @DisplayName("fromString")
    class FromString {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "CULTURE_CITIZEN, CULTURE_CITIZEN",
                "CONTACT_AGENT,   CONTACT_AGENT",
                "SC_AGENT,        SC_AGENT",
                "DRONE,           DRONE",
                "GCU,             GCU",
                "GSV,             GSV",
                "CONTRACTOR,      CONTRACTOR",
                "REFERER,         REFERER",
        })
        void acceptsCanonicalName(String input, HeroClass expected) {
            assertThat(HeroClass.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "culture_citizen, CULTURE_CITIZEN",
                "contact_agent,   CONTACT_AGENT",
                "sc_agent,        SC_AGENT",
        })
        void acceptsLowercaseCanonicalName(String input, HeroClass expected) {
            assertThat(HeroClass.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "'Culture Citizen', CULTURE_CITIZEN",
                "'Contact Agent',   CONTACT_AGENT",
                "'Sc Agent',        SC_AGENT",
                "'Drone',           DRONE",
                "'Gcu',             GCU",
                "'Gsv',             GSV",
                "'Contractor',      CONTRACTOR",
                "'Referer',         REFERER",
        })
        void acceptsDisplayName(String input, HeroClass expected) {
            // The display name is what the UI shows; users will type it back.
            assertThat(HeroClass.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\" {0} \" is trimmed")
        @ValueSource(strings = {" CONTACT_AGENT", "CONTACT_AGENT ", "  DRONE  "})
        void trimsWhitespace(String input) {
            assertThat(HeroClass.fromString(input)).isNotNull();
        }

        @Test
        void nullReturnsNull() {
            assertThat(HeroClass.fromString(null)).isNull();
        }

        @ParameterizedTest(name = "\"{0}\" is unknown")
        @ValueSource(strings = {
                "", " ", "NOT_A_CLASS", "CONTACTAGENT", "Agent",
                "CultureCitizen", "123",
        })
        void rejectsUnknownInput(String input) {
            assertThat(HeroClass.fromString(input)).isNull();
        }

        @Test
        void everyConstantIsParseableByNameAndDisplayName() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(HeroClass.fromString(hc.name())).isEqualTo(hc);
                assertThat(HeroClass.fromString(hc.displayName())).isEqualTo(hc);
            }
        }

        @Test
        void displayNameIsCaseInsensitive() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(HeroClass.fromString(hc.displayName().toLowerCase())).isEqualTo(hc);
                assertThat(HeroClass.fromString(hc.displayName().toUpperCase())).isEqualTo(hc);
            }
        }
    }

    /* ================================================================== */
    /*  displayName                                                        */
    /* ================================================================== */

    @Nested
    @DisplayName("displayName")
    class DisplayNameTests {

        @ParameterizedTest(name = "{0} -> \"{1}\"")
        @CsvSource({
                "CULTURE_CITIZEN, 'Culture Citizen'",
                "CONTACT_AGENT,   'Contact Agent'",
                "DRONE,           'Drone'",
                "CONTRACTOR,      'Contractor'",
                "REFERER,         'Referer'",
        })
        void multiWordClassesAreTitleCased(HeroClass hc, String expected) {
            assertThat(hc.displayName()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} -> \"{1}\"")
        @CsvSource({
                "SC_AGENT, 'SC Agent'",
                "GCU,      'GCU'",
                "GSV,      'GSV'",
        })
        void acronymsArePreservedInUpperCase(HeroClass hc, String expected) {
            // SC, GCU, GSV are 2-3 letter acronyms that must stay uppercase.
            assertThat(hc.displayName()).isEqualTo(expected);
        }

        @Test
        void everyClassNameIsNonBlank() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(hc.displayName()).isNotBlank();
            }
        }

        @Test
        void displayNameContainsNoUnderscore() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(hc.displayName()).doesNotContain("_");
            }
        }
    }

    /* ================================================================== */
    /*  Base stats                                                         */
    /* ================================================================== */

    @Nested
    @DisplayName("base stats")
    class BaseStats {

        @ParameterizedTest(name = "{0}: atk {1}, def {2}, hp {3}")
        @CsvSource({
                "CULTURE_CITIZEN, 10, 2, 300",
                "CONTACT_AGENT,   10, 2, 300",
                "SC_AGENT,        10, 2, 300",
                "DRONE,           10, 2, 300",
                "GCU,             10, 2, 300",
                "GSV,             10, 2, 300",
                "CONTRACTOR,      10, 2, 300",
                "REFERER,         10, 2, 300",
        })
        void everyClassHasTheSameBaseStats(HeroClass hc, int atk, int def, int hp) {
            // Currently all classes are identical. This test pins the
            // current design — if the intent is to differentiate classes,
            // update the @CsvSource rows individually.
            assertThat(hc.getBaseAttack()).isEqualTo(atk);
            assertThat(hc.getBaseDefense()).isEqualTo(def);
            assertThat(hc.getBaseHitPoints()).isEqualTo(hp);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(HeroClass.class)
        void baseStatsArePositive(HeroClass hc) {
            assertThat(hc.getBaseAttack()).isPositive();
            assertThat(hc.getBaseDefense()).isNotNegative();
            assertThat(hc.getBaseHitPoints()).isPositive();
        }

        @Test
        void noClassIsWeakerThanReferer() {
            // A design invariant that is easy to break accidentally: if
            // you differentiate classes, no class should start with
            // strictly less than a REFERER at anything.
            HeroClass referer = HeroClass.REFERER;
            for (HeroClass hc : HeroClass.values()) {
                assertThat(hc.getBaseAttack()).isGreaterThanOrEqualTo(referer.getBaseAttack());
                assertThat(hc.getBaseDefense()).isGreaterThanOrEqualTo(referer.getBaseDefense());
                assertThat(hc.getBaseHitPoints()).isGreaterThanOrEqualTo(referer.getBaseHitPoints());
            }
        }
    }

    /* ================================================================== */
    /*  Enum surface                                                       */
    /* ================================================================== */

    @Nested
    @DisplayName("enum surface")
    class Surface {

        @Test
        void hasExpectedNumberOfClasses() {
            // If this changes, verify the save format still works for
            // old save files (which use HeroClass.name()).
            assertThat(HeroClass.values()).hasSize(8);
        }

        @Test
        void everyConstantHasUniqueName() {
            java.util.Set<String> names = new java.util.HashSet<>();
            for (HeroClass hc : HeroClass.values()) {
                assertThat(names.add(hc.name())).isTrue();
            }
        }
    }
}
