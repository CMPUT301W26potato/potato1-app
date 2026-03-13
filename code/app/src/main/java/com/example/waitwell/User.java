package com.example.waitwell;

/**
 * Represents a user in the WaitWell application.
 *
 * This class stores basic contact information for a user,
 * including their name, email address, and phone number.
 * It is mainly used for storing and retrieving user data
 * from Firebase.
 *
 * @author Sarang Kim
 */
public class User {

    // The user's full name
    public String name;

    // The user's email address
    public String email;

    // The user's phone number
    public String phone;

    /**
     * Default constructor required for Firebase.
     *
     * Firebase needs an empty constructor in order to
     * automatically deserialize data into a User object
     * when retrieving it from the database.
     */
    public User() {
        // Required empty constructor for Firebase
    }

    /**
     * Creates a User object with the provided information.
     *
     * @param name  the user's full name
     * @param email the user's email address
     * @param phone the user's phone number
     */
    public User(String name, String email, String phone) {
        // Assign the provided values to the user's fields
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}

