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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

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

        /* ────────── PADRE ↧ HIJO ────────── */
        Map<String, List<NodeView>> groups = new HashMap<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.PARENT) continue;
            NodeView child = find(nodes, r.getToId());
            if (child != null)
                groups.computeIfAbsent(r.getFromId(), k -> new java.util.ArrayList<>()).add(child);
        }
        shapes.setColor(Color.WHITE);
        for (Map.Entry<String, List<NodeView>> e : groups.entrySet()) {
            NodeView parent = find(nodes, e.getKey());
            List<NodeView> kids = e.getValue();
            if (parent == null || kids.isEmpty()) continue;

            float px = parent.getX(), py = parent.getY();
            float cy = py - NodeView.RADIUS - 20;

            shapes.line(px, py - NodeView.RADIUS, px, cy);          // tronco
            shapes.line(kids.get(0).getX(), cy, kids.get(kids.size() - 1).getX(), cy);         // barra
            for (NodeView c : kids)                                  // ramas
                shapes.line(c.getX(), cy, c.getX(), c.getY() + NodeView.RADIUS + nameOffset);
        }

        /* ────────── CÓNYUGE ↔ CÓNYUGE ────────── */
        shapes.setColor(Color.LIGHT_GRAY);
        Set<String> drawn = new HashSet<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.SPOUSE) continue;
            String aId = r.getFromId(), bId = r.getToId();
            String key = aId.compareTo(bId) < 0 ? aId + "-" + bId : bId + "-" + aId;
            if (drawn.contains(key)) continue;
            drawn.add(key);
            NodeView a = find(nodes, aId);
            NodeView b = find(nodes, bId);
            if (a != null && b != null) {
                shapes.line(a.getX() + NodeView.RADIUS, a.getY(), b.getX() - NodeView.RADIUS, b.getY());
            }
        }


    /* ────────── HERMANO ──────────
       Solo si NO comparten padre (ya representados arriba) */
        shapes.setColor(Color.CYAN);
        java.util.Set<String> done = new java.util.HashSet<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.SIBLING) continue;

            String aId = r.getFromId(), bId = r.getToId();
            String key = aId.compareTo(bId) < 0 ? aId + "-" + bId : bId + "-" + aId;
            if (done.contains(key)) continue;
            done.add(key);

            if (shareParent(relations, aId, bId)) continue;

            NodeView a = find(nodes, aId);
            NodeView b = find(nodes, bId);
            if (a == null || b == null) continue;

            float y = a.getY() + NodeView.RADIUS + 10f;      // barra sobre nodos
            shapes.line(a.getX(), a.getY() + NodeView.RADIUS, a.getX(), y);
            shapes.line(b.getX(), b.getY() + NodeView.RADIUS, b.getX(), y);
            shapes.line(a.getX(), y, b.getX(), y);
        }

        shapes.end();
    }

    /* Helpers */

    private boolean shareParent(List<Relation> rels, String a, String b) {
        for (Relation r : rels) {
            if (r.getType() != RelationType.PARENT) continue;
            if (r.getToId().equals(a)) {
                String p = r.getFromId();
                for (Relation r2 : rels)
                    if (r2.getType() == RelationType.PARENT && r2.getFromId().equals(p) && r2.getToId().equals(b))
                        return true;
            }
        }
        return false;
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
