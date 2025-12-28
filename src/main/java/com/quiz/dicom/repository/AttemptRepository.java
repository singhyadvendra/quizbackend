package com.quiz.dicom.repository;

import com.quiz.dicom.domain.Attempt;
import com.quiz.dicom.domain.AttemptStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByUserIdAndQuizIdOrderByCreatedAtDesc(Long userId, Long quizId);

    List<Attempt> findByQuizIdAndStatusOrderBySubmittedAtDesc(Long quizId, AttemptStatus status);

    // Leaderboard: Top N submitted attempts by score desc, submittedAt desc
    List<Attempt> findByQuizIdAndStatusOrderByScoreDescSubmittedAtDesc(Long quizId, AttemptStatus status, Pageable pageable);

    Optional<Attempt> findByIdAndUser_Id(Long id, Long userId);

}
