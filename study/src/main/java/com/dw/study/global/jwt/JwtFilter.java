package com.dw.study.global.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
    실제로 이 컴포넌트를 이용하는 것은 인증 작업을 진행하는 Filter
    이 필터는 검증이 끝난 JWT로부터 유저정보를 받아와서 UsernamePasswordAuthenticationFilter 로 전달
 */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    /*
        실제 필터링 로직은 doFilterInternal 에 들어감
        JWT 토큰의 인증 정보를 현재 쓰레드의 SecurityContext 에 저장하는 역할 수행
        가입/로그인/재발급을 제외한 모든 Request 요청은 이 필터를 거치기 때문에 토큰 정보가 없거나 유효하지 않으면 정상적으로 수행되지 않음.
        그리고 요청이 정상적으로 Controller까지 도착했다면 SecurityContext에 Member ID 가 존재한다는 것이 보장됨.
        대신 직접 DB를 조회한 것이 아니라 Access Token에 있는 Member ID를 꺼낸 거라서, 탈퇴로 인해 Member ID 가 DB에 없는 경우 등 예외 상황은 Service 단에서 고려해야 함.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // 1. Request Header 에서 토큰을 꺼냄
        String jwt = resolveToken(request);

        // 2. validateToken 으로 토큰 유효성 검사
        // 정상 토큰이면 해당 토큰으로 Authentication 을 가져와서 SecurityContext 에 저장
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication authentication = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // Request Header 에서 토큰 정보를 꺼내오기
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.split(" ")[1].trim();
        }
        return null;
    }
}