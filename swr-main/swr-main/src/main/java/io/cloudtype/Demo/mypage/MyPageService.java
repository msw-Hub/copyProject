package io.cloudtype.Demo.mypage;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.login.JoinDetailsDTO;
import io.cloudtype.Demo.login.JoinService;
import io.cloudtype.Demo.mypage.pet.PetDTO;
import io.cloudtype.Demo.mypage.pet.PetEntity;
import io.cloudtype.Demo.mypage.pet.PetRepository;
import io.cloudtype.Demo.mypage.user.PartnerEntity;
import io.cloudtype.Demo.mypage.user.PartnerRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MyPageService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PetRepository petRepository;
    private final PartnerRepository partnerRepository;
    private final JWTUtil jwtUtil;
    private final JoinService joinService;
    private final WebClient.Builder webClientBuilder;

    public MyPageService(UserRepository userRepository, JWTUtil jwtUtil, JoinService joinService,
                         BCryptPasswordEncoder bCryptPasswordEncoder, PetRepository petRepository,
                         PartnerRepository partnerRepository, WebClient.Builder webClientBuilder
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.joinService = joinService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.petRepository = petRepository;
        this.partnerRepository = partnerRepository;
        this.webClientBuilder = webClientBuilder;
    }
    public boolean checkPinNumber(String accessToken, int pinNumber) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken,1);
        return userRepository.findByUsername(username).getPinNumber() == pinNumber;
    }
    //username을 받아 nickname, phoneNumber, pinNumber를 가져와서 DTO로 반환
    public List<JoinDetailsDTO> getUserInfoByUsername(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
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
        UserEntity user = getUserEntity(accessToken);
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
        UserEntity user = getUserEntity(accessToken);
        // 새 비밀번호가 이전 비밀번호와 같은지 확인
        if (bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 이전 비밀번호와 달라야 합니다.");
        }
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);
    }
    @Transactional
    public boolean checkPassword(String accessToken, String password) {
        UserEntity user = getUserEntity(accessToken);
        return bCryptPasswordEncoder.matches(password, user.getPassword());
    }
    @Transactional
    public int addPetStep1(String accessToken, String petName, int petAge, String species,String gender, String imgURL) {
        UserEntity user = getUserEntity(accessToken);
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
        UserEntity user = getUserEntity(accessToken);
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
        UserEntity user = getUserEntity(accessToken);
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
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다");
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
        UserEntity user = getUserEntity(accessToken);
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
        UserEntity user = getUserEntity(accessToken);
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다");
        }
        petRepository.delete(pet);
    }
    public PetDTO getPet(String accessToken, int petId) {
        UserEntity user = getUserEntity(accessToken);
        PetEntity pet = petRepository.findById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("해당 반려동물을 찾을 수 없습니다");
        }
        if (pet.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 반려동물의 주인이 아닙니다");
        }
        return mapToDTO3(pet);
    }
    public int getCoin(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        return user.getCoin();
    }
    public int getPartnerApplyCheck(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        PartnerEntity partner = partnerRepository.findByUser_Id(user.getId());
        if (partner == null) {
            return 0;
        }
        return partner.getPartnerStep();
    }
    public int getPartnerTestCount(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        PartnerEntity partner = partnerRepository.findByUser_Id(user.getId());
        return partner.getTestCount();
    }
    public LocalDate getNextTestDate(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        PartnerEntity partner = partnerRepository.findByUser_Id(user.getId());
        LocalDate testDate = partner.getTestDate();

        return testDate.plusDays(3);
    }
    @Transactional
    public void identifyIdentification(String accessToken, MultipartFile identificationImage) {
        UserEntity user = getUserEntity(accessToken);
        String url = "http://43.201.218.68:8000/detect/";
        // Send image to Python server
        String result = webClientBuilder.build()
                .post()
                .uri(url)
                .body(BodyInserters.fromMultipartData("file", identificationImage.getResource()))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JSONObject jsonObject = new JSONObject(result);
        String ocrName = jsonObject.getString("name");
        String ocrBirth = jsonObject.getString("birth");
        String ocrGender = jsonObject.getString("gender");
        log.info("ocrName: " + ocrName);
        log.info("ocrBirth: " + ocrBirth);
        log.info("ocrGender: " + ocrGender);
        //이미지를 받아서 결과를 확인 및 비교
        String dbName = user.getName();
        String dbBirth = user.getBirth();
        String dbGender = user.getGender();
        if (!ocrName.equals(dbName) || !ocrBirth.equals(dbBirth) || !ocrGender.equals(dbGender)) {
            throw new IllegalArgumentException("인증에 실패하였습니다");
        }
        //결과가 일치하면 partnerStep을 1로 변경
        PartnerEntity partner = new PartnerEntity();
        partner.setUser(user);
        partner.setPartnerStep(1);
        partnerRepository.save(partner);
    }
    @Transactional
    public void addPartnerStep2(String accessToken, String imageUrl, String address,String career){
        UserEntity user = getUserEntity(accessToken);
        PartnerEntity partner = partnerRepository.findByUser_Id(user.getId());
        if (partner == null) {
            throw new IllegalArgumentException("파트너 정보를 찾을 수 없습니다");
        }
        if(partner.getPartnerStep() != 1){
            throw new IllegalArgumentException("신분증 인증을 먼저 해주세요");
        }
        partner.setPartnerProfileImage(imageUrl);
        partner.setAddress(address);
        partner.setCareer(career);
        partner.setPartnerStep(2);
        partnerRepository.save(partner);
    }
    @Transactional
    public int checkQuiz(String accessToken, QuizDTO quizDTO){
        UserEntity user = getUserEntity(accessToken);
        PartnerEntity partner = partnerRepository.findByUser_Id(user.getId());
        if (partner == null) {
            throw new IllegalArgumentException("파트너 정보를 찾을 수 없습니다");
        }
        //step2 -> step3
        if(partner.getPartnerStep() == 2 && partner.getTestCount() == 0){
            partner.setPartnerStep(3);  //파트너 등록 단계 3으로 변경
        }
        if (partner.getTestCount() > 3) {
            if(partner.getTestDate().plusDays(3).isBefore(LocalDate.now())) {
                partner.setTestCount(0);
            }else {
                throw new IllegalArgumentException("퀴즈 풀기 횟수를 초과하였습니다 시간이 지나고 다시 진행해주세요");
            }
        }
        partner.setTestCount(partner.getTestCount() + 1);
        partner.setTestDate(LocalDate.now());
        partnerRepository.save(partner);
        int score = getScore(quizDTO);
        if(score >=80){
            partner.setPartnerStep(5);
            partner.setPartnerDate(LocalDateTime.now());
            user.setPartnership(1);
            userRepository.save(user);
        }else if (partner.getTestCount()==3) partner.setPartnerStep(4);
        partnerRepository.save(partner);
        return score;
    }
    private static int getScore(QuizDTO quizDTO) {
        int score = 0;
        if(quizDTO.isQ1()) score+=5;
        if(!quizDTO.isQ2()) score+=5;
        if(!quizDTO.isQ3()) score+=5;
        if(quizDTO.isQ4()) score+=5;
        if(!quizDTO.isQ5()) score+=5;
        if(!quizDTO.isQ6()) score+=5;
        if(!quizDTO.isQ7()) score+=5;
        if(quizDTO.isQ8()) score+=5;
        if(quizDTO.isQ9()) score+=5;
        if(quizDTO.isQ10()) score+=5;
        if(quizDTO.getQ11()==4) score+=5;
        if(quizDTO.getQ12()==2) score+=5;
        if(quizDTO.getQ13()==4) score+=5;
        if(quizDTO.getQ14()==5) score+=5;
        if(quizDTO.getQ15()==5) score+=5;
        if(quizDTO.getQ16()==1) score+=5;
        if(quizDTO.getQ17()==3) score+=5;
        if(quizDTO.getQ18()==3) score+=5;
        if(quizDTO.getQ19()==1) score+=5;
        if(quizDTO.getQ20()==3) score+=5;
        return score;
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
