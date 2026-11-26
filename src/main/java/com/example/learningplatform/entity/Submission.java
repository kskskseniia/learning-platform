package com.example.learningplatform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "submission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private Integer score;

    @Column(columnDefinition = "text")
    private String feedback;

    protected Submission() {
    }

    public Submission(
            Assignment assignment,
            User student,
            LocalDateTime submittedAt,
            String content) {
        this.assignment = assignment;
        this.student = student;
        this.submittedAt = submittedAt;
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public User getStudent() {
        return student;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getContent() {
        return content;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
