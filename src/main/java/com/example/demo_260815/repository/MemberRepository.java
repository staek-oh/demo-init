package com.example.demo_260815.repository;

import com.example.demo_260815.domain.Member;
import com.example.demo_260815.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
