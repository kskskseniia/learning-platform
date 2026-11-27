package com.example.learningplatform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.learningplatform.repository.QuizRepository;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

class QuizMappingTests {

    @Test
    void mapsOnlyFieldsFromErDiagram() {
        assertThat(Quiz.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "module", "title", "timeLimit", "questions");
        assertThat(Question.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "quiz", "text", "type", "answerOptions");
        assertThat(AnswerOption.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "question", "text", "correct");
        assertThat(QuizSubmission.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "quiz", "student", "score", "takenAt");
    }

    @Test
    void mapsAllQuizAssociationsAsLazy() throws NoSuchFieldException {
        assertThat(oneToOne(Module.class, "quiz").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToOne(Quiz.class, "module").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToMany(Quiz.class, "questions").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne(Question.class, "quiz").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToMany(Question.class, "answerOptions").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne(AnswerOption.class, "question").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne(QuizSubmission.class, "quiz").fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne(QuizSubmission.class, "student").fetch()).isEqualTo(FetchType.LAZY);
    }

    @Test
    void cascadesOnlyOwnedQuizContent() throws NoSuchFieldException {
        OneToMany questions = oneToMany(Quiz.class, "questions");
        OneToMany answerOptions = oneToMany(Question.class, "answerOptions");
        OneToOne moduleQuiz = oneToOne(Module.class, "quiz");

        assertThat(questions.cascade()).containsExactly(CascadeType.ALL);
        assertThat(questions.orphanRemoval()).isTrue();
        assertThat(answerOptions.cascade()).containsExactly(CascadeType.ALL);
        assertThat(answerOptions.orphanRemoval()).isTrue();
        assertThat(moduleQuiz.cascade()).containsExactly(CascadeType.ALL);
        assertThat(moduleQuiz.orphanRemoval()).isTrue();
        assertThat(manyToOne(QuizSubmission.class, "quiz").cascade()).isEmpty();
        assertThat(manyToOne(QuizSubmission.class, "student").cascade()).isEmpty();
    }

    @Test
    void storesQuestionTypeAsText() throws NoSuchFieldException {
        Enumerated type = Question.class.getDeclaredField("type").getAnnotation(Enumerated.class);

        assertThat(type.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void locksQuizBeforeConcurrentWriteOperations() throws NoSuchMethodException {
        Lock lock = QuizRepository.class
                .getMethod("findByIdForUpdate", Integer.class)
                .getAnnotation(Lock.class);

        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private OneToOne oneToOne(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        return entityType.getDeclaredField(fieldName).getAnnotation(OneToOne.class);
    }

    private OneToMany oneToMany(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        return entityType.getDeclaredField(fieldName).getAnnotation(OneToMany.class);
    }

    private ManyToOne manyToOne(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        return entityType.getDeclaredField(fieldName).getAnnotation(ManyToOne.class);
    }
}
