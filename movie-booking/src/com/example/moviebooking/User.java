package com.example.moviebooking;

import java.util.Objects;

public class User {
    private final String email;
    private final String name;
    private final String phone;

    public User(String email, String name, String phone) {
        this.email = Objects.requireNonNull(email);
        this.name = Objects.requireNonNull(name);
        this.phone = Objects.requireNonNull(phone);
    }

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return email.equals(user.email);
    }

    @Override
    public int hashCode() { return email.hashCode(); }

    @Override
    public String toString() { return "User{" + name + ", " + email + "}"; }
}
