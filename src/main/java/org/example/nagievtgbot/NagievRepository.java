package org.example.nagievtgbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NagievRepository extends JpaRepository<Messages, Long> {
    @Query("SELECT m FROM Messages m ORDER BY RANDOM() LIMIT 1")
    Messages findRandomMessage();
}
