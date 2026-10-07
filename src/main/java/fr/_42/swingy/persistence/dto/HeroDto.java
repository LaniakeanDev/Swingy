package fr._42.swingy.persistence.dto;

import java.util.List;

/** On-disk shape for a hero. Kept separate from the domain model. */
public class HeroDto {
    public String name;
    public String heroClass;      // HeroClass.name()
    public int level;
    public long experience;
    public int currentHitPoints;
    public Integer x;             // null when the hero has no position
    public Integer y;
    public List<ArtifactDto> artifacts = List.of();
}
