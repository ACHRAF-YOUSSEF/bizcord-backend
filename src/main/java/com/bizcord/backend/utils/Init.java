package com.bizcord.backend.utils;

import com.bizcord.backend.repository.ChannelRepository;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;

//@Component
@RequiredArgsConstructor
public class Init implements CommandLineRunner {
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(String... args) throws Exception {
        memberRepository.deleteAll();
        channelRepository.deleteAll();
        serverRepository.deleteAll();
    }
}
