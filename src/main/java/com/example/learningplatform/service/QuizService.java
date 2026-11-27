package com.example.learningplatform.service;

import com.example.learningplatform.dto.AnswerOptionRequest;
import com.example.learningplatform.dto.AnswerOptionResponse;
import com.example.learningplatform.dto.QuestionRequest;
import com.example.learningplatform.dto.QuestionResponse;
import com.example.learningplatform.dto.QuizRequest;
import com.example.learningplatform.dto.QuizResponse;
import com.example.learningplatform.dto.QuizSubmissionResponse;
import com.example.learningplatform.dto.TakeQuizRequest;
import com.example.learningplatform.entity.AnswerOption;
import com.example.learningplatform.entity.EnrollmentStatus;
import com.example.learningplatform.entity.Module;
import com.example.learningplatform.entity.Question;
import com.example.learningplatform.entity.QuestionType;
import com.example.learningplatform.entity.Quiz;
import com.example.learningplatform.entity.QuizSubmission;
import com.example.learningplatform.entity.Role;
import com.example.learningplatform.entity.User;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.InvalidResourceStateException;
import com.example.learningplatform.exception.ResourceInUseException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.EnrollmentRepository;
import com.example.learningplatform.repository.ModuleRepository;
import com.example.learningplatform.repository.QuizRepository;
import com.example.learningplatform.repository.QuizSubmissionRepository;
import com.example.learningplatform.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public QuizService(
            QuizRepository quizRepository,
            ModuleRepository moduleRepository,
            QuizSubmissionRepository quizSubmissionRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository) {
        this.quizRepository = quizRepository;
        this.moduleRepository = moduleRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public QuizResponse create(Integer moduleId, QuizRequest request) {
        Module module = getModule(moduleId);
        verifyCourseTeacher(module, request.teacherId());
        if (quizRepository.existsByModule_Id(moduleId)) {
            throw new DuplicateResourceException("Quiz already exists for this module");
        }
        validateQuestions(request.questions());

        Quiz quiz = new Quiz(module, request.title(), request.timeLimit());
        addQuestions(quiz, request.questions());
        return toResponse(quizRepository.save(quiz));
    }

    public List<QuizResponse> findByModule(Integer moduleId) {
        getModule(moduleId);
        return quizRepository.findByModule_Id(moduleId)
                .map(quiz -> List.of(toResponse(quiz)))
                .orElseGet(List::of);
    }

    public QuizResponse findById(Integer id) {
        return toResponse(getQuizWithQuestions(id));
    }

    public List<QuizSubmissionResponse> findSubmissions(Integer id, Integer teacherId) {
        Quiz quiz = getQuizWithQuestions(id);
        verifyCourseTeacher(quiz.getModule(), teacherId);
        return quizSubmissionRepository.findAllByQuiz_IdOrderByIdAsc(id).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional
    public QuizResponse update(Integer id, QuizRequest request) {
        Quiz quiz = getQuizWithQuestionsForUpdate(id);
        verifyCourseTeacher(quiz.getModule(), request.teacherId());
        verifyQuizHasNoSubmissions(id);
        validateQuestions(request.questions());

        quiz.update(request.title(), request.timeLimit());
        quiz.replaceQuestions(new HashSet<>());
        addQuestions(quiz, request.questions());
        quizRepository.flush();
        return toResponse(quiz);
    }

    @Transactional
    public void delete(Integer id, Integer teacherId) {
        Quiz quiz = getQuizWithQuestionsForUpdate(id);
        verifyCourseTeacher(quiz.getModule(), teacherId);
        verifyQuizHasNoSubmissions(id);
        quiz.getModule().removeQuiz();
        quizRepository.delete(quiz);
        quizRepository.flush();
    }

    @Transactional
    public QuizSubmissionResponse submit(Integer quizId, TakeQuizRequest request) {
        Quiz quiz = getQuizWithQuestionsForUpdate(quizId);
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.studentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new InvalidResourceStateException("Only students can take quizzes");
        }
        Integer courseId = quiz.getModule().getCourse().getId();
        if (!enrollmentRepository.existsByStudent_IdAndCourse_IdAndStatus(
                student.getId(), courseId, EnrollmentStatus.ACTIVE)) {
            throw new InvalidResourceStateException(
                    "Active enrollment in the course is required");
        }

        int score = calculateScore(quiz, request.selectedAnswers());
        QuizSubmission submission = new QuizSubmission(
                quiz, student, score, LocalDateTime.now());
        return toSubmissionResponse(quizSubmissionRepository.save(submission));
    }

    private Module getModule(Integer id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", id));
    }

    private Quiz getQuizWithQuestions(Integer id) {
        return quizRepository.findWithQuestionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
    }

    private Quiz getQuizWithQuestionsForUpdate(Integer id) {
        quizRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
        return getQuizWithQuestions(id);
    }

    private void verifyQuizHasNoSubmissions(Integer id) {
        if (quizSubmissionRepository.existsByQuiz_Id(id)) {
            throw new ResourceInUseException(
                    "Quiz with submitted results cannot be changed or deleted");
        }
    }

    private void verifyCourseTeacher(Module module, Integer teacherId) {
        Integer courseTeacherId = module.getCourse().getTeacher().getId();
        if (!courseTeacherId.equals(teacherId)) {
            throw new InvalidResourceStateException(
                    "Only the course teacher can manage quizzes");
        }
    }

    private void validateQuestions(List<QuestionRequest> questions) {
        for (QuestionRequest question : questions) {
            long correctCount = question.answerOptions().stream()
                    .filter(option -> Boolean.TRUE.equals(option.correct()))
                    .count();
            if (question.type() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
                throw new InvalidResourceStateException(
                        "Single choice question must contain exactly one correct answer");
            }
            if (question.type() == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
                throw new InvalidResourceStateException(
                        "Multiple choice question must contain at least one correct answer");
            }
        }
    }

    private void addQuestions(Quiz quiz, List<QuestionRequest> requests) {
        for (QuestionRequest request : requests) {
            Question question = new Question(quiz, request.text(), request.type());
            for (AnswerOptionRequest optionRequest : request.answerOptions()) {
                question.addAnswerOption(new AnswerOption(
                        question, optionRequest.text(), optionRequest.correct()));
            }
            quiz.addQuestion(question);
        }
    }

    private int calculateScore(Quiz quiz, Map<Integer, List<Integer>> selectedAnswers) {
        Map<Integer, Question> questionsById = new HashMap<>();
        for (Question question : quiz.getQuestions()) {
            questionsById.put(question.getId(), question);
        }
        for (Map.Entry<Integer, List<Integer>> entry : selectedAnswers.entrySet()) {
            Question question = questionsById.get(entry.getKey());
            if (question == null) {
                throw new InvalidResourceStateException(
                        "Selected question does not belong to the quiz");
            }
            validateSelectedOptions(question, entry.getValue());
        }

        long correctQuestions = quiz.getQuestions().stream()
                .filter(question -> isCorrectAnswer(
                        question, selectedAnswers.getOrDefault(question.getId(), List.of())))
                .count();
        return (int) (correctQuestions * 100 / quiz.getQuestions().size());
    }

    private void validateSelectedOptions(Question question, List<Integer> selectedOptionIds) {
        if (selectedOptionIds == null) {
            throw new InvalidResourceStateException("Selected answer list must not be null");
        }
        Set<Integer> selected = new HashSet<>(selectedOptionIds);
        if (selected.size() != selectedOptionIds.size()) {
            throw new InvalidResourceStateException("Selected answers must not contain duplicates");
        }
        if (question.getType() == QuestionType.SINGLE_CHOICE && selected.size() > 1) {
            throw new InvalidResourceStateException(
                    "Single choice question accepts at most one answer");
        }
        Set<Integer> available = question.getAnswerOptions().stream()
                .map(AnswerOption::getId)
                .collect(Collectors.toSet());
        if (!available.containsAll(selected)) {
            throw new InvalidResourceStateException(
                    "Selected answer option does not belong to the question");
        }
    }

    private boolean isCorrectAnswer(Question question, List<Integer> selectedOptionIds) {
        Set<Integer> selected = new HashSet<>(selectedOptionIds);
        Set<Integer> correct = question.getAnswerOptions().stream()
                .filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId)
                .collect(Collectors.toSet());
        return selected.equals(correct);
    }

    private QuizResponse toResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(question -> new QuestionResponse(
                        question.getId(),
                        question.getText(),
                        question.getType(),
                        question.getAnswerOptions().stream()
                                .map(option -> new AnswerOptionResponse(
                                        option.getId(), option.getText()))
                                .toList()))
                .toList();
        return new QuizResponse(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getTitle(),
                quiz.getTimeLimit(),
                questions);
    }

    private QuizSubmissionResponse toSubmissionResponse(QuizSubmission submission) {
        return new QuizSubmissionResponse(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getStudent().getId(),
                submission.getScore(),
                submission.getTakenAt());
    }
}
