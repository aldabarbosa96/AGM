package com.agm.model;

import java.util.*;

public class FamilyTree {
    private final Map<String, Person> people = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();

    public void addPerson(Person p) {
        people.put(p.getId(), p);
    }

    public void addRelation(String parentId, String childId) {
        relations.add(new Relation(parentId, childId));
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
}
