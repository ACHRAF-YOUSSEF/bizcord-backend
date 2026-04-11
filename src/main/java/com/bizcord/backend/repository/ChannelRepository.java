package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, String> {

	@Query("""
			select c from bizcord_channels c
			join fetch c.server
			where c.id = :channelId
			""")
	Optional<Channel> findByIdWithServer(String channelId);
	@Query("""
			select c from bizcord_channels c
			join fetch c.user
			join fetch c.server
			left join fetch c.category
			where c.server.id = :serverId
			order by c.position asc
			""")
	List<Channel> findAllByServerIdWithUserAndServer(String serverId);

	@Query("""
			select c from bizcord_channels c
			join fetch c.user
			join fetch c.server
			left join fetch c.category
			where c.server.id in :serverIds
			order by c.position asc
			""")
	List<Channel> findAllByServerIdInWithUserAndServer(List<String> serverIds);
}
