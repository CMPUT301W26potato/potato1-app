package com.example.waitwell;

public class User {
    public String name;
    public String email;
    public String phone;

    public User() {
        // for Firebase
    }

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
