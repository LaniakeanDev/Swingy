package fr._42.swingy.model.entity;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import fr._42.swingy.model.enums.ArtifactType;
import fr._42.swingy.model.enums.HeroClass;

class HeroTest {
    private Hero hero() {
        return new Hero.HeroBuilder()
            .name("Aria").heroClass(HeroClass.CONTACT_AGENT).build();
    }

    @Test void freshHeroStartsAtMaxHp() {
        Hero h = hero();
        assertThat(h.getCurrentHitPoints()).isEqualTo(h.getHitPoints());
    }

    @Test void gainingThresholdXpLevelsUp() {
        Hero h = hero();
        long needed = h.experienceToNextLevel();
        h.gainExperience(needed);
        assertThat(h.getLevel()).isEqualTo(2);
    }

    @Test void incompatibleArtifactIsRejected() {
        Hero referer = new Hero.HeroBuilder()
            .name("Ref").heroClass(HeroClass.REFERER).build();
        Artifact armor = new Artifact(ArtifactType.ARMOR, 5, "Chainmail");
        assertThat(referer.equipArtifact(armor)).isFalse();
        assertThat(referer.getArtifacts()).isEmpty();
    }

    @Test void attackScalesWithLevelAndWeapons() {
        Hero h = hero();
        int baseAttack = h.getAttack();
        h.equipArtifact(new Artifact(ArtifactType.WEAPON, 7, "Sword"));
        assertThat(h.getAttack()).isEqualTo(baseAttack + 7);
    }
}