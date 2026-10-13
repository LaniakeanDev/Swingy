package fr._42.swingy.model.entity;

import fr._42.swingy.model.enums.ArtifactType;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Curated catalog of concrete artifacts.
 *
 * <p>Entries are grouped into tiers from weakest to strongest so that
 * {@link #rollForPower(Random, int)} can bias a drop toward the tier
 * appropriate to the villain that produced it.</p>
 */
public final class ArtifactPool {

    private ArtifactPool() {
        // utility class — no instantiation
    }

    /** Weak tier — drops from low-power villains (power 1–3). */
    private static final List<Artifact> WEAK = List.of(
        new Artifact(ArtifactType.WEAPON, 2,  "Knife missile"),
        new Artifact(ArtifactType.ARMOR,  2,  "Combat Sarcophagus"),
        new Artifact(ArtifactType.HELM,   3,  "Cortex Cowl"),
        new Artifact(ArtifactType.WEAPON, 3,  "CREWS"),
        new Artifact(ArtifactType.ARMOR,  3,  "Ghost Cloak")
    );

    /** Mid tier — drops from mid-power villains (power 4–7). */
    private static final List<Artifact> MID = List.of(
        new Artifact(ArtifactType.WEAPON, 6,  "Monofilament Warp"),
        new Artifact(ArtifactType.ARMOR,  5,  "Gelfield Suit"),
        new Artifact(ArtifactType.HELM,   6,  "MindHalo"),
        new Artifact(ArtifactType.WEAPON, 5,  "Gridfire"),
        new Artifact(ArtifactType.ARMOR,  6,  "Displacer")
    );

    /** Strong tier — drops from high-power villains (power 8+). */
    private static final List<Artifact> STRONG = List.of(
        new Artifact(ArtifactType.WEAPON, 12, "Effector"),
        new Artifact(ArtifactType.ARMOR,  11, "Dragonscale"),
        new Artifact(ArtifactType.HELM,   14, "Youth Therapy"),
        new Artifact(ArtifactType.WEAPON, 13, "Nanohole Bomb"),
        new Artifact(ArtifactType.ARMOR,  12, "Mind Protection"),
        new Artifact(ArtifactType.HELM,   13, "Idiran Helmet")
    );

    /**
     * Picks a random artifact appropriate to the villain's power.
     * Falls back to the strongest tier for overpowered villains.
     */
    public static Artifact rollForPower(Random random, int villainPower) {
        List<Artifact> tier = tierFor(villainPower);
        return tier.get(random.nextInt(tier.size()));
    }

    private static List<Artifact> tierFor(int villainPower) {
        if (villainPower <= 3) return WEAK;
        if (villainPower <= 7) return MID;
        return STRONG;
    }

    /** All artifacts, weakest to strongest. Useful for tests and debugging. */
    public static List<Artifact> all() {
        return Stream.of(WEAK, MID, STRONG)
                .flatMap(List::stream)
                .toList();
    }
}