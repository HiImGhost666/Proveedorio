package com.proveedorio.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa los roles de usuario en el sistema.
 * Se utiliza para la gestión de permisos y autorizaciones.
 * Tabla y columnas en base de datos en español (roles, nombre, fecha_creacion).
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Column(name = "nombre", nullable = false, unique = true)
    private String name;

    @NotNull(message = "La fecha de creación del rol debe estar definida")
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    public Role() {
        this.createdAt = LocalDateTime.now();
    }

    public Role(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
