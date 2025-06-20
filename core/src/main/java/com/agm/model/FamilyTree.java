package com.agm.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FamilyTree {

    /* ───────── datos ───────── */
    private final Map<String, Person> people = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();

    /* ───────── CRUD de personas ───────── */

    public void addPerson(Person p) {
        people.put(p.getId(), p);
    }

    public Optional<Person> getPerson(String id) {
        return Optional.ofNullable(people.get(id));
    }

    public Map<String, Person> getPeople() {
        return people;
    }

    /* ───────── acceso a relaciones ───────── */

    /**
     * Vista inmutable para la UI
     */
    public List<Relation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    /**
     * Vista mutable (solo para persistencia)
     */
    public List<Relation> getRelationsMutable() {
        return relations;
    }

    /* ───────── consultas ───────── */

    public Stream<Person> parentsOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.PARENT && r.getToId().equals(id)).map(r -> people.get(r.getFromId())).filter(Objects::nonNull);
    }

    public Stream<Person> childrenOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.PARENT && r.getFromId().equals(id)).map(r -> people.get(r.getToId())).filter(Objects::nonNull);
    }

    public Stream<Person> spousesOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.SPOUSE && (r.getFromId().equals(id) || r.getToId().equals(id))).map(r -> people.get(r.getFromId().equals(id) ? r.getToId() : r.getFromId())).filter(Objects::nonNull);
    }

    public Stream<Person> siblingsOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.SIBLING && r.getFromId().equals(id)).map(r -> people.get(r.getToId())).filter(Objects::nonNull);
    }

    /* ───────── creación de vínculos ───────── */

    /**
     * Añade un vínculo padre→hijo evitando ciclos y duplicados
     */
    /**
     * Añade el vínculo padre → hijo evitando ciclos y duplicados.
     * ▸ Si el hijo tiene hermanos, el nuevo padre se enlaza con todos ellos.
     * ▸ Si el padre tiene cónyuge(s), éstos pasan a ser también progenitores
     *   del hijo (y, por herencia, de los hermanos).
     */
    public void addParentChild(String parentId, String childId) {

        /* 0 · seguridad: nada de ciclos genealógicos */
        if (createsCycle(parentId, childId))
            throw new IllegalArgumentException("Crearía un ciclo genealógico");

        /* 1 · vínculo principal (se evita duplicado internamente) */
        link(parentId, childId, RelationType.PARENT);

        /* 2 · PROPAGAR A HERMANOS ———————— */
        siblingsOf(childId)
            .map(Person::getId)
            .filter(sibId -> !hasRelation(parentId, sibId, RelationType.PARENT))
            .forEach(sibId -> link(parentId, sibId, RelationType.PARENT));

        /* 3 · PROPAGAR A CÓNYUGE(S) DEL PADRE ———— */
        spousesOf(parentId)
            .map(Person::getId)
            .filter(spId -> !hasRelation(spId, childId, RelationType.PARENT))
            .forEach(spId -> link(spId, childId, RelationType.PARENT));
    }

    /**
     * Añade matrimonio bidireccional evitando duplicados
     */
    public void addSpouse(String a, String b) {
        link(a, b, RelationType.SPOUSE);
        link(b, a, RelationType.SPOUSE);
    }

    /**
     * Añade hermandad bidireccional, hereda padres y
     * hace transitiva la relación (A-B, B-C ⇒ A-C).
     */
    public void addSibling(String a, String b) {
        // 1 · vínculo directo
        link(a, b, RelationType.SIBLING);
        link(b, a, RelationType.SIBLING);

        // 2 · heredar padres
        List<String> parentsA = parentsOf(a).map(Person::getId).collect(Collectors.toList());
        List<String> parentsB = parentsOf(b).map(Person::getId).collect(Collectors.toList());

        for (String p : parentsA) addParentChild(p, b);
        for (String p : parentsB) addParentChild(p, a);

        // 3 · transitividad  ──────────────
        List<String> sibsA = siblingsOf(a).map(Person::getId).collect(Collectors.toList());
        List<String> sibsB = siblingsOf(b).map(Person::getId).collect(Collectors.toList());

        for (String s : sibsA) {
            link(s, b, RelationType.SIBLING);
            link(b, s, RelationType.SIBLING);
        }
        for (String s : sibsB) {
            link(s, a, RelationType.SIBLING);
            link(a, s, RelationType.SIBLING);
        }
    }

    /* ───────── utilidades internas ───────── */

    private boolean hasRelation(String from, String to, RelationType t) {
        return relations.stream().anyMatch(r -> r.getType() == t && r.getFromId().equals(from) && r.getToId().equals(to));
    }

    private void link(String from, String to, RelationType t) {
        if (!hasRelation(from, to, t)) relations.add(new Relation(from, to, t));
    }

    private boolean createsCycle(String parent, String child) {
        if (parent.equals(child)) return true;
        return childrenOf(child).anyMatch(p -> createsCycle(parent, p.getId()));
    }

}
