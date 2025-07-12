package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Message;
import com.linknest.linknest.entity.User;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderAndRecipientOrderByCreatedAtDesc(User sender, User recipient);
    List<Message> findByRecipientOrderByCreatedAtDesc(User recipient);
    // Fetch all messages between two users (both directions)
    List<Message> findBySenderAndRecipientOrRecipientAndSenderOrderByCreatedAtAsc(User sender1, User recipient1, User sender2, User recipient2);
} 