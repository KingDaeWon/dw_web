package com.dw.study.member.service;

import com.dw.study.global.jwt.TokenProvider;
import com.dw.study.member.dto.MemberRequestDto;
import com.dw.study.member.dto.MemberResponseDto;
import com.dw.study.member.dto.TokenDto;
import com.dw.study.member.dto.TokenRequestDto;
import com.dw.study.member.entity.Member;
import com.dw.study.member.entity.RefreshToken;
import com.dw.study.member.repository.MemberRepository;
import com.dw.study.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // ID, PW를 받아서 회원가입을 진행, ID의 경우 이미 존재하면 예외발생
    @Transactional
    public MemberResponseDto signup(MemberRequestDto memberRequestDto) {
        if (memberRepository.existsByMemberName(memberRequestDto.getMemberName())) {
            throw new RuntimeException("이미 가입되어 있는 유저입니다");
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        return MemberResponseDto.of(memberRepository.save(member));
    }
    /*
        Authentication
         - 사용자가 입력한 Login ID, PW로 인증 정보 객체 UsernamePasswordAuthenticationToken를 생성
         - 아직 인증이 완료된 객체가 아니며 AuthenticationManager에서 authenticate 메서드의 파라미터로 넘겨서 검증 후에 Authentication를 받는다.
        AuthenticationManager
         - 스프링 시큐리티에서 실제로 인증이 이루어지는 곳이다.
         - authenticate 메서드 하나만 정의되어 있는 인터페이스며 위 코드에서는 Builder에서 유저 정보가 서로 일치하는지 검사한다.
        인증이 완료된 authentication 에는 Member ID 가 들어있다.
        인증 객체를 바탕으로 Access Token + Refresh Token을 생성한다.
        Refresh Token 은 저장하고, 생성된 토큰 정보를 클라이언트에게 전달한다.
     */
    @Transactional
    public TokenDto login(MemberRequestDto memberRequestDto) {
        // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
        UsernamePasswordAuthenticationToken authenticationToken = memberRequestDto.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build();

        refreshTokenRepository.save(refreshToken);

        // 5. 토큰 발급
        return tokenDto;
    }

    /*
        Access Token + Refresh Token을 Request Body에 받아서 검증한다.
        Refresh Token의 만료 여부를 먼저 검사한다.
        Access Token을 복호화하여 유저 정보 (Member ID)를 가져오고 저장소에 있는 Refresh Token과 클라이언트가 전달한 Refresh Token의 일치 여부를 검사한다.
        만약 일치한다면 로그인했을 때와 동일하게 새로운 토큰을 생성해서 클라이언트에게 전달한다.
        Refresh Token 은 재사용하지 못하게 저장소에서 값을 갱신해준다.
     */
    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 이 유효하지 않습니다.");
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshToken = refreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return tokenDto;
    }
}