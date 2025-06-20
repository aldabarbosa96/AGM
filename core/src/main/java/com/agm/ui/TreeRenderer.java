// core/src/main/java/com/agm/ui/TreeRenderer.java
package com.agm.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.agm.screens.NodeView;
import com.agm.model.Relation;
import com.agm.model.RelationType;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TreeRenderer {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera cam;
    private final BitmapFont font;
    private final float nameOffset = 30f;

    public TreeRenderer(Stage stage) {
        this.cam = (OrthographicCamera) stage.getCamera();
        this.font = new BitmapFont();
        this.font.getData().setScale(2f);
    }

    // Ahora recibe selectedNode
    public void render(List<NodeView> nodes, List<Relation> relations, NodeView selected) {
        shapes.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        drawConnections(nodes, relations);
        drawNodes(nodes, selected);
        drawLabels(nodes);
    }

    private void drawConnections(List<NodeView> nodes, List<Relation> relations) {
        shapes.begin(ShapeRenderer.ShapeType.Line);

        /* ----------- líneas padre-hijo (ya existentes) ----------- */
        Map<String, List<NodeView>> groups = new HashMap<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.PARENT) continue;
            NodeView child = find(nodes, r.getToId());
            if (child != null)
                groups.computeIfAbsent(r.getFromId(), k -> new java.util.ArrayList<>()).add(child);
        }
        for (Map.Entry<String, List<NodeView>> e : groups.entrySet()) {
            NodeView parent = find(nodes, e.getKey());
            List<NodeView> kids = e.getValue();
            if (parent == null || kids.isEmpty()) continue;

            float px = parent.getX(), py = parent.getY();
            float cy = py - NodeView.RADIUS - 20;
            shapes.setColor(Color.WHITE);
            shapes.line(px, py - NodeView.RADIUS, px, cy);

            float fx = kids.get(0).getX();
            float lx = kids.get(kids.size() - 1).getX();
            shapes.line(fx, cy, lx, cy);

            for (NodeView c : kids) {
                shapes.line(c.getX(), cy, c.getX(), c.getY() + NodeView.RADIUS + nameOffset);
            }
        }

        /* ----------- líneas cónyuge ↔ cónyuge ----------- */
        shapes.setColor(Color.LIGHT_GRAY);
        for (Relation r : relations) {
            if (r.getType() != RelationType.SPOUSE) continue;
            NodeView a = find(nodes, r.getFromId());
            NodeView b = find(nodes, r.getToId());
            if (a != null && b != null) {
                shapes.line(a.getX() + NodeView.RADIUS, a.getY(), b.getX() - NodeView.RADIUS, b.getY());
            }
        }

        /* (opcional) podrías añadir aquí conexiones de hermanos si lo deseas */

        shapes.end();
    }


    private void drawNodes(List<NodeView> nodes, NodeView selected) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NodeView nv : nodes) {
            nv.draw(shapes, nv == selected);
        }
        shapes.end();
    }

    private void drawLabels(List<NodeView> nodes) {
        batch.begin();
        for (NodeView nv : nodes) {
            String name = nv.getPerson().getFirstName();
            GlyphLayout gl = new GlyphLayout(font, name);
            float x = nv.getX() - gl.width / 2f;
            float y = nv.getY() + NodeView.RADIUS + nameOffset;
            font.draw(batch, gl, x, y);
        }
        batch.end();
    }

    private NodeView find(List<NodeView> nodes, String id) {
        for (NodeView n : nodes) if (n.getPerson().getId().equals(id)) return n;
        return null;
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
