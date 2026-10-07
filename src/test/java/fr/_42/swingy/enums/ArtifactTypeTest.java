package fr._42.swingy.enums;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactTypeTest {

    /* ================================================================== */
    /*  fromString                                                         */
    /* ================================================================== */

    @Nested
    @DisplayName("fromString")
    class FromString {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "WEAPON, WEAPON",
                "ARMOR,  ARMOR",
                "HELM,   HELM",
        })
        void acceptsCanonicalName(String input, ArtifactType expected) {
            assertThat(ArtifactType.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "weapon, WEAPON",
                "armor,  ARMOR",
                "helm,   HELM",
                "Weapon, WEAPON",
                "hElM,   HELM",
        })
        void acceptsAnyCase(String input, ArtifactType expected) {
            assertThat(ArtifactType.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\" {0} \" is trimmed")
        @ValueSource(strings = {" WEAPON", "WEAPON ", "  HELM  ", "\tARMOR\n"})
        void trimsWhitespace(String input) {
            assertThat(ArtifactType.fromString(input)).isNotNull();
        }

        @Test
        void nullReturnsNull() {
            assertThat(ArtifactType.fromString(null)).isNull();
        }

        @ParameterizedTest(name = "\"{0}\" is unknown")
        @ValueSource(strings = {
                "", " ", "shield", "boots", "WEAPONX", "0", "WEAP0N",
        })
        void rejectsUnknownInput(String input) {
            assertThat(ArtifactType.fromString(input)).isNull();
        }

        @Test
        void everyConstantIsParseable() {
            for (ArtifactType t : ArtifactType.values()) {
                assertThat(ArtifactType.fromString(t.name())).isEqualTo(t);
                assertThat(ArtifactType.fromString(t.name().toLowerCase())).isEqualTo(t);
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
                "WEAPON, 'Weapon'",
                "ARMOR,  'Armor'",
                "HELM,   'Helm'",
        })
        void returnsTitleCase(ArtifactType t, String expected) {
            assertThat(t.displayName()).isEqualTo(expected);
        }

        @Test
        void everyTypeHasNonBlankDisplayName() {
            for (ArtifactType t : ArtifactType.values()) {
                assertThat(t.displayName()).isNotBlank();
            }
        }
    }

    /* ================================================================== */
    /*  isCompatibleWith                                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("isCompatibleWith")
    class Compatibility {

        @Test
        void nullHeroClassIsNeverCompatible() {
            for (ArtifactType t : ArtifactType.values()) {
                assertThat(t.isCompatibleWith(null))
                        .as("%s with null class", t)
                        .isFalse();
            }
        }

        @ParameterizedTest(name = "WEAPON on {0}")
        @EnumSource(HeroClass.class)
        void weaponIsUniversal(HeroClass hc) {
            assertThat(ArtifactType.WEAPON.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "HELM on {0}")
        @EnumSource(HeroClass.class)
        void helmIsUniversal(HeroClass hc) {
            assertThat(ArtifactType.HELM.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "ARMOR on {0}")
        @EnumSource(value = HeroClass.class, names = {
                "CONTACT_AGENT", "SC_AGENT", "DRONE", "CONTRACTOR", "GCU", "GSV"
        })
        void armorWorksOnMartialClasses(HeroClass hc) {
            assertThat(ArtifactType.ARMOR.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "ARMOR rejected on {0}")
        @EnumSource(value = HeroClass.class, names = {
                "CULTURE_CITIZEN", "REFERER"
        })
        void armorRejectedOnCivilianClasses(HeroClass hc) {
            assertThat(ArtifactType.ARMOR.isCompatibleWith(hc)).isFalse();
        }

        @Test
        void everyHeroClassAcceptsSomething() {
            // Regression guard: a class that accepts nothing is unplayable.
            for (HeroClass hc : HeroClass.values()) {
                boolean anyCompatible = false;
                for (ArtifactType t : ArtifactType.values()) {
                    if (t.isCompatibleWith(hc)) {
                        anyCompatible = true;
                        break;
                    }
                }
                assertThat(anyCompatible)
                        .as("%s should accept at least one type", hc)
                        .isTrue();
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
        void hasExactlyThreeTypes() {
            assertThat(ArtifactType.values()).hasSize(3);
        }
    }
}