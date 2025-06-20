package com.agm.ui;

import com.agm.model.FamilyTree;
import com.agm.model.Person;
import com.agm.screens.NodeView;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.List;

/**
 * Muestra el menú contextual de un nodo y despacha las acciones
 * (ver, editar, añadir padre/hijo).
 */
public class NodeMenuController {

    private final Stage stage;
    private final FamilyTree tree;
    private final List<NodeView> nodes;
    private final Skin skin;
    private final PersonEditorDialog editor;
    private final Runnable relayout;          // callback para recalcular layout
    private Table menu;                       // menú actualmente visible

    public NodeMenuController(Stage stage, FamilyTree tree, List<NodeView> nodes, Skin skin, Runnable relayout) {
        this.stage = stage;
        this.tree = tree;
        this.nodes = nodes;
        this.skin = skin;
        this.relayout = relayout;
        this.editor = new PersonEditorDialog(stage, skin);
    }

    /* --------------------------------------------------------------------- */

    public void show(NodeView node) {
        close();                               // cierra menú anterior si existe

        menu = new Table(skin);
        menu.setBackground("white");
        menu.pad(20).defaults().pad(10).minWidth(160).minHeight(45);

        TextButton viewBtn = new TextButton("Ver info", skin, "big");
        TextButton editBtn = new TextButton("Editar nodo", skin, "big");
        TextButton childBtn = new TextButton("Añadir hijo", skin, "big");
        TextButton parentBtn = new TextButton("Añadir padre", skin, "big");

        menu.add(viewBtn).row();
        menu.add(editBtn).row();
        menu.add(childBtn).row();
        menu.add(parentBtn);

        /* ----------- Callbacks ------------------------------------------- */

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

        childBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editor.create(node, np -> {
                    tree.addPerson(np);
                    tree.addParentChild(node.getPerson().getId(), np.getId());
                    nodes.add(new NodeView(np, 0, 0));
                    relayout.run();
                });
                close();
            }
        });

        parentBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                editor.create(node, np -> {
                    tree.addPerson(np);
                    tree.addParentChild(np.getId(), node.getPerson().getId());
                    nodes.add(new NodeView(np, 0, 0));
                    relayout.run();
                });
                close();
            }
        });

        /* Posición debajo del nodo */
        menu.pack();
        menu.setPosition(node.getX() - menu.getWidth() / 2f, node.getY() - NodeView.RADIUS - menu.getHeight() - 20);
        stage.addActor(menu);
    }

    private void showInfo(NodeView node) {
        Person p = node.getPerson();
        Dialog dlg = new Dialog("Info de " + p.getFirstName(), skin) {
            @Override
            protected void result(Object obj) {
                hide();
            }
        };
        dlg.getContentTable().pad(20).defaults().pad(10).left().width(300);
        dlg.getContentTable().add(new Label("Nombre:    " + p.getFirstName() + " " + p.getLastName(), skin)).row();
        dlg.getContentTable().add(new Label("Nacimiento: " + p.getBirthDate(), skin)).row();
        dlg.getContentTable().add(new Label("Defunción:  " + (p.getDeathDate() != null ? p.getDeathDate() : "—"), skin)).row();
        dlg.getContentTable().add(new Label("Nota:       " + (p.getQuote().isEmpty() ? "—" : "\"" + p.getQuote() + "\""), skin)).row();

        dlg.button("Cerrar", true).pad(15);
        dlg.key(Input.Keys.ESCAPE, true);

        dlg.show(stage).setPosition((stage.getViewport().getWorldWidth() - dlg.getWidth()) / 2, (stage.getViewport().getWorldHeight() - dlg.getHeight()) / 2);
    }

    private void close() {
        if (menu != null) {
            menu.remove();
            menu = null;
        }
    }

    public void dispose() {
        editor.dispose();
    }
}
