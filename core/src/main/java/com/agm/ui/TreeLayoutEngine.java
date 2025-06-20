// core/src/main/java/com/agm/ui/TreeLayoutEngine.java
package com.agm.ui;

import com.agm.model.FamilyTree;
import com.agm.screens.NodeView;
import com.agm.model.Relation;
import com.agm.model.RelationType;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.*;

public class TreeLayoutEngine {
    private final FamilyTree tree;
    private final List<NodeView> nodes;
    private final Stage stage;

    public TreeLayoutEngine(FamilyTree tree, List<NodeView> nodes, Stage stage) {
        this.tree = tree;
        this.nodes = nodes;
        this.stage = stage;
    }

    public void layoutAll() {
        if (nodes.isEmpty()) return;

        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();
        NodeView root = nodes.get(0);

        Map<String, Float> widthMap = new HashMap<>();
        computeWidth(root, widthMap);

        float startX = vw / 2f;
        float startY = vh / 2f;
        layoutSubtree(root, startX, startY, widthMap);

        /* ─── ajuste horizontal de cónyuges una vez colocado el árbol ─── */
        placeSpouses();
    }


    private float computeWidth(NodeView node, Map<String, Float> wmap) {
        List<NodeView> children = getChildren(node);
        if (children.isEmpty()) {
            float w = NodeView.RADIUS * 2 + 50f;
            wmap.put(node.getPerson().getId(), w);
            return w;
        }
        float total = 0;
        for (NodeView c : children) {
            total += computeWidth(c, wmap);
        }
        wmap.put(node.getPerson().getId(), total);
        return total;
    }

    private void layoutSubtree(NodeView node, float x, float y, Map<String, Float> wmap) {
        node.setPosition(x, y);
        List<NodeView> children = getChildren(node);
        if (children.isEmpty()) return;

        float subtreeW = wmap.get(node.getPerson().getId());
        float startX = x - subtreeW/2f;
        float vgap = NodeView.RADIUS*2 + 100f;

        for (NodeView c : children) {
            float cw = wmap.get(c.getPerson().getId());
            float cx = startX + cw/2f;
            layoutSubtree(c, cx, y - vgap, wmap);
            startX += cw;
        }
    }

    private List<NodeView> getChildren(NodeView parent) {
        List<NodeView> out = new ArrayList<>();
        for (Relation r : tree.getRelations()) {
            if (r.getType() == RelationType.PARENT &&
                r.getFromId().equals(parent.getPerson().getId())) {
                for (NodeView nv : nodes) {
                    if (nv.getPerson().getId().equals(r.getToId())) {
                        out.add(nv);
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** Coloca a los cónyuges uno al lado del otro en la misma fila */
    private void placeSpouses() {
        float gap = NodeView.RADIUS * 2 + 50f;           // distancia mínima

        for (Relation r : tree.getRelations()) {
            if (r.getType() != RelationType.SPOUSE) continue;

            NodeView a = findNode(r.getFromId());
            NodeView b = findNode(r.getToId());
            if (a == null || b == null) continue;

            /* Si ya están cerca, no tocamos nada */
            if (Math.abs(a.getY() - b.getY()) < 1f &&
                Math.abs(a.getX() - b.getX()) < gap * 0.9f) continue;

            /* Colocamos b a la derecha de a */
            b.setPosition(a.getX() + gap, a.getY());
        }
    }

    private NodeView findNode(String id) {
        for (NodeView n : nodes)
            if (n.getPerson().getId().equals(id)) return n;
        return null;
    }

}
