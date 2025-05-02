package com.bytesnova.jobs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bytesnova.jobs.models.Applicant;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

}
