package com.dw.study.member.controller;


import com.dw.study.global.utils.SecurityUtil;
import com.dw.study.member.dto.MemberResponseDto;
import com.dw.study.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberResponseDto> findMemberInfoById() {
        return ResponseEntity.ok(memberService.findMemberInfoById(SecurityUtil.getCurrentMemberId()));
    }

    @GetMapping("/{memberName}")
    public ResponseEntity<MemberResponseDto> findMemberInfoByMemberName(@PathVariable String memberName) {
        return ResponseEntity.ok(memberService.findMemberInfoByMemberName(memberName));
    }
}