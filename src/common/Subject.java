package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String accessCode;
    private Timestamp startTime;
    private Timestamp endTime;
    private boolean isPublished;
    private String status; // PENDING_REVIEW, APPROVED_FOR_QUESTIONS, QUESTIONS_PENDING, PUBLISHED
    private int createdBy;

    public Subject(int id, String name, String accessCode, Timestamp startTime, Timestamp endTime,
            boolean isPublished, String status, int createdBy) {
        this.id = id;
        this.name = name;
        this.accessCode = accessCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPublished = isPublished;
        this.status = status != null ? status : "PENDING_REVIEW";
        this.createdBy = createdBy;
    }

    // Backward compatibility constructor
    public Subject(int id, String name, String accessCode, Timestamp startTime, Timestamp endTime,
            boolean isPublished) {
        this(id, name, accessCode, startTime, endTime, isPublished, "PENDING_REVIEW", 0);
    }

    // Legacy constructor for backward compatibility if needed (but we updated
    // calls)
    public Subject(int id, String name, String accessCode, Timestamp startTime, Timestamp endTime) {
        this(id, name, accessCode, startTime, endTime, false, "PENDING_REVIEW", 0);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public String getStatus() {
        return status;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return name;
    }
}
