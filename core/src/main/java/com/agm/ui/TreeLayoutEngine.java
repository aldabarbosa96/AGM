// core/src/main/java/com/agm/ui/TreeLayoutEngine.java
package com.agm.ui;

import com.agm.model.FamilyTree;
import com.agm.model.Relation;
import com.agm.model.RelationType;
import com.agm.screens.NodeView;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de distribución de nodos:
 * <p>
 * 1. Encuentra las raíces (sin padre) y centra cada sub-árbol.
 * 2. Distribuye recursivamente de arriba abajo.
 * 3. Ajusta parejas (muy cerca) y hermanos sin padre común (más lejos).
 * <p>
 * ✱ La anchura de un “bloque conyugal” (nodo + pareja) se integra ahora en los
 * cálculos, evitando solapes entre pareja y hermanos.
 */
public class TreeLayoutEngine {

    /* ── constantes de geometría ───────────────────────────────────────── */
    private static final float NODE_W = NodeView.RADIUS * 2 + 50f;   // ancho de un nodo
    private static final float PARTNER_GAP = NodeView.RADIUS * 1.5f;      // hueco entre cónyuges
    private static final float SIBLING_GAP = NodeView.RADIUS * 3f;        // hueco entre hermanos sin padre

    /* ------------------------------------------------------------------- */
    private final FamilyTree tree;
    private final List<NodeView> nodes;
    private final Stage stage;

    public TreeLayoutEngine(FamilyTree tree, List<NodeView> nodes, Stage stage) {
        this.tree = tree;
        this.nodes = nodes;
        this.stage = stage;
    }

    /* ─────────────────────────  API pública  ─────────────────────────── */

    public void layoutAll() {
        if (nodes.isEmpty()) return;

        /* 1 ─ raíces posibles (si no hubiese, usamos el primero) */
        List<NodeView> roots = findRoots();
        if (roots.isEmpty()) roots = Collections.singletonList(nodes.get(0));

        /* 2 ─ anchura de cada sub-árbol (incluye parejas) */
        Map<String, Float> wmap = new HashMap<>();
        float totalW = 0f, gapRoots = NODE_W;                 // hueco entre árboles raíz

        for (NodeView r : roots) {
            totalW += computeWidth(r, wmap);
        }
        totalW += gapRoots * (roots.size() - 1);

        /* 3 ─ distribuir raíces centradas */
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

        /* 4 ─ ajustes finos */
        placeSpouses();
        placeSiblingRoots();
    }

    private float computeWidth(NodeView n, Map<String, Float> map) {

        // anchura del propio nodo o del bloque nodo+pareja
        float ownBlock = NODE_W;
        if (getSpouse(n) != null) {
            ownBlock = NODE_W * 2 + PARTNER_GAP;
        }

        // hijos directos
        List<NodeView> children = getChildren(n);
        if (children.isEmpty()) {
            map.put(n.getPerson().getId(), ownBlock);
            return ownBlock;
        }

        // anchura total de los hijos
        float kidsW = 0f;
        for (NodeView c : children) kidsW += computeWidth(c, map);

        float finalW = Math.max(ownBlock, kidsW);
        map.put(n.getPerson().getId(), finalW);
        return finalW;
    }

    private void layoutSubtree(NodeView n, float x, float y, Map<String, Float> map) {
        n.setPosition(x, y);

        List<NodeView> children = getChildren(n);
        if (children.isEmpty()) return;

        float subtreeW = map.get(n.getPerson().getId());
        float startX = x - subtreeW / 2f;
        float vgap = NodeView.RADIUS * 2 + 100f;

        for (NodeView c : children) {
            float cw = map.get(c.getPerson().getId());
            float cx = startX + cw / 2f;
            layoutSubtree(c, cx, y - vgap, map);
            startX += cw;
        }
    }

    /* ─────────────────────  Ajuste de parejas  ───────────────────────── */

    /**
     * Bloque conyugal: se juntan mucho (PARTNER_GAP)
     */
    private void placeSpouses() {
        Set<String> done = new HashSet<>();

        for (Relation r : tree.getRelations()) {
            if (r.getType() != RelationType.SPOUSE) continue;

            String aId = r.getFromId(), bId = r.getToId();
            String key = aId.compareTo(bId) < 0 ? aId + '-' + bId : bId + '-' + aId;
            if (done.contains(key)) continue;
            done.add(key);

            NodeView a = findNode(aId);
            NodeView b = findNode(bId);
            if (a == null || b == null) continue;

            float y = Math.max(a.getY(), b.getY());
            a.setPosition(a.getX(), y);
            b.setPosition(a.getX() + PARTNER_GAP + NODE_W, y);
        }
    }

    /* ───────── Hermanos sin padre: separación larga en la misma fila ── */

    // ──────────────────────────────────────────────────────────────
// 3. Sustituye COMPLETAMENTE este método
// ──────────────────────────────────────────────────────────────
    private void placeSiblingRoots() {
        Set<String> done = new HashSet<>();

        for (Relation r : tree.getRelations()) {
            if (r.getType() != RelationType.SIBLING) continue;

            String aId = r.getFromId(), bId = r.getToId();
            String key = aId.compareTo(bId) < 0 ? aId + ':' + bId : bId + ':' + aId;
            if (done.contains(key) || haveCommonParent(aId, bId)) continue;
            done.add(key);

            NodeView a = findNode(aId);
            NodeView b = findNode(bId);
            if (a == null || b == null) continue;

            // referencia: extremo derecho del bloque “a + pareja”
            float anchor = rightmostOfCouple(a);

            // nueva posición objetivo para b
            float targetX = anchor + SIBLING_GAP;
            float dx = targetX - b.getX();

            shiftSubtree(b, dx);
        }
    }


    /* ────────────────────────  Helpers  ─────────────────────────────── */

    private List<NodeView> getChildren(NodeView parent) {
        List<NodeView> out = new ArrayList<>();
        for (Relation r : tree.getRelations())
            if (r.getType() == RelationType.PARENT && r.getFromId().equals(parent.getPerson().getId())) {

                NodeView child = findNode(r.getToId());
                if (child != null) out.add(child);
            }
        return out;
    }

    /**
     * Raíces = nodos que nunca aparecen como hijo en PARENT.
     */
    // ──────────────────────────────────────────────────────────────
// 1. Sustituye COMPLETAMENTE este método
// ──────────────────────────────────────────────────────────────
    private List<NodeView> findRoots() {
        // hijos directos de alguien → NO son raíz
        Set<String> childIds = tree.getRelations().stream()
            .filter(r -> r.getType() == RelationType.PARENT)
            .map(Relation::getToId)
            .collect(Collectors.toSet());

        Set<String> skip = new HashSet<>();
        for (Relation r : tree.getRelations()) {
            if (r.getType() != RelationType.SPOUSE) continue;

            String a = r.getFromId(), b = r.getToId();
            boolean aIsRoot = !childIds.contains(a);
            boolean bIsRoot = !childIds.contains(b);
            if (aIsRoot && bIsRoot) {
                if (a.compareTo(b) < 0) skip.add(b);   // conserva el “menor”
                else                     skip.add(a);
            }
        }

        List<NodeView> roots = new ArrayList<>();
        for (NodeView n : nodes) {
            String id = n.getPerson().getId();
            if (!childIds.contains(id) && !skip.contains(id))
                roots.add(n);
        }
        return roots;
    }


    private boolean haveCommonParent(String a, String b) {
        for (Relation r : tree.getRelations())
            if (r.getType() == RelationType.PARENT && isChildOf(r.getFromId(), a) && isChildOf(r.getFromId(), b))
                return true;
        return false;
    }

    private boolean isChildOf(String p, String c) {
        for (Relation r : tree.getRelations())
            if (r.getType() == RelationType.PARENT && r.getFromId().equals(p) && r.getToId().equals(c))
                return true;
        return false;
    }

    /**
     * Devuelve el primer cónyuge encontrado o {@code null}.
     */
    private NodeView getSpouse(NodeView n) {
        String id = n.getPerson().getId();
        for (Relation r : tree.getRelations())
            if (r.getType() == RelationType.SPOUSE && (r.getFromId().equals(id) || r.getToId().equals(id))) {

                String other = r.getFromId().equals(id) ? r.getToId() : r.getFromId();
                return findNode(other);
            }
        return null;
    }

    /**
     * Extremo X del bloque (n + pareja(s) a su derecha).
     */
    private float rightmostOfCouple(NodeView n) {
        float max = n.getX();
        NodeView spouse = getSpouse(n);
        if (spouse != null) max = Math.max(max, spouse.getX());
        return max + NODE_W / 2f;   // borde derecho del bloque
    }

    private NodeView findNode(String id) {
        for (NodeView nv : nodes)
            if (nv.getPerson().getId().equals(id)) return nv;
        return null;
    }

    private void shiftSubtree(NodeView n, float dx) {
        // mueve el propio nodo
        n.setPosition(n.getX() + dx, n.getY());

        // mueve al cónyuge si existe
        NodeView sp = getSpouse(n);
        if (sp != null) sp.setPosition(sp.getX() + dx, sp.getY());

        // mueve recursivamente a los hijos
        for (NodeView c : getChildren(n)) shiftSubtree(c, dx);
    }

}
