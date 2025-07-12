package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.PollVote;
import com.linknest.linknest.entity.PollOption;
import com.linknest.linknest.entity.User;
import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollOption(PollOption pollOption);
    Optional<PollVote> findByUserAndPollOption(User user, PollOption pollOption);
    boolean existsByUserAndPollOption(User user, PollOption pollOption);
} 