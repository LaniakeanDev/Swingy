package fr._42.swingy.model.entity;
import fr._42.swingy.model.enums.ArtifactType;

public class Artifact {
    private final ArtifactType type;
    private final int value;
    private final String name;

    public Artifact(ArtifactType type, int value, String name) {
        this.type = type;
        this.value = value;
        this.name = name;
    }
    public ArtifactType getType()  {
        return this.type;
    }
    public int getValue() {
        return this.value;
    }
    public String getName() {
        return this.name;
    }
}
