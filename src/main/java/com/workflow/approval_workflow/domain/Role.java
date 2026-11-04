package com.workflow.approval_workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A role and its capabilities. Capabilities live in the database so no code branches on role names. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "override_allowed", nullable = false)
    private boolean overrideAllowed;

    @Column(name = "view_all_allowed", nullable = false)
    private boolean viewAllAllowed;

    protected Role() {
        // required by JPA
    }

    public Role(String name, boolean overrideAllowed, boolean viewAllAllowed) {
        this.name = name;
        this.overrideAllowed = overrideAllowed;
        this.viewAllAllowed = viewAllAllowed;
    }

    public String getName() {
        return name;
    }

    public boolean isOverrideAllowed() {
        return overrideAllowed;
    }

    public boolean isViewAllAllowed() {
        return viewAllAllowed;
    }
}
