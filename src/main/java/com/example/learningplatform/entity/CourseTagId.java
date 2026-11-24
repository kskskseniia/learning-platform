package com.example.learningplatform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CourseTagId implements Serializable {

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "tag_id")
    private Integer tagId;

    protected CourseTagId() {
    }

    public CourseTagId(Integer courseId, Integer tagId) {
        this.courseId = courseId;
        this.tagId = tagId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public Integer getTagId() {
        return tagId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CourseTagId that)) {
            return false;
        }
        return Objects.equals(courseId, that.courseId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, tagId);
    }
}
