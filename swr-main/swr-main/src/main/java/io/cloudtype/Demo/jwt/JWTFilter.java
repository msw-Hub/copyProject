package io.cloudtype.Demo.jwt;

import io.cloudtype.Demo.mypage.user.BlacklistRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final BlacklistRepository blacklistRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository, BlacklistRepository blacklistRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.split(" ")[1];

        //엑세스 토큰이 유효하지 않은 경우
        if (jwtUtil.isExpired(accessToken, 1)) {
            String refreshToken = request.getHeader("X-Refresh-Token");
            //리프레시 토큰이 없는 경우
            if(refreshToken == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken not found");
                return;
            }
            //블랙리스트인 리프레시 토큰인가
            if(blacklistRepository.existsByRefreshToken(refreshToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Blacklisted refreshToken");
                return;
            }
            //리프레시 토큰의 유효기간이 끝난 경우 - 재로그인
            if(jwtUtil.isExpired(refreshToken, 2)) {
                response.setStatus(402);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\":\"RefreshToken expired. Retry Login\"}");
                response.getWriter().flush();
                return;
            }
            String username = jwtUtil.getUsername(refreshToken,2);
            UserEntity user = userRepository.findByUsername(username);
            String refreshTokenDB = user.getRefreshToken();
            //데이터 베이스에 저장된 리프레시 토큰과 일치하지 않은 경우
            if (!refreshToken.equals(refreshTokenDB)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken mismatch");
                return;
            }
            // 리프레시 토큰과 엑세스 토큰의 유저 정보 일치 여부 확인
            String expiredUsername = jwtUtil.getUsername(accessToken, 1);
            if (!username.equals(expiredUsername)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Username mismatch");
                return;
            }

            String newAccessToken = jwtUtil.createAccessToken(username, user.getRole(), 36000000L);

            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.setHeader("refresh_token", refreshToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization, refresh_token");
            response.setStatus(401);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"New access token issued. Retry with the new token.\"}");
            response.getWriter().flush();
            return;
        }
        //블랙리스트인 엑세스 토큰인가
        if(blacklistRepository.existsByAccessToken(accessToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Blacklisted accessToken");
            return;
        }

        String username = jwtUtil.getUsername(accessToken,1);
        String role = jwtUtil.getRole(accessToken,1);
        String password = jwtUtil.getPassword(accessToken);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userEntity.setRole(role);

        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
