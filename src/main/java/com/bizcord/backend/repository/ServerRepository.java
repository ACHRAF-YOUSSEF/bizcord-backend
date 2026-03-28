package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, String> {
	@Query("""
			select distinct s from discord_servers s
			join s.members currentMember
			left join fetch s.user
			where currentMember.user.id = :userId
			""")
	List<Server> findAllByMemberUserIdWithOwner(String userId);

	@Query("""
			select s from discord_servers s
			left join fetch s.user
			where s.id = :serverId
			""")
	Optional<Server> findByIdWithOwner(String serverId);

	@Query("""
			select s from discord_servers s
			left join fetch s.user
			where s.inviteCode = :inviteCode
			""")
	Optional<Server> findByInviteCodeWithOwner(String inviteCode);

	boolean existsByInviteCode(String inviteCode);
}
