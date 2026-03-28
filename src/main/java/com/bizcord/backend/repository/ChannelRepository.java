package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, String> {
	@Query("""
			select c from bizcord_channels c
			join fetch c.user
			join fetch c.server
			where c.server.id = :serverId
			""")
	List<Channel> findAllByServerIdWithUserAndServer(String serverId);

	@Query("""
			select c from bizcord_channels c
			join fetch c.user
			join fetch c.server
			where c.server.id in :serverIds
			""")
	List<Channel> findAllByServerIdInWithUserAndServer(List<String> serverIds);
}
