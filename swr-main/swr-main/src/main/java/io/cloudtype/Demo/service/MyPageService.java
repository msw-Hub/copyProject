package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.JoinDetailsDTO;
import io.cloudtype.Demo.Dto.PetDTO;
import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.PetRepository;
import io.cloudtype.Demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MyPageService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PetRepository petRepository;
    private final JWTUtil jwtUtil;
    private final JoinService joinService;

    public MyPageService(UserRepository userRepository, JWTUtil jwtUtil, JoinService joinService,
                         BCryptPasswordEncoder bCryptPasswordEncoder, PetRepository petRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.joinService = joinService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.petRepository = petRepository;
    }
    public boolean checkPinNumber(String accessToken, int pinNumber) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken,1);
        return userRepository.findByUsername(username).getPinNumber() == pinNumber;
    }
    //username을 받아 nickname, phoneNumber, pinNumber를 가져와서 DTO로 반환
    public List<JoinDetailsDTO> getUserInfoByUsername(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return List.of(mapToDTO4(user));
    }
    private JoinDetailsDTO mapToDTO4(UserEntity entity) {
        JoinDetailsDTO dto = new JoinDetailsDTO();
        dto.setNickname(entity.getNickname());
        dto.setPinNumber(entity.getPinNumber());
        dto.setPhoneNumber(entity.getPhoneNumber());
        return dto;
    }
    @Transactional
    public void editInfo(String accessToken, String nickname, String phoneNumber, int pinNumber) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        // 닉네임 중복 체크 및 설정
        if (nickname != null && !nickname.isEmpty() && joinService.nicknameCheck(nickname) > 0) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다");
        }
        // 현재 사용 중인 핀번호인지 체크 및 설정
        if (pinNumber !=0 && pinNumber == user.getPinNumber()) {
            throw new IllegalArgumentException("현재 사용중인 핀번호입니다");
        }
        // 현재 사용 중인 전화번호인지 체크 및 설정
        if (phoneNumber != null && !phoneNumber.isEmpty() && phoneNumber.equals(user.getPhoneNumber())) {
            throw new IllegalArgumentException("현재 사용중인 전화번호입니다");
        }
        // 수정사항 적용
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            user.setPhoneNumber(phoneNumber);
        }
        if (pinNumber != 0 && pinNumber != user.getPinNumber()) {
            user.setPinNumber(pinNumber);
        }
        userRepository.save(user);
    }
    @Transactional
    public void editPassword(String accessToken, String password) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        // 새 비밀번호가 이전 비밀번호와 같은지 확인
        if (bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 이전 비밀번호와 달라야 합니다.");
        }
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);
    }
    @Transactional
    public boolean checkPassword(String accessToken, String password) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        return bCryptPasswordEncoder.matches(password, user.getPassword());
    }
    @Transactional
    public int addPetStep1(String accessToken, String petName, int petAge, String species,String gender, String imgURL) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        // 반려동물 생년 계산
        int currentYear = Year.now().getValue();
        int birthYear = currentYear - petAge;

        // 반려동물 정보 저장
        PetEntity pet = new PetEntity();
        pet.setOwner(user);
        pet.setPetName(petName);
        pet.setPetImage(imgURL);
        pet.setBirthYear(birthYear);
        pet.setGender(gender);
        pet.setSpecies(species);
        petRepository.save(pet);

        return pet.getId();
    }
    @Transactional
    public void addPetStep2(String accessToken, PetDTO petDTO) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        double weight = petDTO.getWeight();
        boolean neutering = petDTO.isNeutering();
        String animalHospital = petDTO.getAnimalHospital();
        String vaccination = petDTO.getVaccination();
        String etc = petDTO.getEtc();
        int petId = petDTO.getId();

        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다" + petId);
        }

        // 반려동물 추가 정보 저장
        pet.setWeight(weight);
        pet.setNeutering(neutering);
        pet.setAnimalHospital(animalHospital);
        pet.setVaccination(vaccination);
        pet.setEtc(etc);
        petRepository.save(pet);
    }
    public String findImageUrlById(int id) {
        return petRepository.findImageUrlById(id);
    }
    public void checkOwner(String accessToken, int petId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다");
        }
    }
    @Transactional
    public void editPetStep1(int petId, String petName, int petAge, String species,String gender, String imageUrl) {
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다.");
        }
        // 반려동물 생년 계산
        int currentYear = Year.now().getValue();
        int birthYear = currentYear - petAge;

        // 반려동물 정보 수정
        pet.setPetName(petName);
        pet.setPetImage(imageUrl);
        pet.setBirthYear(birthYear);
        pet.setSpecies(species);
        pet.setGender(gender);
        petRepository.save(pet);
    }
    public List<PetDTO> getPetList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        List<PetEntity> petList = petRepository.findByOwner(user);
        return petList.stream()
                .map(this::mapToDTO3)
                .collect(Collectors.toList());
    }
    private PetDTO mapToDTO3(PetEntity entity) {
        PetDTO dto = new PetDTO();
        dto.setId(entity.getId());
        dto.setPetName(entity.getPetName());
        dto.setPetImage(entity.getPetImage());
        dto.setBirthYear(entity.getBirthYear());
        dto.setSpecies(entity.getSpecies());
        dto.setGender(entity.getGender());
        dto.setWeight(entity.getWeight());
        dto.setNeutering(entity.isNeutering());
        dto.setAnimalHospital(entity.getAnimalHospital());
        dto.setVaccination(entity.getVaccination());
        dto.setEtc(entity.getEtc());
        return dto;
    }
    public void removePet(String accessToken, int petId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다.");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다.");
        }
        petRepository.delete(pet);
    }
    public PetDTO getPet(String accessToken, int petId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다.");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다.");
        }
        return mapToDTO3(pet);
    }
    public int getCoin(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return user.getCoin();
    }
}
