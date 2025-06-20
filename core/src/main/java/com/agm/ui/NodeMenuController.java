package com.agm.ui;

import com.agm.model.FamilyTree;
import com.agm.model.Person;
import com.agm.model.RelationType;
import com.agm.screens.NodeView;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.List;

/**
 * Menú contextual de un nodo: ver info, editar y añadir relación
 * (hijo, padre, cónyuge, hermano).
 */
public class NodeMenuController {

    private final Stage stage;
    private final FamilyTree tree;
    private final List<NodeView> nodes;
    private final Skin skin;
    private final PersonEditorDialog editor;
    private final Runnable relayout;     // callback para recolocar el árbol
    private Table menu;                  // menú visible

    public NodeMenuController(Stage stage, FamilyTree tree, List<NodeView> nodes, Skin skin, Runnable relayout) {
        this.stage = stage;
        this.tree = tree;
        this.nodes = nodes;
        this.skin = skin;
        this.relayout = relayout;
        this.editor = new PersonEditorDialog(stage, skin);
    }

    /* ------------------------------------------------------------------ */
    /*  Menú principal                                                    */
    /* ------------------------------------------------------------------ */

    public void show(NodeView node) {
        close();                              // cierra menú anterior

        menu = new Table(skin);
        menu.setBackground("white");
        menu.pad(20).defaults().pad(10).minWidth(160).minHeight(45);

        TextButton viewBtn = new TextButton("Ver info", skin, "big");
        TextButton editBtn = new TextButton("Editar nodo", skin, "big");
        TextButton relBtn = new TextButton("Añadir relación", skin, "big");

        menu.add(viewBtn).row();
        menu.add(editBtn).row();
        menu.add(relBtn);

        /* --- callbacks --- */
        viewBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                showInfo(node);
            }
        });

        editBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editor.edit(node);
                relayout.run();
                close();
            }
        });

        relBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                showRelationDialog(node);
            }
        });

        /* posición */
        menu.pack();
        menu.setPosition(node.getX() - menu.getWidth() / 2f, node.getY() - NodeView.RADIUS - menu.getHeight() - 20);
        stage.addActor(menu);
    }

    /* ------------------------------------------------------------------ */
    /*  Sub-diálogo de selección de relación                              */
    /* ------------------------------------------------------------------ */

    private void showRelationDialog(NodeView base) {
        Dialog dlg = new Dialog("Selecciona relación", skin) {
            @Override
            protected void result(Object obj) { /* nada */ }
        };

        Table t = dlg.getContentTable();
        t.pad(20).defaults().pad(10).minWidth(180).minHeight(45);

        TextButton hijoBtn = new TextButton("Hijo/a", skin, "big");
        TextButton padreBtn = new TextButton("Padre", skin, "big");
        TextButton conyBtn = new TextButton("Cónyuge", skin, "big");
        TextButton hnoBtn = new TextButton("Hermano/a", skin, "big");

        t.add(hijoBtn).row();
        t.add(padreBtn).row();
        t.add(conyBtn).row();
        t.add(hnoBtn);

        /* ----- listeners para cada tipo ----- */
        hijoBtn.addListener(new RelationCreator(base, RelationKind.CHILD, dlg));
        padreBtn.addListener(new RelationCreator(base, RelationKind.PARENT, dlg));
        conyBtn.addListener(new RelationCreator(base, RelationKind.SPOUSE, dlg));
        hnoBtn.addListener(new RelationCreator(base, RelationKind.SIBLING, dlg));

        dlg.button("Cancelar").pad(15);
        dlg.key(Input.Keys.ESCAPE, true);

        dlg.show(stage).setPosition((stage.getViewport().getWorldWidth() - dlg.getWidth()) / 2, (stage.getViewport().getWorldHeight() - dlg.getHeight()) / 2);
    }

    /* ------------------------------------------------------------------ */
    /*  Helper interno para crear persona + relación                      */
    /* ------------------------------------------------------------------ */

    private enum RelationKind {CHILD, PARENT, SPOUSE, SIBLING}

    private class RelationCreator extends ClickListener {
        private final NodeView base;
        private final RelationKind kind;
        private final Dialog parentDlg;

        RelationCreator(NodeView base, RelationKind kind, Dialog parentDlg) {
            this.base = base;
            this.kind = kind;
            this.parentDlg = parentDlg;
        }

        @Override
        public void clicked(InputEvent e, float x, float y) {

            editor.create(base, np -> {
                tree.addPerson(np);

                switch (kind) {
                    case CHILD:
                        tree.addParentChild(base.getPerson().getId(), np.getId());
                        break;

                    case PARENT:
                        tree.addParentChild(np.getId(), base.getPerson().getId());
                        break;

                    case SPOUSE:
                        /* 1 · vínculo de pareja en ambos sentidos */
                        tree.addSpouse(base.getPerson().getId(), np.getId());

                        /* 2 · el nuevo cónyuge adopta todos los hijos ya existentes */
                        tree.childrenOf(base.getPerson().getId())
                            .map(com.agm.model.Person::getId)
                            .forEach(childId -> tree.addParentChild(np.getId(), childId));
                        break;

                    case SIBLING:
                        tree.addSibling(base.getPerson().getId(), np.getId());

                        /* copia los padres del hermano original al nuevo */
                        tree.parentsOf(base.getPerson().getId())
                            .map(com.agm.model.Person::getId)
                            .forEach(parentId -> tree.addParentChild(parentId, np.getId()));
                        break;
                }

                nodes.add(new com.agm.screens.NodeView(np, 0, 0));
                relayout.run();
            });

            parentDlg.hide();
            close();
        }

    }

    /* ------------------------------------------------------------------ */
    /*  Utilidades                                                        */
    /* ------------------------------------------------------------------ */

    private void showInfo(NodeView node) {
        Person p = node.getPerson();
        Dialog d = new Dialog("Info de " + p.getFirstName(), skin) {
            @Override
            protected void result(Object obj) {
                hide();
            }
        };
        d.getContentTable().pad(20).defaults().pad(10).left().width(300);
        d.getContentTable().add(new Label("Nombre:    " + p.getFirstName() + " " + p.getLastName(), skin)).row();
        d.getContentTable().add(new Label("Nacimiento: " + p.getBirthDate(), skin)).row();
        d.getContentTable().add(new Label("Defunción:  " + (p.getDeathDate() != null ? p.getDeathDate() : "—"), skin)).row();
        d.getContentTable().add(new Label("Nota:       " + (p.getQuote().isEmpty() ? "—" : "\"" + p.getQuote() + "\""), skin)).row();

        d.button("Cerrar", true).pad(15);
        d.key(Input.Keys.ESCAPE, true);

        d.show(stage).setPosition((stage.getViewport().getWorldWidth() - d.getWidth()) / 2, (stage.getViewport().getWorldHeight() - d.getHeight()) / 2);
    }

    /**
     * Cierra el menú contextual (público para EditorScreen)
     */
    public void close() {
        if (menu != null) {
            menu.remove();
            menu = null;
        }
    }

    public void dispose() {
        editor.dispose();
    }
}
