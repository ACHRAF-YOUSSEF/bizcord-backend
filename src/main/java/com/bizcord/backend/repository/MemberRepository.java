package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
	boolean existsByServerIdAndUserId(String serverId, String userId);

	@Query("""
			select m from bizcord_members m
			join fetch m.user
			join fetch m.server
			where m.id = :memberId
			""")
	Optional<Member> findByIdWithUserAndServer(String memberId);

	@Query("""
			select m from bizcord_members m
			join fetch m.user
			join fetch m.server
			where m.server.id = :serverId and m.user.id = :userId
			""")
	Optional<Member> findByServerIdAndUserIdWithUserAndServer(String serverId, String userId);

	@Query("""
			select m from bizcord_members m
			join fetch m.user
			join fetch m.server
			where m.server.id = :serverId
			""")
	List<Member> findAllByServerIdWithUserAndServer(String serverId);

	@Query("""
			select m from bizcord_members m
			join fetch m.user
			join fetch m.server
			where m.server.id in :serverIds
			""")
	List<Member> findAllByServerIdInWithUserAndServer(List<String> serverIds);
}
