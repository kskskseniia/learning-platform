package com.example.learningplatform.service;

import com.example.learningplatform.dto.CourseAnalyticsResponse;
import com.example.learningplatform.dto.LeaderboardEntryResponse;
import com.example.learningplatform.dto.QuizAnalyticsResponse;
import com.example.learningplatform.dto.StudentAnalyticsResponse;
import com.example.learningplatform.entity.Course;
import com.example.learningplatform.entity.Quiz;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.AnalyticsRepository;
import com.example.learningplatform.repository.CourseRepository;
import com.example.learningplatform.repository.QuizRepository;
import com.example.learningplatform.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            QuizRepository quizRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
    }

    public StudentAnalyticsResponse getStudentAnalytics(Integer studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));
        if (student.getRole() != Role.STUDENT) {
            throw new InvalidResourceStateException("Analytics is available only for students");
        }
        return analyticsRepository.getStudentAnalytics(studentId, student.getName());
    }

    public CourseAnalyticsResponse getCourseAnalytics(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return analyticsRepository.getCourseAnalytics(courseId, course.getTitle());
    }

    public QuizAnalyticsResponse getQuizAnalytics(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));
        return analyticsRepository.getQuizAnalytics(quizId, quiz.getTitle());
    }

    public List<LeaderboardEntryResponse> getCourseLeaderboard(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course", courseId);
        }
        return analyticsRepository.getCourseLeaderboard(courseId);
    }
}
