package fr._42.swingy.model.entity;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;


class HeroTest {

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private static Hero hero() {
        return new Hero.HeroBuilder()
                .name("Aria")
                .heroClass(HeroClass.CONTACT_AGENT)
                .build();
    }

    private static Hero heroOfClass(HeroClass hc) {
        return new Hero.HeroBuilder()
                .name("Aria")
                .heroClass(hc)
                .build();
    }

    private static Artifact weapon(int value) {
        return new Artifact(ArtifactType.WEAPON, value, "W" + value);
    }

    private static Artifact armor(int value) {
        return new Artifact(ArtifactType.ARMOR, value, "A" + value);
    }

    private static Artifact helm(int value) {
        return new Artifact(ArtifactType.HELM, value, "H" + value);
    }

    /* ================================================================== */
    /*  Builder & construction                                             */
    /* ================================================================== */

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void builderPreservesEveryField() {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria")
                    .heroClass(HeroClass.SC_AGENT)
                    .level(5)
                    .experience(1234)
                    .currentHitPoints(77)
                    .position(new Position(3, 4))
                    .artifacts(List.of(weapon(6)))
                    .build();

            assertThat(h.getName()).isEqualTo("Aria");
            assertThat(h.getHeroClass()).isEqualTo(HeroClass.SC_AGENT);
            assertThat(h.getLevel()).isEqualTo(5);
            assertThat(h.getExperience()).isEqualTo(1234);
            assertThat(h.getCurrentHitPoints()).isEqualTo(77);
            assertThat(h.getPosition()).isEqualTo(new Position(3, 4));
            assertThat(h.getArtifacts()).hasSize(1);
        }

        @Test
        void defaultsAreLevelOneZeroXp() {
            Hero h = hero();
            assertThat(h.getLevel()).isEqualTo(1);
            assertThat(h.getExperience()).isZero();
        }

        @Test
        void freshHeroStartsAtMaxHp() {
            Hero h = hero();
            assertThat(h.getCurrentHitPoints()).isEqualTo(h.getHitPoints());
        }

        @Test
        void builderHonoursExplicitCurrentHp() {
            Hero h = new Hero.HeroBuilder()
                    .name("Aria").heroClass(HeroClass.CONTACT_AGENT)
                    .currentHitPoints(42)
                    .build();
            assertThat(h.getCurrentHitPoints()).isEqualTo(42);
        }

        @Test
        void defaultPositionIsNull() {
            assertThat(hero().getPosition()).isNull();
        }

        @Test
        void defaultArtifactListIsEmpty() {
            assertThat(hero().getArtifacts()).isEmpty();
        }
    }

    /* ================================================================== */
    /*  Derived stats: attack, defense, hitPoints                          */
    /* ================================================================== */

    @Nested
    @DisplayName("derived stats")
    class DerivedStats {

        @Test
        void level1HeroWithNoArtifactsHasClassBaseStats() {
            for (HeroClass hc : HeroClass.values()) {
                Hero h = heroOfClass(hc);
                assertThat(h.getAttack()).as("%s attack", hc).isEqualTo(hc.getBaseAttack());
                assertThat(h.getDefense()).as("%s defense", hc).isEqualTo(hc.getBaseDefense());
                assertThat(h.getHitPoints()).as("%s hp", hc).isEqualTo(hc.getBaseHitPoints());
            }
        }

        @Test
        void attackGrowsByTwoPerLevel() {
            Hero l1 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(1).build();
            Hero l4 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(4).build();
            assertThat(l4.getAttack() - l1.getAttack()).isEqualTo(6);  // (4-1)*2
        }

        @Test
        void defenseGrowsByOnePerLevel() {
            Hero l1 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(1).build();
            Hero l4 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(4).build();
            assertThat(l4.getDefense() - l1.getDefense()).isEqualTo(3); // (4-1)*1
        }

        @Test
        void hitPointsGrowByFivePerLevel() {
            Hero l1 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(1).build();
            Hero l4 = new Hero.HeroBuilder().name("A").heroClass(HeroClass.CONTACT_AGENT).level(4).build();
            assertThat(l4.getHitPoints() - l1.getHitPoints()).isEqualTo(15); // (4-1)*5
        }

        @Test
        void weaponsAddOnlyToAttack() {
            Hero h = hero();
            int atk = h.getAttack(), def = h.getDefense(), hp = h.getHitPoints();
            h.equipArtifact(weapon(7));

            assertThat(h.getAttack()).isEqualTo(atk + 7);
            assertThat(h.getDefense()).isEqualTo(def);
            assertThat(h.getHitPoints()).isEqualTo(hp);
        }

        @Test
        void armorsAddOnlyToDefense() {
            Hero h = hero();
            int atk = h.getAttack(), def = h.getDefense(), hp = h.getHitPoints();
            h.equipArtifact(armor(5));

            assertThat(h.getAttack()).isEqualTo(atk);
            assertThat(h.getDefense()).isEqualTo(def + 5);
            assertThat(h.getHitPoints()).isEqualTo(hp);
        }

        @Test
        void helmsAddOnlyToHitPoints() {
            Hero h = hero();
            int atk = h.getAttack(), def = h.getDefense(), hp = h.getHitPoints();
            h.equipArtifact(helm(3));

            assertThat(h.getAttack()).isEqualTo(atk);
            assertThat(h.getDefense()).isEqualTo(def);
            assertThat(h.getHitPoints()).isEqualTo(hp + 3);
        }

        @Test
        void multipleWeaponsStack() {
            Hero h = hero();
            int base = h.getAttack();
            h.equipArtifact(weapon(3));
            h.equipArtifact(weapon(4));
            h.equipArtifact(weapon(5));
            assertThat(h.getAttack()).isEqualTo(base + 12);
        }

        @Test
        void multipleArtifactsOfSameTypeStack() {
            Hero h = hero();
            int baseHp = h.getHitPoints();
            h.equipArtifact(helm(2));
            h.equipArtifact(helm(4));
            assertThat(h.getHitPoints()).isEqualTo(baseHp + 6);
        }

        @Test
        void hitPointsFromHelmRaisesMaxButNotCurrentHp() {
            // Important: equipping a helm raises the max, but doesn't heal.
            // Otherwise you could swap helms repeatedly to regenerate.
            Hero h = hero();
            int before = h.getCurrentHitPoints();
            h.equipArtifact(helm(100));
            assertThat(h.getHitPoints()).isEqualTo(before + 100);
            assertThat(h.getCurrentHitPoints()).isEqualTo(before);
        }
    }

    /* ================================================================== */
    /*  XP & leveling                                                      */
    /* ================================================================== */

    @Nested
    @DisplayName("experience and leveling")
    class Leveling {

        @Test
        void xpBelowThresholdDoesNotLevelUp() {
            Hero h = hero();
            h.gainExperience(h.experienceToNextLevel() - 1);
            assertThat(h.getLevel()).isEqualTo(1);
        }

        @Test
        void xpAtThresholdLevelsUp() {
            Hero h = hero();
            h.gainExperience(h.experienceToNextLevel());
            assertThat(h.getLevel()).isEqualTo(2);
        }

        @Test
        void xpOverThresholdAlsoLevelsUp() {
            Hero h = hero();
            h.gainExperience(h.experienceToNextLevel() + 500);
            assertThat(h.getLevel()).isEqualTo(2);
        }

        @Test
        void gainExperienceAccumulates() {
            Hero h = hero();
            h.gainExperience(100);
            h.gainExperience(200);
            assertThat(h.getExperience()).isEqualTo(300);
        }

        @Test
        void multipleSmallGainsEventuallyLevelUp() {
            Hero h = hero();
            long threshold = h.experienceToNextLevel();
            for (int i = 0; i < 10; i++) h.gainExperience(threshold / 10);
            assertThat(h.getLevel()).isEqualTo(2);
        }

        @Test
        void zeroXpIsHarmless() {
            Hero h = hero();
            h.gainExperience(0);
            assertThat(h.getLevel()).isEqualTo(1);
            assertThat(h.getExperience()).isZero();
        }

        @Test
        void levelingUpRaisesMaxHitPoints() {
            Hero h = hero();
            int before = h.getHitPoints();
            h.gainExperience(h.experienceToNextLevel());
            assertThat(h.getHitPoints()).isEqualTo(before + 5);
        }

        @Test
        void levelingUpRaisesAttackAndDefense() {
            Hero h = hero();
            int atk = h.getAttack(), def = h.getDefense();
            h.gainExperience(h.experienceToNextLevel());
            assertThat(h.getAttack()).isEqualTo(atk + 2);
            assertThat(h.getDefense()).isEqualTo(def + 1);
        }

        @Test
        void levelingUpDoesNotHealCurrentHp() {
            // Documented behaviour: level up raises the cap, doesn't heal.
            Hero h = hero();
            h.takeDamage(50);
            int damaged = h.getCurrentHitPoints();
            h.gainExperience(h.experienceToNextLevel());
            assertThat(h.getCurrentHitPoints()).isEqualTo(damaged);
        }

        @ParameterizedTest(name = "level {0} needs {1} xp")
        @CsvSource({
                "1, 1000",
                "2, 2450",
                "3, 4800",
                "4, 8050",
        })
        void experienceCurveMatchesFormula(int level, long expected) {
            // experienceToNextLevel = level*1000 + (level-1)^2 * 450
            Hero h = new Hero.HeroBuilder()
                    .name("A").heroClass(HeroClass.CONTACT_AGENT)
                    .level(level)
                    .build();
            assertThat(h.experienceToNextLevel()).isEqualTo(expected);
        }

        @Test
        void xpCurveIsStrictlyIncreasing() {
            long prev = 0;
            for (int level = 1; level <= 20; level++) {
                Hero h = new Hero.HeroBuilder()
                        .name("A").heroClass(HeroClass.CONTACT_AGENT)
                        .level(level)
                        .build();
                long curr = h.experienceToNextLevel();
                assertThat(curr).as("level %d", level).isGreaterThan(prev);
                prev = curr;
            }
        }
    }

    /* ================================================================== */
    /*  Damage                                                             */
    /* ================================================================== */

    @Nested
    @DisplayName("takeDamage")
    class Damage {

        @Test
        void reducesCurrentHp() {
            Hero h = hero();
            int before = h.getCurrentHitPoints();
            h.takeDamage(10);
            assertThat(h.getCurrentHitPoints()).isEqualTo(before - 10);
        }

        @Test
        void cannotDropBelowZero() {
            Hero h = hero();
            h.takeDamage(h.getCurrentHitPoints() + 999);
            assertThat(h.getCurrentHitPoints()).isZero();
        }

        @Test
        void zeroDamageIsHarmless() {
            Hero h = hero();
            int before = h.getCurrentHitPoints();
            h.takeDamage(0);
            assertThat(h.getCurrentHitPoints()).isEqualTo(before);
        }

        @Test
        void damageDoesNotAffectMaxHp() {
            Hero h = hero();
            int max = h.getHitPoints();
            h.takeDamage(50);
            assertThat(h.getHitPoints()).isEqualTo(max);
        }

        @Test
        void damageIsIdempotentAtZero() {
            Hero h = hero();
            h.takeDamage(99999);
            h.takeDamage(99999);
            assertThat(h.getCurrentHitPoints()).isZero();
        }
    }

    /* ================================================================== */
    /*  Artifacts                                                          */
    /* ================================================================== */

    @Nested
    @DisplayName("artifacts")
    class Artifacts {

        @Test
        void nullArtifactIsRejected() {
            assertThat(hero().equipArtifact(null)).isFalse();
        }

        @Test
        void weaponIsAcceptedByEveryClass() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(heroOfClass(hc).equipArtifact(weapon(1)))
                        .as("%s weapon", hc).isTrue();
            }
        }

        @Test
        void helmIsAcceptedByEveryClass() {
            for (HeroClass hc : HeroClass.values()) {
                assertThat(heroOfClass(hc).equipArtifact(helm(1)))
                        .as("%s helm", hc).isTrue();
            }
        }

        @Test
        void armorIsAcceptedBySomeClasses() {
            assertThat(heroOfClass(HeroClass.CONTACT_AGENT).equipArtifact(armor(1))).isTrue();
            assertThat(heroOfClass(HeroClass.SC_AGENT).equipArtifact(armor(1))).isTrue();
            assertThat(heroOfClass(HeroClass.GCU).equipArtifact(armor(1))).isTrue();
        }

        @Test
        void armorIsRejectedByCultureCitizen() {
            assertThat(heroOfClass(HeroClass.CULTURE_CITIZEN).equipArtifact(armor(1))).isFalse();
        }

        @Test
        void armorIsRejectedByReferer() {
            assertThat(heroOfClass(HeroClass.REFERER).equipArtifact(armor(1))).isFalse();
        }

        @Test
        void incompatibleArtifactIsRejectedAndNotAdded() {
            Hero referer = heroOfClass(HeroClass.REFERER);
            Artifact a = armor(5);
            assertThat(referer.equipArtifact(a)).isFalse();
            assertThat(referer.getArtifacts()).isEmpty();
        }

        @Test
        void successfulEquipAppendsToArtifacts() {
            Hero h = hero();
            Artifact w = weapon(3);
            assertThat(h.equipArtifact(w)).isTrue();
            assertThat(h.getArtifacts()).containsExactly(w);
        }

        @Test
        void getArtifactsIsUnmodifiable() {
            Hero h = hero();
            h.equipArtifact(weapon(3));

            List<Artifact> artifacts = h.getArtifacts();
            assertThat(artifacts).isUnmodifiable();

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> artifacts.add(weapon(99)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void getArtifactsIsSnapshotNotLiveView() {
            // Mutation of the *internal* list after getArtifacts() must not be
            // visible through a previously-returned reference.
            Hero h = hero();
            h.equipArtifact(weapon(3));

            List<Artifact> snapshot = h.getArtifacts();   // snapshot at size 1
            h.equipArtifact(weapon(4));                    // internal list now size 2

            assertThat(snapshot).hasSize(1);               // still the old state
            assertThat(h.getArtifacts()).hasSize(2);
        }
        
        @Test
        void getArtifactsIsImmutable() {
            Hero h = hero();
            assertThat(h.getArtifacts()).isUnmodifiable();
        }

        @Test
        void equippingManyArtifactsAccumulates() {
            Hero h = hero();
            for (int i = 1; i <= 10; i++) h.equipArtifact(weapon(i));
            assertThat(h.getArtifacts()).hasSize(10);
            assertThat(h.getAttack()).isEqualTo(
                    HeroClass.CONTACT_AGENT.getBaseAttack() + 55);   // 1+2+...+10
        }

        @ParameterizedTest(name = "weapon + helm on {0} always works")
        @EnumSource(HeroClass.class)
        void universalSlotsWorkOnEveryClass(HeroClass hc) {
            Hero h = heroOfClass(hc);
            assertThat(h.equipArtifact(weapon(2))).isTrue();
            assertThat(h.equipArtifact(helm(2))).isTrue();
            assertThat(h.getArtifacts()).hasSize(2);
        }
    }

    /* ================================================================== */
    /*  Position                                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("position")
    class PositionTests {

        @Test
        void setPositionUpdatesValue() {
            Hero h = hero();
            h.setPosition(new Position(3, 5));
            assertThat(h.getPosition()).isEqualTo(new Position(3, 5));
        }

        @Test
        void setPositionCanReplaceExisting() {
            Hero h = new Hero.HeroBuilder()
                    .name("A").heroClass(HeroClass.CONTACT_AGENT)
                    .position(new Position(1, 1))
                    .build();
            h.setPosition(new Position(9, 9));
            assertThat(h.getPosition()).isEqualTo(new Position(9, 9));
        }
    }

    /* ================================================================== */
    /*  Integration                                                        */
    /* ================================================================== */

    @Nested
    @DisplayName("integration")
    class Integration {

        @Test
        void levelUpWithArtifactsCompoundsCorrectly() {
            Hero h = hero();
            h.equipArtifact(weapon(5));
            h.equipArtifact(armor(5));
            h.equipArtifact(helm(5));

            int atk = h.getAttack(), def = h.getDefense(), hp = h.getHitPoints();
            h.gainExperience(h.experienceToNextLevel());

            assertThat(h.getAttack()).isEqualTo(atk + 2);
            assertThat(h.getDefense()).isEqualTo(def + 1);
            assertThat(h.getHitPoints()).isEqualTo(hp + 5);
        }

        @Test
        void fullLifecycleFromFreshToDead() {
            Hero h = hero();
            int maxHp = h.getHitPoints();
            assertThat(h.getCurrentHitPoints()).isEqualTo(maxHp);

            h.takeDamage(maxHp / 2);
            assertThat(h.getCurrentHitPoints()).isEqualTo(maxHp / 2);

            h.gainExperience(h.experienceToNextLevel());
            assertThat(h.getLevel()).isEqualTo(2);
            // Note: level up doesn't heal, so still at maxHp/2.
            assertThat(h.getCurrentHitPoints()).isEqualTo(maxHp / 2);

            h.takeDamage(maxHp);
            assertThat(h.getCurrentHitPoints()).isZero();
        }
    }
}