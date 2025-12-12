package common;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String role;

    // New Fields
    private String fullName;
    private String department;
    private int score;
    private boolean hasSubmitted;

    public User(int id, String username, String role, String fullName, String department) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.department = department;
        this.score = 0;
        this.hasSubmitted = false;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean hasSubmitted() {
        return hasSubmitted;
    }

    public void setHasSubmitted(boolean hasSubmitted) {
        this.hasSubmitted = hasSubmitted;
    }
}
