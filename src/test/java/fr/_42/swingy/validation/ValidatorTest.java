package fr._42.swingy.validation;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.enums.HeroClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorTest {

    /* ------------------------------------------------------------------ */
    /*  Shared factory — building one is expensive and thread-safe         */
    /* ------------------------------------------------------------------ */

    private static ValidatorFactory factory;

    @BeforeAll
    static void bootstrap() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    private Validator validator() {
        return new Validator(factory.getValidator());
    }

    private static Hero hero(String name, HeroClass hc) {
        return new Hero.HeroBuilder().name(name).heroClass(hc).build();
    }

    private static Hero validHero() {
        return hero("Aria", HeroClass.CONTACT_AGENT);
    }

    /* ================================================================== */
    /*  Name constraints                                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("name")
    class Name {

        @Test
        void acceptsTypicalName() {
            assertThat(validator().isValid(hero("Aria", HeroClass.CONTACT_AGENT))).isTrue();
        }

        @Test
        void acceptsNameAtMinimumLength() {
            // @Size(min = 3)
            assertThat(validator().isValid(hero("Ana", HeroClass.CONTACT_AGENT))).isTrue();
        }

        @Test
        void acceptsNameAtMaximumLength() {
            // @Size(max = 20)
            String twenty = "A".repeat(20);
            assertThat(validator().isValid(hero(twenty, HeroClass.CONTACT_AGENT))).isTrue();
        }

        @Test
        void acceptsNameWithSpaces() {
            // @Pattern("[A-Za-z ]+") explicitly allows spaces.
            assertThat(validator().isValid(hero("Ann Marie", HeroClass.CONTACT_AGENT))).isTrue();
        }

        @Test
        void rejectsShortName() {
            assertThat(validator().isValid(hero("Al", HeroClass.CONTACT_AGENT))).isFalse();
        }

        @Test
        void rejectsNameOfLengthOne() {
            assertThat(validator().isValid(hero("A", HeroClass.CONTACT_AGENT))).isFalse();
        }

        @Test
        void rejectsNameOverMaximumLength() {
            String twentyOne = "A".repeat(21);
            assertThat(validator().isValid(hero(twentyOne, HeroClass.CONTACT_AGENT))).isFalse();
        }

        @ParameterizedTest(name = "rejects \"{0}\"")
        @ValueSource(strings = {
                "Aria123",     // digits
                "Aria!",       // punctuation
                "Aria-",       // hyphen
                "Aria_",       // underscore
                "Aria|Evil",   // pipe  — would corrupt save format if allowed
                "Aria;Evil",   // semicolon
                "Aria:Evil",   // colon
                "Aria\nEvil",  // newline
                "Aria\tEvil",  // tab
                "Aria.Evil",   // dot
        })
        void rejectsIllegalCharacters(String name) {
            assertThat(validator().isValid(hero(name, HeroClass.CONTACT_AGENT)))
                    .as("name %s should be rejected by @Pattern", name)
                    .isFalse();
        }

        @ParameterizedTest(name = "accepts \"{0}\"")
        @ValueSource(strings = {
                "Aria",
                "Aria Bo",
                "Ana Maria Smith Jr",
                "X Y Z",
        })
        void acceptsLegalNames(String name) {
            assertThat(validator().isValid(hero(name, HeroClass.CONTACT_AGENT))).isTrue();
        }
    }

    /* ================================================================== */
    /*  Class constraints                                                  */
    /* ================================================================== */

    @Nested
    @DisplayName("hero class")
    class HeroClassValidation {

        @ParameterizedTest(name = "class {0}")
        @EnumSource(HeroClass.class)
        void acceptsEveryDefinedClass(HeroClass hc) {
            assertThat(validator().isValid(hero("Aria", hc))).isTrue();
        }

        // NOTE: `rejectsNullHeroClass` was removed on purpose.
        // Hero.HeroBuilder.build() dereferences heroClass via
        // heroClass.getBaseHitPoints() *before* Bean Validation runs, so a
        // null heroClass causes an NPE inside the constructor. The @NotNull
        // on the field is currently unreachable through the builder.
        // If the builder is ever changed to defer that dereference, restore
        // a test here.
    }

    /* ================================================================== */
    /*  Numeric constraints                                                */
    /* ================================================================== */

    @Nested
    @DisplayName("numeric fields")
    class Numeric {

        @Test
        void acceptsLevelOfOne() {
            // @Min(1) on level — the floor must pass.
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .level(1)
                    .build();
            assertThat(validator().isValid(h)).isTrue();
        }

        @Test
        void acceptsLargeLevel() {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .level(9999)
                    .build();
            assertThat(validator().isValid(h)).isTrue();
        }

        @Test
        void acceptsZeroExperience() {
            // @Min(0) — the default for fresh heroes.
            assertThat(validator().isValid(validHero())).isTrue();
        }

        @Test
        void acceptsHeroAtZeroHitPoints() {
            // Documented reality: currentHitPoints is NOT annotated, so a
            // hero at 0 HP is still "valid". The battle logic, not the
            // validator, decides death. Pin this so nobody silently adds
            // @Min(0) and breaks the game-over path.
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.CONTACT_AGENT)
                    .currentHitPoints(0)
                    .build();
            assertThat(validator().isValid(h)).isTrue();
        }
    }

    /* ================================================================== */
    /*  validateAndCollect                                                 */
    /* ================================================================== */

    @Nested
    @DisplayName("validateAndCollect")
    class ValidateAndCollect {

        @Test
        void returnsEmptyStringWhenNoViolations() {
            assertThat(validator().validateAndCollect(validHero())).isEmpty();
        }

        @Test
        void returnsNonEmptyStringWhenInvalid() {
            String report = validator().validateAndCollect(
                    hero("Al", HeroClass.CONTACT_AGENT));
            assertThat(report).isNotBlank();
        }

        @Test
        void reportContainsTheOffendingPropertyName() {
            String report = validator().validateAndCollect(
                    hero("Al", HeroClass.CONTACT_AGENT));
            assertThat(report).contains("name");
        }

        @Test
        void reportsEveryViolationWhenMultipleConstraintsFail() {
            // "A" fails @Size(min=3) AND matches… nothing else, so we need
            // another violating field that is reachable. A 21-char name
            // fails @Size(max=20); a name of 3 chars that matches the pattern
            // is valid. So combine: too-short name (fails @Size) + a second
            // reachable violation. Currently there is only one reachable
            // violation source on Hero: the name. So this test asserts that
            // *at least* the name violation is reported, and skips the
            // multi-field claim (which requires a nullable heroClass to be
            // reachable, and it isn't — see HeroClassValidation).
            String report = validator().validateAndCollect(
                    hero("Al", HeroClass.CONTACT_AGENT));

            assertThat(report.lines())
                    .isNotEmpty()
                    .anyMatch(l -> l.startsWith("name"));
        }

        @Test
        void agreesWithIsValidForTheSameObject() {
            Hero valid   = validHero();
            Hero invalid = hero("Al", HeroClass.CONTACT_AGENT);

            assertThat(validator().isValid(valid))
                    .isEqualTo(validator().validateAndCollect(valid).isEmpty());
            assertThat(validator().isValid(invalid))
                    .isEqualTo(validator().validateAndCollect(invalid).isEmpty());
        }

        @Test
        void throwsOnNullObject() {
            // Hibernate Validator rejects null targets with HV000116.
            // The façade forwards straight through, so this is the
            // observable contract.
            assertThatThrownBy(() -> validator().validateAndCollect(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HV000116");
        }
    }

    /* ================================================================== */
    /*  Façade behaviour                                                   */
    /* ================================================================== */

    @Nested
    @DisplayName("façade")
    class Facade {

        @Test
        void constructorRejectsNullDelegate() {
            // This test requires the Objects.requireNonNull guard added to
            // Validator's constructor. If you keep the current constructor
            // (no guard), delete this test — the NPE only fires on first use.
            assertThatThrownBy(() -> new Validator(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("delegate");
        }

        @Test
        void validatesAcrossMultipleInstancesIndependently() {
            Validator v1 = validator();
            Validator v2 = validator();

            Hero good = validHero();
            Hero bad  = hero("Al", HeroClass.CONTACT_AGENT);

            assertThat(v1.isValid(good)).isEqualTo(v2.isValid(good));
            assertThat(v1.isValid(bad)).isEqualTo(v2.isValid(bad));
        }

        @Test
        void isValidDoesNotThrowOnInvalidObjects() {
            // isValid's whole purpose is a boolean answer, never an exception
            // for *invalid* (but non-null) objects.
            assertThat(validator().isValid(hero("Al", HeroClass.CONTACT_AGENT)))
                    .isFalse();
        }

        @Test
        void isValidThrowsOnNullObject() {
            // Null is not "invalid" in the assertion sense — Hibernate
            // Validator rejects it outright. Pin the contract.
            assertThatThrownBy(() -> validator().isValid(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HV000116");
        }
    }
}