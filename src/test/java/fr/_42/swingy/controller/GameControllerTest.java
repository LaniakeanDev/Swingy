// package fr._42.swingy.controller;

// import static org.assertj.core.api.Assertions.assertThat;

// import java.nio.file.Path;
// import java.util.ArrayDeque;
// import java.util.ArrayList;
// import java.util.Deque;
// import java.util.List;
// import java.util.Random;

// import javax.validation.Validation;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.io.TempDir;

// import fr._42.swingy.model.entity.Hero;
// import fr._42.swingy.model.entity.Villain;
// import fr._42.swingy.model.enums.EncounterResult;
// import fr._42.swingy.model.enums.HeroClass;
// import fr._42.swingy.model.map.GameMap;
// import fr._42.swingy.persistence.HeroRepository;
// import fr._42.swingy.validation.Validator;
// import fr._42.swingy.view.View;

// class GameControllerTest {

//     /** Returns canned inputs in order, records everything displayed. */
//     static class ScriptedView implements View {
//         final Deque<String> inputs;
//         final List<String> messages = new ArrayList<>();
//         ScriptedView(String... inputs) { this.inputs = new ArrayDeque<>(List.of(inputs)); }
//         public void displayMessage(String m) { messages.add(m); }
//         public void displayError(String e)   { messages.add("[ERR] " + e); }
//         public String askInput(String p)     { return inputs.isEmpty() ? "exit" : inputs.poll(); }
//         public void renderMap(GameMap m, Hero h) {}
//         public void showHeroStats(Hero h) {}
//         public void displayHeroList(List<Hero> h) {}
//         public void showBattleResult(EncounterResult r, Hero h, Villain v) {}
//         public void showWinDialog(Hero h) {}
//         public void showLossDialog(Hero h, Villain v) {}
//         public void close() {}
//     }

//     @TempDir Path tempDir;

//     @Test void creatingHeroThenExitingSavesIt() {
//         ScriptedView view = new ScriptedView(
//                 "0",           // create new hero
//                 "Aria",        // name
//                 "CONTACT_AGENT", // class
//                 "exit");       // exit game loop
//         HeroRepository repo = new HeroRepository(tempDir.resolve("h.txt").toString());
//         Validator val = new Validator(
//             Validation.buildDefaultValidatorFactory().getValidator());

//         GameController controller = new GameController(
//                 view, val, repo, new Random(42));
//         controller.run();

//         assertThat(repo.loadAll()).extracting(Hero::getName).containsExactly("Aria");
//     }

//     @Test void duplicateNameIsRejectedThenAccepted() {
//         // ScriptedView view = new ScriptedView(
//         //         "0", "Aria", "CONTACT_AGENT",  // first hero
//         //         "exit");
//         // HeroRepository repo = new HeroRepository(tempDir.resolve("h.txt").toString());
//         // // pre-seed a hero named Aria
//         // repo.saveAll(List.of(new Hero.HeroBuilder()
//         //         .name("Aria").heroClass(HeroClass.CONTACT_AGENT).build()));

//         // // ... run and assert the view saw "already taken"
//     }

//     @Test void invalidDirectionDoesNotMoveHero() {
//         // Script: pick/create hero, try "up", then "exit"
//         // Assert hero position unchanged after "up"
//     }
// }
