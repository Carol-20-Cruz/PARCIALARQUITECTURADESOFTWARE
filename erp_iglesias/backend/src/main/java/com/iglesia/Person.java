package com.iglesia;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$",
             message = "El nombre solo puede contener letras")
    private String firstName;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúñÑ ]+$",
             message = "El apellido solo puede contener letras")
    private String lastName;

    private String email;
    private String phone;

    public Person() {}

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
