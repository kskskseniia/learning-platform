package com.example.learningplatform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 255)
    private String duration;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<CourseTag> courseTags = new LinkedHashSet<>();

    protected Course() {
    }

    public Course(
            String title,
            String description,
            Category category,
            User teacher,
            String duration,
            LocalDate startDate) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.teacher = teacher;
        this.duration = duration;
        this.startDate = startDate;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Set<CourseTag> getCourseTags() {
        return courseTags;
    }

    public void replaceTags(Iterable<Tag> tags) {
        Set<Tag> desiredTags = new LinkedHashSet<>();
        tags.forEach(desiredTags::add);
        Set<Integer> desiredTagIds = desiredTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        courseTags.removeIf(courseTag -> !desiredTagIds.contains(courseTag.getTag().getId()));
        Set<Integer> currentTagIds = courseTags.stream()
                .map(courseTag -> courseTag.getTag().getId())
                .collect(Collectors.toSet());
        desiredTags.stream()
                .filter(tag -> !currentTagIds.contains(tag.getId()))
                .forEach(tag -> courseTags.add(new CourseTag(this, tag)));
    }
}
