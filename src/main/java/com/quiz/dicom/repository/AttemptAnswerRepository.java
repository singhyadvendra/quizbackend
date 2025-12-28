package com.quiz.dicom.repository;

import com.quiz.dicom.domain.AttemptAnswer;
import com.quiz.dicom.domain.AttemptAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, AttemptAnswerId> {

    List<AttemptAnswer> findByIdAttemptId(Long attemptId);

    List<AttemptAnswer> findByIdAttemptIdAndIdQuestionId(Long attemptId, Long questionId);

    boolean existsByIdAttemptIdAndIdQuestionId(Long attemptId, Long questionId);

    @Query("""
      select aa
      from AttemptAnswer aa
      where aa.id.attemptId = :attemptId
        and aa.id.questionId in :questionIds
    """)
    List<AttemptAnswer> findByAttemptAndQuestionIds(@Param("attemptId") Long attemptId,
                                                    @Param("questionIds") List<Long> questionIds);
}
