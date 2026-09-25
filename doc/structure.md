swingy/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── fr/
    │   │       └── 42/
    │   │           └── swingy/
    │   │               ├── Main.java                    # Entry point, parses console/gui args
    │   │               │
    │   │               ├── controller/                  # CONTROLLER layer
    │   │               │   ├── GameController.java      # Orchestrates game loop
    │   │               │   ├── HeroController.java      # Hero creation/selection logic
    │   │               │   ├── BattleController.java    # Fight/run resolution
    │   │               │   └── InputHandler.java        # Abstract input dispatch
    │   │               │
    │   │               ├── model/                       # MODEL layer
    │   │               │   ├── entity/
    │   │               │   │   ├── Hero.java
    │   │               │   │   ├── Villain.java
    │   │               │   │   └── Artifact.java
    │   │               │   ├── enums/
    │   │               │   │   ├── HeroClass.java
    │   │               │   │   ├── ArtifactType.java
    │   │               │   │   └── Direction.java
    │   │               │   ├── map/
    │   │               │   │   ├── GameMap.java
    │   │               │   │   └── Position.java
    │   │               │   ├── battle/
    │   │               │   │   └── BattleSimulator.java
    │   │               │   └── builder/                 # Builder pattern (required by subject)
    │   │               │       ├── HeroBuilder.java
    │   │               │       └── VillainBuilder.java
    │   │               │
    │   │               ├── view/                        # VIEW layer (interface + impls)
    │   │               │   ├── View.java                # Interface: display(), getUserInput()
    │   │               │   ├── console/
    │   │               │   │   └── ConsoleView.java
    │   │               │   └── gui/
    │   │               │       ├── GuiView.java
    │   │               │       ├── panels/
    │   │               │       │   ├── MenuPanel.java
    │   │               │       │   ├── GamePanel.java
    │   │               │       │   └── HeroStatsPanel.java
    │   │               │       └── SwingyFrame.java     # Main JFrame
    │   │               │
    │   │               ├── validation/                  # Annotation-based validation
    │   │               │   ├── Validator.java
    │   │               │   └── annotations/
    │   │               │       └── ValidHeroName.java
    │   │               │
    │   │               ├── persistence/                 # Save/load heroes
    │   │               │   └── HeroRepository.java      # Reads/writes heroes.txt
    │   │               │
    │   │               └── util/
    │   │                   ├── RandomGenerator.java
    │   │                   └── Constants.java
    │   │
    │   └── resources/
    │       ├── META-INF/
    │       │   └── validation.xml                       # Hibernate Validator config
    │       └── heroes.txt                               # Default save file (optional)
    │
    └── test/
        └── java/
            └── fr/
                └── 42/
                    └── swingy/
                        ├── model/
                        │   ├── HeroTest.java
                        │   └── BattleSimulatorTest.java
                        ├── controller/
                        │   └── GameControllerTest.java
                        └── validation/
                            └── ValidatorTest.java