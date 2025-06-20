package com.agm.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FamilyTree {
    private final Map<String, Person> people = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();

    public void addPerson(Person p) {
        people.put(p.getId(), p);
    }

    public void addRelation(String fromId, String toId, RelationType type) {
        relations.add(new Relation(fromId, toId, type));
    }

    public void addParentChild(String parentId, String childId) {
        addRelation(parentId, childId, RelationType.PARENT);
    }


    public Optional<Person> getPerson(String id) {
        return Optional.ofNullable(people.get(id));
    }

    // Sigue devolviendo la vista inmutable
    public List<Relation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    // Nuevo método para modificar la lista interna
    public List<Relation> getRelationsMutable() {
        return relations;
    }

    // Nuevo método para modificar el mapa interno
    public Map<String, Person> getPeople() {
        return people;
    }

    public Stream<Person> parentsOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.PARENT && r.getToId().equals(id)).map(r -> people.get(r.getFromId()));
    }

    public Stream<Person> childrenOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.PARENT && r.getFromId().equals(id)).map(r -> people.get(r.getToId()));
    }

    public Stream<Person> spousesOf(String id) {
        return relations.stream().filter(r -> r.getType() == RelationType.SPOUSE && (r.getFromId().equals(id) || r.getToId().equals(id))).map(r -> people.get(r.getFromId().equals(id) ? r.getToId() : r.getFromId()));
    }

    public Stream<Person> siblingsOf(String id) {
        // hermanos = tienen al menos un padre en común
        Set<String> commonParents = parentsOf(id).map(Person::getId).collect(Collectors.toSet());
        return relations.stream().filter(r -> r.getType() == RelationType.PARENT && commonParents.contains(r.getFromId()) && !r.getToId().equals(id)).map(r -> people.get(r.getToId())).distinct();
    }

    public void addRelationSafe(String from, String to, RelationType t){
        if(t==RelationType.PARENT && createsCycle(from,to))
            throw new IllegalArgumentException("Ciclo genealógico");
        relations.add(new Relation(from,to,t));
    }
    private boolean createsCycle(String parent,String child){
        if(parent.equals(child)) return true;
        return childrenOf(child).anyMatch(p -> createsCycle(parent,p.getId()));
    }

    /** Crea la relación SPOUSE en ambos sentidos A ↔ B  */
    public void addSpouse(String a, String b) {
        addRelationSafe(a, b, RelationType.SPOUSE);
        addRelationSafe(b, a, RelationType.SPOUSE);   // ida y vuelta
    }

    /** Crea la relación SIBLING en ambos sentidos A ↔ B */
    public void addSibling(String a, String b) {
        addRelationSafe(a, b, RelationType.SIBLING);
        addRelationSafe(b, a, RelationType.SIBLING);  // ida y vuelta
    }



}
