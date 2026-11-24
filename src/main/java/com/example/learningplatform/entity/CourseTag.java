package com.example.learningplatform.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_tag")
public class CourseTag {

    @EmbeddedId
    private CourseTagId id;

    @MapsId("courseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    protected CourseTag() {
    }

    public CourseTag(Course course, Tag tag) {
        this.id = new CourseTagId(course.getId(), tag.getId());
        this.course = course;
        this.tag = tag;
    }

    public CourseTagId getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public Tag getTag() {
        return tag;
    }
}
