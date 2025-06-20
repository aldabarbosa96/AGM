package com.agm.ui;

import com.agm.model.FamilyTree;
import com.agm.model.Relation;
import com.agm.model.RelationType;
import com.agm.screens.NodeView;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de distribución del árbol genealógico.
 * <p>
 * • El nodo que se creó primero dentro de la pareja (el “ancla”)
 * siempre queda a la izquierda y es el único que reparte hijos.
 * • La anchura de cada sub-árbol se calcula recursivamente para
 * centrar parejas e hijos y evitar solapes.
 */
public class TreeLayoutEngine {

    /* ────────── constantes geométricas ────────── */
    private static final float NODE_W = NodeView.RADIUS * 2 + 50f;
    private static final float PARTNER_GAP = NodeView.RADIUS * 1.5f;
    private static final float SIBLING_GAP = NodeView.RADIUS * 3f;

    /* ────────── estado ────────── */
    private final FamilyTree tree;
    private final List<NodeView> nodes;
    private final Stage stage;

    public TreeLayoutEngine(FamilyTree tree, List<NodeView> nodes, Stage stage) {
        this.tree = tree;
        this.nodes = nodes;
        this.stage = stage;
    }

    /* ───────────────────── layout completo ───────────────────── */

    public void layoutAll() {
        if (nodes.isEmpty()) return;

        /* 1 · raíces (si ambos cónyuges lo son, sólo el “ancla”) */
        List<NodeView> roots = findRoots();
        if (roots.isEmpty()) roots = Collections.singletonList(nodes.get(0));

        /* 2 · anchura recursiva de cada sub-árbol */
        Map<String, Float> wmap = new HashMap<>();
        float totalW = 0f, gapRoots = NODE_W;
        for (NodeView r : roots) totalW += computeWidth(r, wmap);
        totalW += gapRoots * (roots.size() - 1);

        /* 3 · colocar raíces centradas en la pantalla */
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();
        float currentX = vw / 2f - totalW / 2f;
        float startY = vh / 2f;

        for (NodeView r : roots) {
            float w = wmap.get(r.getPerson().getId());
            float cx = currentX + w / 2f;
            layoutSubtree(r, cx, startY, wmap);
            currentX += w + gapRoots;
        }

        /* 4 · separar hermanos que no comparten padres */
        placeSiblingRoots();
    }

    /* ───────────────────── cálculo de anchuras ───────────────────── */

    private float computeWidth(NodeView n, Map<String, Float> wmap) {
        float own = NODE_W;
        if (getSpouse(n) != null) own = NODE_W * 2 + PARTNER_GAP;

        List<NodeView> ch = getChildren(n);
        if (ch.isEmpty()) {
            wmap.put(n.getPerson().getId(), own);
            return own;
        }

        float kidsW = 0f;
        for (NodeView c : ch) kidsW += computeWidth(c, wmap);

        float finalW = Math.max(own, kidsW);
        wmap.put(n.getPerson().getId(), finalW);
        return finalW;
    }

    /* ───────────────────── distribución recursiva ───────────────────── */

    private void layoutSubtree(NodeView n, float x, float y, Map<String, Float> wmap) {

        NodeView spouse = getSpouse(n);
        boolean anchor = isAnchor(n);          // “ancla” = creado antes

        /* 1 · colocar (n + pareja) */
        float blockW = wmap.get(n.getPerson().getId());
        if (spouse != null) {
            float half = blockW / 2f;
            float xLeft = x - half + NODE_W / 2f;
            float xRight = x + half - NODE_W / 2f;

            if (anchor) {                      // n a la izquierda
                n.setPosition(xLeft, y);
                spouse.setPosition(xRight, y);
            } else {                           // spouse a la izquierda
                spouse.setPosition(xLeft, y);
                n.setPosition(xRight, y);
            }
        } else {
            n.setPosition(x, y);
        }

        /* 2 · sólo el ancla reparte a los hijos */
        if (spouse != null && !anchor) return;

        /* 3 · distribuir hijos centrados bajo la pareja */
        List<NodeView> children = getChildren(n);
        if (children.isEmpty()) return;

        float kidsW = 0f;
        for (NodeView c : children) kidsW += wmap.get(c.getPerson().getId());

        float startX = x - kidsW / 2f;
        float vgap = NodeView.RADIUS * 2 + 100f;

        for (NodeView c : children) {
            float cw = wmap.get(c.getPerson().getId());
            float cx = startX + cw / 2f;
            layoutSubtree(c, cx, y - vgap, wmap);
            startX += cw;
        }
    }

    /* ───────────────────── ajuste de hermanos “sueltos” ───────────────────── */

    private void placeSiblingRoots() {
        Set<String> done = new HashSet<>();
        for (Relation r : tree.getRelations()) {
            if (r.getType() != RelationType.SIBLING) continue;
            String a = r.getFromId(), b = r.getToId();
            String key = a.compareTo(b) < 0 ? a + ':' + b : b + ':' + a;
            if (done.contains(key) || haveCommonParent(a, b)) continue;
            done.add(key);

            NodeView na = findNode(a), nb = findNode(b);
            if (na == null || nb == null) continue;

            float anchor = rightmostOfCouple(na);
            float dx = (anchor + SIBLING_GAP) - nb.getX();
            shiftSubtree(nb, dx);
        }
    }

    /* ───────────────────── helpers ───────────────────── */

    /**
     * Devuelve true si {@code n} es el miembro “ancla” de su pareja.
     */
    private boolean isAnchor(NodeView n) {
        NodeView sp = getSpouse(n);
        return sp == null || nodes.indexOf(n) < nodes.indexOf(sp);
    }

    private List<NodeView> getChildren(NodeView p) {
        return tree.getRelations().stream().filter(r -> r.getType() == RelationType.PARENT && r.getFromId().equals(p.getPerson().getId())).map(r -> findNode(r.getToId())).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Raíces = nodos sin padre; si ambos cónyuges cumplen, conserva sólo al ancla.
     */
    private List<NodeView> findRoots() {

        Set<String> childIds = tree.getRelations().stream().filter(r -> r.getType() == RelationType.PARENT).map(Relation::getToId).collect(Collectors.toSet());

        List<NodeView> roots = new ArrayList<>();
        for (NodeView n : nodes) {
            String id = n.getPerson().getId();
            if (childIds.contains(id)) continue;          // tiene padre

            NodeView sp = getSpouse(n);
            if (sp != null && !childIds.contains(sp.getPerson().getId())) {
                if (!isAnchor(n)) continue;               // pareja pero no ancla
            }
            roots.add(n);
        }
        return roots;
    }

    private boolean haveCommonParent(String a, String b) {
        return tree.getRelations().stream().anyMatch(r -> r.getType() == RelationType.PARENT && isChildOf(r.getFromId(), a) && isChildOf(r.getFromId(), b));
    }

    private boolean isChildOf(String p, String c) {
        return tree.getRelations().stream().anyMatch(r -> r.getType() == RelationType.PARENT && r.getFromId().equals(p) && r.getToId().equals(c));
    }

    private NodeView getSpouse(NodeView n) {
        String id = n.getPerson().getId();
        for (Relation r : tree.getRelations()) {
            if (r.getType() == RelationType.SPOUSE && (r.getFromId().equals(id) || r.getToId().equals(id))) {
                String other = r.getFromId().equals(id) ? r.getToId() : r.getFromId();
                return findNode(other);
            }
        }
        return null;
    }

    private float rightmostOfCouple(NodeView n) {
        float max = n.getX();
        NodeView sp = getSpouse(n);
        if (sp != null) max = Math.max(max, sp.getX());
        return max + NODE_W / 2f;
    }

    private NodeView findNode(String id) {
        for (NodeView nv : nodes)
            if (nv.getPerson().getId().equals(id)) return nv;
        return null;
    }

    private void shiftSubtree(NodeView n, float dx) {
        n.setPosition(n.getX() + dx, n.getY());
        NodeView sp = getSpouse(n);
        if (sp != null) sp.setPosition(sp.getX() + dx, sp.getY());
        for (NodeView c : getChildren(n)) shiftSubtree(c, dx);
    }
}
