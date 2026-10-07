package fr._42.swingy.model.entity;

import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactTest {

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private static Artifact weapon(int value) {
        return new Artifact(ArtifactType.WEAPON, value, "Sword" + value);
    }

    /* ================================================================== */
    /*  Construction: happy path                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void storesEveryFieldAsGiven() {
            Artifact a = new Artifact(ArtifactType.WEAPON, 7, "Runeblade");

            assertThat(a.getType()).isEqualTo(ArtifactType.WEAPON);
            assertThat(a.getValue()).isEqualTo(7);
            assertThat(a.getName()).isEqualTo("Runeblade");
        }

        @ParameterizedTest(name = "type {0}")
        @EnumSource(ArtifactType.class)
        void acceptsEveryArtifactType(ArtifactType type) {
            Artifact a = new Artifact(type, 5, "Item");
            assertThat(a.getType()).isEqualTo(type);
        }

        @Test
        void acceptsValueOfOne() {
            assertThat(weapon(1).getValue()).isEqualTo(1);
        }

        @Test
        void acceptsLargeValue() {
            assertThat(weapon(9999).getValue()).isEqualTo(9999);
        }

        @Test
        void acceptsNameWithSpaces() {
            Artifact a = new Artifact(ArtifactType.HELM, 3, "Crown of Ages");
            assertThat(a.getName()).isEqualTo("Crown of Ages");
        }

        @Test
        void acceptsNameWithHyphens() {
            Artifact a = new Artifact(ArtifactType.WEAPON, 7, "Blade-of-Doom");
            assertThat(a.getName()).isEqualTo("Blade-of-Doom");
        }

        @Test
        void acceptsNameWithDigits() {
            Artifact a = new Artifact(ArtifactType.ARMOR, 5, "Shield2");
            assertThat(a.getName()).isEqualTo("Shield2");
        }

        @ParameterizedTest(name = "name \"{0}\"")
        @ValueSource(strings = {
                "A",                     // single char
                "Aa",
                "  Padded  ",            // surrounding whitespace preserved
                "A'B",
                "A.B",
                "A/B",
                "A\\B",
                "Ω",                     // non-ASCII
        })
        void acceptsVarietyOfNames(String name) {
            Artifact a = new Artifact(ArtifactType.WEAPON, 1, name);
            assertThat(a.getName()).isEqualTo(name);
        }
    }

    /* ================================================================== */
    /*  Construction: rejections                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("rejects invalid input")
    class Rejections {

        @Test
        void rejectsNullType() {
            assertThatThrownBy(() -> new Artifact(null, 5, "Sword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type must not be null");
        }

        @Test
        void rejectsZeroValue() {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 0, "Sword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be positive");
        }

        @Test
        void rejectsNegativeValue() {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, -5, "Sword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value must be positive");
        }

        @Test
        void rejectsNullName() {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 5, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must not be blank");
        }

        @ParameterizedTest(name = "blank name \"{0}\"")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void rejectsBlankName(String name) {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 5, name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must not be blank");
        }

        @ParameterizedTest(name = "name containing delimiter \"{0}\"")
        @ValueSource(strings = {
                "Runeblade|Evil",
                "Runeblade;Evil",
                "Runeblade:Evil",
                "|leading",
                "trailing|",
                "a:b:c",
        })
        void rejectsNamesWithSaveFileDelimiters(String name) {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 5, name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not contain");
        }

        @Test
        void messageListsAllThreeForbiddenCharacters() {
            // The error text mentions ':', ';' and '|' — pin it so a change
            // to the delimiters requires touching this test.
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 5, "a|b"))
                    .hasMessageContaining("':'")
                    .hasMessageContaining("';'")
                    .hasMessageContaining("'|'");
        }

        @Test
        void rejectionOrderTypeBeforeValue() {
            // Type is checked before value — passing both invalid should
            // report the type error.
            assertThatThrownBy(() -> new Artifact(null, 0, ""))
                    .hasMessageContaining("type must not be null");
        }

        @Test
        void rejectionOrderValueBeforeName() {
            assertThatThrownBy(() -> new Artifact(ArtifactType.WEAPON, 0, ""))
                    .hasMessageContaining("value must be positive");
        }
    }

    /* ================================================================== */
    /*  Field access                                                       */
    /* ================================================================== */

    @Nested
    @DisplayName("fields")
    class Fields {

        @Test
        void typeIsFinal() {
            assertThat(Artifact.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("type"))
                    .allMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void valueIsFinal() {
            assertThat(Artifact.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("value"))
                    .allMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void nameIsFinal() {
            assertThat(Artifact.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("name"))
                    .allMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void hasNoSetters() {
            assertThat(Artifact.class.getMethods())
                    .extracting(java.lang.reflect.Method::getName)
                    .noneMatch(n -> n.startsWith("set"));
        }
    }

    /* ================================================================== */
    /*  Equality                                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void artifactsWithSameFieldsAreNotEqual() {
            // Artifact doesn't override equals/hashCode. Two "identical"
            // artifacts are distinct references. Documents the fact so
            // a future equals override is a conscious decision.
            Artifact a = new Artifact(ArtifactType.WEAPON, 5, "Sword");
            Artifact b = new Artifact(ArtifactType.WEAPON, 5, "Sword");

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void sameInstanceIsEqualToItself() {
            Artifact a = weapon(5);
            assertThat(a).isEqualTo(a);
        }
    }

    /* ================================================================== */
    /*  Compatibility with hero classes                                    */
    /* ================================================================== */

    @Nested
    @DisplayName("isCompatibleWith")
    class Compatibility {

        @Test
        void nullHeroClassIsIncompatibleForEveryType() {
            for (ArtifactType t : ArtifactType.values()) {
                assertThat(t.isCompatibleWith(null))
                        .as("type %s with null class", t)
                        .isFalse();
            }
        }

        @ParameterizedTest(name = "weapon on {0}")
        @EnumSource(HeroClass.class)
        void weaponsWorkOnEveryClass(HeroClass hc) {
            assertThat(ArtifactType.WEAPON.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "helm on {0}")
        @EnumSource(HeroClass.class)
        void helmsWorkOnEveryClass(HeroClass hc) {
            assertThat(ArtifactType.HELM.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "armor on {0}")
        @EnumSource(value = HeroClass.class, names = {
                "CONTACT_AGENT", "SC_AGENT", "DRONE", "CONTRACTOR", "GCU", "GSV"
        })
        void armorWorksOnMartialClasses(HeroClass hc) {
            assertThat(ArtifactType.ARMOR.isCompatibleWith(hc)).isTrue();
        }

        @ParameterizedTest(name = "armor rejected on {0}")
        @EnumSource(value = HeroClass.class, names = {
                "CULTURE_CITIZEN", "REFERER"
        })
        void armorRejectedOnCivilianClasses(HeroClass hc) {
            assertThat(ArtifactType.ARMOR.isCompatibleWith(hc)).isFalse();
        }

        @Test
        void everyHeroClassAcceptsAtLeastOneArtifactType() {
            // Regression guard: a class that accepts nothing is unplayable.
            for (HeroClass hc : HeroClass.values()) {
                boolean accepts = false;
                for (ArtifactType t : ArtifactType.values()) {
                    if (t.isCompatibleWith(hc)) {
                        accepts = true;
                        break;
                    }
                }
                assertThat(accepts).as("class %s accepts at least one type", hc).isTrue();
            }
        }
    }

    /* ================================================================== */
    /*  toString                                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        void defaultToStringIncludesClassName() {
            assertThat(weapon(5).toString()).contains("Artifact@");
        }
    }
}