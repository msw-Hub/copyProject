package io.cloudtype.Demo.mypage;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.BlacklistEntity;
import io.cloudtype.Demo.mypage.user.BlacklistRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LogoutService {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final BlacklistRepository blacklistRepository;

    @Autowired
    public LogoutService(JWTUtil jwtUtil, UserRepository userRepository,
                         BlacklistRepository blacklistRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.blacklistRepository = blacklistRepository;
    }

    //블랙 리스트에 추가하고 나중에 비교함
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        UserEntity user = getUserEntity(accessToken);
        user.setRefreshToken(null);
        userRepository.save(user);
        //먼저 있는 지를 찾아서 없으면 추가
        if (blacklistRepository.existsByAccessToken(accessToken) || blacklistRepository.existsByRefreshToken(refreshToken)) {
            throw new IllegalStateException("이미 로그아웃된 사용자입니다");
        }
        BlacklistEntity blacklistEntity = new BlacklistEntity();
        blacklistEntity.setAccessToken(accessToken);
        blacklistEntity.setRefreshToken(refreshToken);
        blacklistRepository.save(blacklistEntity);
    }
    @Transactional
    public void signOut(String accessToken){
        UserEntity user = getUserEntity(accessToken);
        //회원 정보 삭제 전에 고려 해야할 사항
        //데이터베이스의 엔티티 설정을 cascade로 변경시, 어떻게 되는지를 고려해야한다

        //회원 정보 삭제
        userRepository.delete(user);
    }
    private UserEntity getUserEntity(String accessToken){
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}
