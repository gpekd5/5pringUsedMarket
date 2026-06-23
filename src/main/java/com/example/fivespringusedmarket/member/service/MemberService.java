package com.example.fivespringusedmarket.member.service;

import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.member.dto.MemberMeResponse;
import com.example.fivespringusedmarket.member.entity.Member;
import com.example.fivespringusedmarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberMeResponse getInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return new MemberMeResponse(member.getId(), member.getEmail(), member.getNickname());
    }

    @Transactional
    public MemberMeResponse updateInfo(Long memberId, String nickname, String password) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (nickname != null) {
            validateDuplicatedNickname(nickname, memberId);
            member.updateNickname(nickname);
        }

        if (password != null) {
            member.updatePassword(passwordEncoder.encode(password));
        }

        return new MemberMeResponse(member.getId(), member.getEmail(), member.getNickname());
    }

    private void validateDuplicatedNickname(String nickname, Long memberId) {
        if (memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
        }
    }
}
