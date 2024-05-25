package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.Care.CareUsageDetailsDTO;
import io.cloudtype.Demo.Dto.Care.CareUsageSimpleDTO;
import io.cloudtype.Demo.Dto.Walk.WalkUsageDetailsDTO;
import io.cloudtype.Demo.Dto.Walk.WalkUsageSimpleDTO;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.CareRecordEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.entity.Walk.WalkRecordEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.CarePostRepository;
import io.cloudtype.Demo.repository.Care.CareRecordRepository;
import io.cloudtype.Demo.repository.UserRepository;
import io.cloudtype.Demo.repository.Walk.WalkRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsageDetailsService {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final WalkRecordRepository walkRecordRepository;
    private final CareRecordRepository careRecordRepository;
    private final CarePostRepository carePostRepository;
    @Autowired
    public UsageDetailsService(UserRepository userRepository, JWTUtil jwtUtil, WalkRecordRepository walkRecordRepository,
                               CareRecordRepository careRecordRepository, CarePostRepository carePostRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.walkRecordRepository = walkRecordRepository;
        this.careRecordRepository = careRecordRepository;
        this.carePostRepository = carePostRepository;
    }
    //유저의 산책이용내역보기
    public List<WalkUsageSimpleDTO> getUserWalkUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<WalkRecordEntity> walkRecodeList = walkRecordRepository.findByUser_IdOrderByCreateDateDesc(user.getId());
        return walkRecodeList.stream()
                .map(this::getWalkUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    private WalkUsageSimpleDTO getWalkUsageDetailSimplified(WalkRecordEntity walkRecordEntity) {
        WalkUsageSimpleDTO walkUsageDTO = new WalkUsageSimpleDTO();
        walkUsageDTO.setWalkRecodeId(walkRecordEntity.getId());
        walkUsageDTO.setUserNickname(walkRecordEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecordEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecordEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecordEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecordEntity.getPet().getPetName());
        walkUsageDTO.setWalkTime(walkRecordEntity.getWalkTime());
        walkUsageDTO.setEndTime(walkRecordEntity.getEndTime());
        walkUsageDTO.setAmount(walkRecordEntity.getAmount());
        return walkUsageDTO;
    }

    public WalkUsageDetailsDTO getUserWalkUsageDetail(String accessToken, int walkRecodeId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        WalkRecordEntity walkRecode = walkRecordRepository.findById(walkRecodeId);
        if (walkRecode == null) {
            throw new IllegalArgumentException("해당 산책기록을 찾을 수 없습니다");
        }
        if (walkRecode.getUser().getId() != user.getId() && walkRecode.getWalker().getId() != user.getId()){
            throw new IllegalArgumentException("해당 산책기록의 작성자가 혹은 산책러가 아닙니다");
        }
        return getWalkUsageDetail(walkRecode);
    }
    private WalkUsageDetailsDTO getWalkUsageDetail(WalkRecordEntity walkRecordEntity) {
        WalkUsageDetailsDTO walkUsageDTO = new WalkUsageDetailsDTO();
        walkUsageDTO.setWalkRecodeId(walkRecordEntity.getId());
        walkUsageDTO.setUserNickname(walkRecordEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecordEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecordEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecordEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecordEntity.getPet().getPetName());
        walkUsageDTO.setPetImage(walkRecordEntity.getPet().getPetImage());
        walkUsageDTO.setPetGender(walkRecordEntity.getPet().getGender());
        walkUsageDTO.setPetSpecies(walkRecordEntity.getPet().getSpecies());
        walkUsageDTO.setPetBirthYear(walkRecordEntity.getPet().getBirthYear());
        walkUsageDTO.setWalkTime(walkRecordEntity.getWalkTime());
        walkUsageDTO.setStartTime(walkRecordEntity.getStartTime());
        walkUsageDTO.setEndTime(walkRecordEntity.getEndTime());
        walkUsageDTO.setLatitude(walkRecordEntity.getLatitude());
        walkUsageDTO.setLongitude(walkRecordEntity.getLongitude());
        walkUsageDTO.setAddress(walkRecordEntity.getAddress());
        walkUsageDTO.setDetailAddress(walkRecordEntity.getDetailAddress());
        walkUsageDTO.setTitle(walkRecordEntity.getTitle());
        walkUsageDTO.setContent(walkRecordEntity.getContent());
        walkUsageDTO.setStatus(walkRecordEntity.getStatus());
        walkUsageDTO.setReason(walkRecordEntity.getReason());
        walkUsageDTO.setAmount(walkRecordEntity.getAmount());
        return walkUsageDTO;
    }
    public List<WalkUsageSimpleDTO> getPartnerWalkUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<WalkRecordEntity> walkRecodeList = walkRecordRepository.findByWalker_IdOrderByCreateDateDesc(user.getId());
        return walkRecodeList.stream()
                .map(this::getWalkUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    public List<CareUsageSimpleDTO> getUserCareUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<CareRecordEntity> careRecodeList = careRecordRepository.findByOwner_IdOrderByEndDateDesc(user.getId());
        return careRecodeList.stream()
                .map(this::getCareUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    private CareUsageSimpleDTO getCareUsageDetailSimplified(CareRecordEntity careRecordEntity) {
        CareUsageSimpleDTO careUsageDTO = new CareUsageSimpleDTO();
        careUsageDTO.setCareRecodeId(careRecordEntity.getId());
        careUsageDTO.setUserNickname(careRecordEntity.getOwner().getNickname());
        careUsageDTO.setUserImage(careRecordEntity.getOwner().getProfileImage());
        careUsageDTO.setCaregiverNickname(careRecordEntity.getCaregiver().getNickname());
        careUsageDTO.setCaregiverImage(careRecordEntity.getCaregiver().getProfileImage());
        careUsageDTO.setPetName(careRecordEntity.getPet().getPetName());
        careUsageDTO.setStartDate(careRecordEntity.getStartDate());
        careUsageDTO.setAmount(careRecordEntity.getAmount());
        return careUsageDTO;
    }
    public List<CareUsageSimpleDTO> getPartnerCareUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<CareRecordEntity> careRecodeList = careRecordRepository.findByCaregiver_IdOrderByEndDateDesc(user.getId());
        return careRecodeList.stream()
                .map(this::getCareUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    public CareUsageDetailsDTO getUserCareUsageDetail(String accessToken, int careRecodeId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        CareRecordEntity careRecode = careRecordRepository.findById(careRecodeId);
        if (careRecode == null) {
            throw new IllegalArgumentException("해당 돌봄기록을 찾을 수 없습니다");
        }
        if (careRecode.getOwner().getId() != user.getId() && careRecode.getCaregiver().getId() != user.getId()){
            throw new IllegalArgumentException("해당 돌봄기록의 작성자가 혹은 돌봄사가 아닙니다");
        }
        CarePostEntity carePost = carePostRepository.findById(careRecode.getCaregiver().getId());
        return getCareUsageDetail(careRecode, carePost);
    }
    private CareUsageDetailsDTO getCareUsageDetail(CareRecordEntity careRecordEntity, CarePostEntity carePostEntity) {
        CareUsageDetailsDTO careUsageDTO = new CareUsageDetailsDTO();
        careUsageDTO.setCareRecodeId(careRecordEntity.getId());
        careUsageDTO.setUserNickname(careRecordEntity.getOwner().getNickname());
        careUsageDTO.setUserImage(careRecordEntity.getOwner().getProfileImage());
        careUsageDTO.setCaregiverNickname(careRecordEntity.getCaregiver().getNickname());
        careUsageDTO.setCaregiverImage(careRecordEntity.getCaregiver().getProfileImage());

        careUsageDTO.setPetName(careRecordEntity.getPet().getPetName());
        careUsageDTO.setPetImage(careRecordEntity.getPet().getPetImage());
        careUsageDTO.setPetGender(careRecordEntity.getPet().getGender());
        careUsageDTO.setPetSpecies(careRecordEntity.getPet().getSpecies());
        careUsageDTO.setPetBirthYear(careRecordEntity.getPet().getBirthYear());
        careUsageDTO.setStartDate(careRecordEntity.getStartDate());
        careUsageDTO.setEndDate(careRecordEntity.getEndDate());

        careUsageDTO.setLatitude(carePostEntity.getLatitude());
        careUsageDTO.setLongitude(carePostEntity.getLongitude());
        careUsageDTO.setAdministrativeAddress1(carePostEntity.getAdministrativeAddress1());
        careUsageDTO.setAdministrativeAddress2(carePostEntity.getAdministrativeAddress2());
        careUsageDTO.setStreetAddress(carePostEntity.getStreetNameAddress());
        careUsageDTO.setDetailAddress(carePostEntity.getDetailAddress());

        careUsageDTO.setTitle(carePostEntity.getTitle());
        careUsageDTO.setContent(carePostEntity.getContent());

        careUsageDTO.setStatus(careRecordEntity.getStatus());
        careUsageDTO.setReason(careRecordEntity.getReason());
        careUsageDTO.setAmount(careRecordEntity.getAmount());
        return careUsageDTO;
    }
}
