package com.agm.model;

import java.time.LocalDate;

public class Person {
    private final String id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private LocalDate deathDate;    // nuevo
    private String quote;           // nuevo

    public Person(String id, String firstName, String lastName, LocalDate birthDate, LocalDate deathDate, String quote) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.quote = quote;
    }

    // getters
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public String getQuote() {
        return quote;
    }

    // setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setBirthDate(LocalDate bd) {
        this.birthDate = bd;
    }

    public void setDeathDate(LocalDate dd) {
        this.deathDate = dd;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }
}
