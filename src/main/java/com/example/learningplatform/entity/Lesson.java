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

@Entity
@Table(name = "lesson")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "video_url", length = 2048)
    private String videoUrl;

    protected Lesson() {
    }

    public Lesson(Module module, String title, String content, String videoUrl) {
        this.module = module;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
