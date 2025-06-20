package com.agm.ui;

import com.agm.model.Person;
import com.agm.screens.NodeView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Lanzador de un flujo de 4 diálogos (nombre -> nacimiento -> defunción -> cita)
 * que sirve tanto para CREAR como para EDITAR una persona.
 * <p>
 * • Para crear un nuevo hijo o padre llama a {@link #create(NodeView, Consumer)}
 * • Para editar un nodo existente llama a {@link #edit(NodeView)}
 * <p>
 * *No* destruye la Skin (la cierra el EditorScreen).
 */
public class PersonEditorDialog {

    private final Stage stage;              // solo por si lo necesitas más adelante
    private final Skin skin;

    public PersonEditorDialog(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
    }

    /* --------------------------------------------------------------------- */
    /*  API pública                                                          */
    /* --------------------------------------------------------------------- */

    /**
     * Flujo de creación: devuelve la nueva Person en el callback
     */
    public void create(NodeView base, Consumer<Person> onCreated) {
        launchWizard(/*editing*/false, base, null, onCreated);
    }

    /**
     * Flujo de edición in-place del propio objeto Person
     */
    public void edit(NodeView node) {
        launchWizard(/*editing*/true, node, null, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Implementación                                                       */
    /* --------------------------------------------------------------------- */

    private void launchWizard(boolean editing, NodeView base, Person personToEdit, Consumer<Person> creationCallback) {

        // Si editing==true rellenamos valores iniciales; si no, cadena vacía
        Person initial = editing ? base.getPerson() : null;

        /* PASO 1 ─────────────── Nombre ─────────────────────────────────── */
        Gdx.input.getTextInput(new TextInputListener() {
            @Override
            public void input(String fullName) {

                String[] parts = fullName.trim().split("\\s+", 2);
                String first = parts.length > 0 ? parts[0] : "";
                String last = parts.length > 1 ? parts[1] : "";

                /* PASO 2 ─────────── Nacimiento ─────────────────────────── */
                String initBirth = editing ? initial.getBirthDate().toString() : "";
                Gdx.input.getTextInput(new TextInputListener() {
                    @Override
                    public void input(String bdTxt) {
                        LocalDate bd;
                        try {
                            bd = LocalDate.parse(bdTxt);
                        } catch (Exception e) {
                            bd = LocalDate.now();
                        }

                        /* PASO 3 ───── Defunción (opcional) ─────────────── */
                        String initDeath = editing && initial.getDeathDate() != null ? initial.getDeathDate().toString() : "";
                        LocalDate finalBd = bd;

                        Gdx.input.getTextInput(new TextInputListener() {
                            @Override
                            public void input(String ddTxt) {
                                LocalDate dd = null;
                                if (!ddTxt.isBlank()) {
                                    try {
                                        dd = LocalDate.parse(ddTxt);
                                    } catch (Exception ignore) {
                                    }
                                }

                                /* PASO 4 ───── Cita / Nota ──────────────── */
                                String initQuote = editing ? initial.getQuote() : "";
                                LocalDate finalDd = dd;

                                Gdx.input.getTextInput(new TextInputListener() {
                                    @Override
                                    public void input(String quoteTxt) {

                                        if (editing) {              // ← EDITAR
                                            initial.setFirstName(first);
                                            initial.setLastName(last);
                                            initial.setBirthDate(finalBd);
                                            initial.setDeathDate(finalDd);
                                            initial.setQuote(quoteTxt);
                                        } else {                     // ← CREAR
                                            Person np = new Person(UUID.randomUUID().toString(), first, last, finalBd, finalDd, quoteTxt);
                                            creationCallback.accept(np);
                                        }
                                    }

                                    @Override
                                    public void canceled() {
                                    }
                                }, "Cita u observación", initQuote, "Texto libre");

                            }

                            @Override
                            public void canceled() {
                            }
                        }, "Fecha defunción (YYYY-MM-DD)", initDeath, "YYYY-MM-DD");
                    }

                    @Override
                    public void canceled() {
                    }
                }, "Fecha nacimiento (YYYY-MM-DD)", initBirth, "YYYY-MM-DD");
            }

            @Override
            public void canceled() {
            }
        }, editing ? "Editar nombre" : "Nombre completo", editing ? initial.getFirstName() + " " + initial.getLastName() : "", "Nombre Apellidos");
    }

    /**
     * No libera la Skin: la gestiona EditorScreen.
     */
    public void dispose() {
    }
}
