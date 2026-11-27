package com.example.learningplatform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, unique = true)
    private Module module;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit;

    @OneToMany(
            mappedBy = "quiz",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private final Set<Question> questions = new LinkedHashSet<>();

    protected Quiz() {
    }

    public Quiz(Module module, String title, Integer timeLimit) {
        this.module = module;
        this.title = title;
        this.timeLimit = timeLimit;
        module.assignQuiz(this);
    }

    public Integer getId() {
        return id;
    }

    public Module getModule() {
        return module;
    }

    public String getTitle() {
        return title;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public void update(String title, Integer timeLimit) {
        this.title = title;
        this.timeLimit = timeLimit;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public void replaceQuestions(Set<Question> newQuestions) {
        questions.clear();
        questions.addAll(newQuestions);
    }
}
