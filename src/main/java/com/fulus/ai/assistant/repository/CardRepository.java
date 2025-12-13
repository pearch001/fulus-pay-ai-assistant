package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.Card;
import com.fulus.ai.assistant.enums.CardStatus;
import com.fulus.ai.assistant.enums.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByUserId(UUID userId);

    //List<Card> findByUserIdAndCardType(UUID userId, CardType cardType);

    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> findByUserIdAndStatus(UUID userId, CardStatus status);

    Optional<Card> findByUserIdAndCardType(UUID userId, CardType cardType);

    boolean existsByUserIdAndCardType(UUID userId, CardType cardType);
}
