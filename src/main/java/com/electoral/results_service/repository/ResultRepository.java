package com.electoral.results_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.electoral.results_service.entity.Result;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByElectionId(Long electionId);
}