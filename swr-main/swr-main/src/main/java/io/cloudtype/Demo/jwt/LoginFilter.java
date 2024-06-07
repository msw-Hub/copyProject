package io.cloudtype.Demo.jwt;

import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    //JWTUtil 주입
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, UserRepository userRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        //클라이언트 요청에서 username, password 추출
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        log.info("username: " + username + " password: " + password);

        //스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);

        //token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        //UserDetailsS
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();

        String accessToken = jwtUtil.createAccessToken(username, role, 36000000L); //10시간
        String refreshToken = jwtUtil.createRefreshToken(username, role, 60*60*24*7*1000L); //1주일

        // 리프레시 토큰을 데이터베이스에 저장
        UserEntity user = userRepository.findByUsername(username);
        int partner = user.getPartnership();
        String profileImage = user.getProfileImage();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        String email = user.getUsername();
        String nickname;
        String encodedNickname;
        //데이터베이스에 JoinDetails가 저장되어있는지를 확인
        String birth = user.getBirth();
        if(birth == null) {
            //JoinDetails가 없는 경우
            response.addHeader("JoinDetails", "false");
        } else {
            //JoinDetails가 있는 경우
            response.addHeader("JoinDetails", "true");
            // URL 인코딩하여 닉네임을 헤더에 추가
            nickname = user.getNickname();
            encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8);
            response.addHeader("nickname", encodedNickname);
            log.info("nickname: " + encodedNickname);
        }

        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("refresh_token", refreshToken);
        response.addHeader("partnership", String.valueOf(partner));
        response.addHeader("profileImage", profileImage);
        response.addHeader("email", email);
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {

        //로그인 실패시 401 응답 코드 반환
        response.setStatus(401);
    }
}
