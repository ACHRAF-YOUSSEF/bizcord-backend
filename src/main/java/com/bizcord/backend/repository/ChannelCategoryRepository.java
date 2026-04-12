package com.bizcord.backend.repository;

import com.bizcord.backend.entity.ChannelCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChannelCategoryRepository extends JpaRepository<ChannelCategory, String> {

    @Query("""
            select cc from bizcord_channel_categories cc
            join fetch cc.server
            where cc.server.id = :serverId
            order by cc.position asc
            """)
    List<ChannelCategory> findAllByServerIdWithServer(String serverId);

    @Query("""
            select cc from bizcord_channel_categories cc
            join fetch cc.server
            where cc.id = :id
            """)
    Optional<ChannelCategory> findByIdWithServer(String id);

    long countByServerId(String serverId);
}
