package io.cloudtype.Demo.login;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JoinService {

    private final UserRepository userRepository;

    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final KakaoService kakaoService;

    public JoinService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, KakaoService kakaoService, JWTUtil jwtUtil) {

        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.kakaoService = kakaoService;
        this.jwtUtil = jwtUtil;
    }

    public boolean joinProcess(JoinDTO joinDTO) {

        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();
        String name = joinDTO.getName();
        String gender = joinDTO.getGender();
        //0=회원가입, 1=일반로그인, 2=카카오로그인
        int isExist = kakaoService.isUsernameTaken(username);

        if (isExist>0) {
            return false;
        }
        UserEntity data = new UserEntity();
        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setRole("ROLE_USER");
        data.setName(name);
        data.setGender(gender);
        data.setProfileImage("https://storage.googleapis.com/swr-bucket/default.jpg");
        data.setKakaoLogin(false);
        data.setPartnership(0);
        userRepository.save(data);
        return true;
    }
    public int usernameCheck(String username) {
        return kakaoService.isUsernameTaken(username);
    }
    public int nicknameCheck(String nickname) {
        return userRepository.countByNickname(nickname);
    }
    public boolean signUp(JoinDetailsDTO joinDetailsDTO, String accessToken) {
        String nickname = joinDetailsDTO.getNickname();
        int pinNumber = joinDetailsDTO.getPinNumber();
        String phoneNumber = joinDetailsDTO.getPhoneNumber();
        String birth = joinDetailsDTO.getBirth();

        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);

        if (nicknameCheck(nickname) > 0) {
            return false;
        }
        UserEntity user = userRepository.findByUsername(username);
        user.setNickname(nickname);
        user.setPinNumber(pinNumber);
        user.setPhoneNumber(phoneNumber);
        user.setBirth(birth);
        userRepository.save(user);
        return true;
    }
}
