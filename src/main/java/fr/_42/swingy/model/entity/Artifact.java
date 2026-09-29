package fr._42.swingy.model.entity;
import fr._42.swingy.model.enums.ArtifactType;

public class Artifact {
    private final ArtifactType type;
    private final int value;
    private final String name;

    public Artifact(ArtifactType type, int value, String name) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.contains(":") || name.contains(";") || name.contains("|")) {
            throw new IllegalArgumentException(
                "artifact name must not contain ':', ';' or '|': " + name);
        }
        this.type  = type;
        this.value = value;
        this.name  = name;
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
