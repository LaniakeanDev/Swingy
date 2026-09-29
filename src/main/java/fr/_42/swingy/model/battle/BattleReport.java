package fr._42.swingy.model.battle;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.enums.EncounterResult;

import java.util.List;
import java.util.Optional;

/**
 * Immutable record of a resolved battle.
 *
 * @param result the outcome (won / lost — fleeing is handled before the fight)
 * @param log    one line per exchange, ready to print
 * @param drop   an artifact the villain dropped, if any
 */
public record BattleReport(
        EncounterResult result,
        List<String> log,
        Optional<Artifact> drop
) {}