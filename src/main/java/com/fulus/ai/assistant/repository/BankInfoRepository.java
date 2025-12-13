package com.fulus.ai.assistant.repository;

import com.fulus.ai.assistant.entity.BankInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankInfoRepository extends JpaRepository<BankInfo, Long> {

    Optional<BankInfo> findByBankCode(String bankCode);

    List<BankInfo> findByActiveTrue();
}
