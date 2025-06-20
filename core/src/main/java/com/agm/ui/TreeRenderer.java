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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    public void render(List<NodeView> nodes, List<Relation> relations, NodeView selected) {
        shapes.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        drawConnections(nodes, relations);
        drawNodes(nodes, selected);
        drawLabels(nodes);
    }

    private void drawConnections(List<NodeView> nodes, List<Relation> relations) {
        shapes.begin(ShapeRenderer.ShapeType.Line);

        // 1) Recogemos para cada hijo la lista de sus padres directos
        Map<String, List<String>> childToParents = new HashMap<>();
        for (Relation r : relations) {
            if (r.getType() == RelationType.PARENT) {
                childToParents.computeIfAbsent(r.getToId(), k -> new ArrayList<>()).add(r.getFromId());
            }
        }

        // 2) Ampliamos esa lista para que incluya SIEMPRE a los cónyuges de cada padre
        for (Map.Entry<String, List<String>> e : new ArrayList<>(childToParents.entrySet())) {
            List<String> direct = e.getValue();
            Set<String> extended = new TreeSet<>(direct);  // TreeSet para orden y unicidad
            for (String pid : direct) {
                for (Relation r : relations) {
                    if (r.getType() == RelationType.SPOUSE && (r.getFromId().equals(pid) || r.getToId().equals(pid))) {
                        String mate = r.getFromId().equals(pid) ? r.getToId() : r.getFromId();
                        extended.add(mate);
                    }
                }
            }
            // Reemplazo la lista por la extendida
            childToParents.put(e.getKey(), new ArrayList<>(extended));
        }

        // 3) Grupos de hijos que comparten exactamente el mismo set de padres
        Map<Set<String>, List<NodeView>> familyGroups = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : childToParents.entrySet()) {
            String childId = entry.getKey();
            Set<String> parents = new TreeSet<>(entry.getValue());
            NodeView childView = find(nodes, childId);
            if (childView == null) continue;
            familyGroups.computeIfAbsent(parents, k -> new ArrayList<>()).add(childView);
        }

        // 4) Dibujamos cada familia: tronco vertical desde el centro de la pareja, barra y ramitas
        shapes.setColor(Color.WHITE);
        for (Map.Entry<Set<String>, List<NodeView>> grp : familyGroups.entrySet()) {
            Set<String> parents = grp.getKey();
            List<NodeView> kids = grp.getValue();
            if (kids.isEmpty()) continue;

            // A) ordeno hijos por X
            Collections.sort(kids, new Comparator<NodeView>() {
                @Override
                public int compare(NodeView a, NodeView b) {
                    return Float.compare(a.getX(), b.getX());
                }
            });

            // B) calculo centro X + Y del bloque conyugal (o padre único)
            float midX = 0, maxY = Float.NEGATIVE_INFINITY;
            for (String pid : parents) {
                NodeView pv = find(nodes, pid);
                if (pv != null) {
                    midX += pv.getX();
                    maxY = Math.max(maxY, pv.getY());
                }
            }
            midX /= parents.size();
            float trunkTopY = maxY;              // centro de la línea conyugal

            // C) tronco vertical
            float cy = trunkTopY - 20f;
            shapes.line(midX, trunkTopY, midX, cy);

            // D) barra horizontal de hijos
            float x0 = kids.get(0).getX();
            float x1 = kids.get(kids.size() - 1).getX();
            shapes.line(x0, cy, x1, cy);

            // E) ramitas a cada hijo
            for (NodeView kid : kids) {
                shapes.line(kid.getX(), cy, kid.getX(), kid.getY() + NodeView.RADIUS + nameOffset);
            }
        }

        // 5) Conexiones de cónyuges (para la línea horizontal original)
        shapes.setColor(Color.LIGHT_GRAY);
        Set<String> drawnPair = new HashSet<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.SPOUSE) continue;
            String a = r.getFromId(), b = r.getToId();
            String key = a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
            if (drawnPair.contains(key)) continue;
            drawnPair.add(key);
            NodeView na = find(nodes, a), nb = find(nodes, b);
            if (na != null && nb != null) {
                shapes.line(na.getX() + NodeView.RADIUS, na.getY(), nb.getX() - NodeView.RADIUS, nb.getY());
            }
        }

        // 6) Hermanos (si no comparten padre)
        shapes.setColor(Color.CYAN);
        Set<String> seenSib = new HashSet<>();
        for (Relation r : relations) {
            if (r.getType() != RelationType.SIBLING) continue;
            String a = r.getFromId(), b = r.getToId();
            String key = a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
            if (seenSib.contains(key)) continue;
            seenSib.add(key);
            if (shareParent(relations, a, b)) continue;
            NodeView na = find(nodes, a), nb = find(nodes, b);
            if (na == null || nb == null) continue;
            float y = na.getY() + NodeView.RADIUS + 10f;
            shapes.line(na.getX(), na.getY() + NodeView.RADIUS, na.getX(), y);
            shapes.line(nb.getX(), nb.getY() + NodeView.RADIUS, nb.getX(), y);
            shapes.line(na.getX(), y, nb.getX(), y);
        }

        shapes.end();
    }

    private boolean shareParent(List<Relation> rels, String a, String b) {
        for (Relation r1 : rels) {
            if (r1.getType() == RelationType.PARENT && r1.getToId().equals(a)) {
                String p = r1.getFromId();
                for (Relation r2 : rels) {
                    if (r2.getType() == RelationType.PARENT && r2.getFromId().equals(p) && r2.getToId().equals(b)) {
                        return true;
                    }
                }
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
        for (NodeView nv : nodes) {
            if (nv.getPerson().getId().equals(id)) return nv;
        }
        return null;
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
