package io.cloudtype.Demo.service;

import io.cloudtype.Demo.Dto.Care.CareUsageDetailsDTO;
import io.cloudtype.Demo.Dto.Care.CareUsageSimpleDTO;
import io.cloudtype.Demo.Dto.Walk.WalkUsageDetailsDTO;
import io.cloudtype.Demo.Dto.Walk.WalkUsageSimpleDTO;
import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.CareRecodeEntity;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.entity.Walk.WalkRecodeEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.Care.CarePostRepository;
import io.cloudtype.Demo.repository.Care.CareRecodeRepository;
import io.cloudtype.Demo.repository.UserRepository;
import io.cloudtype.Demo.repository.Walk.WalkRecodeRepository;
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
    private final WalkRecodeRepository walkRecodeRepository;
    private final CareRecodeRepository careRecodeRepository;
    private final CarePostRepository carePostRepository;
    @Autowired
    public UsageDetailsService(UserRepository userRepository, JWTUtil jwtUtil, WalkRecodeRepository walkRecodeRepository,
                               CareRecodeRepository careRecodeRepository, CarePostRepository carePostRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.walkRecodeRepository = walkRecodeRepository;
        this.careRecodeRepository = careRecodeRepository;
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
        List<WalkRecodeEntity> walkRecodeList = walkRecodeRepository.findByUser_IdOrderByCreateDateDesc(user.getId());
        return walkRecodeList.stream()
                .map(this::getWalkUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    private WalkUsageSimpleDTO getWalkUsageDetailSimplified(WalkRecodeEntity walkRecodeEntity) {
        WalkUsageSimpleDTO walkUsageDTO = new WalkUsageSimpleDTO();
        walkUsageDTO.setWalkRecodeId(walkRecodeEntity.getId());
        walkUsageDTO.setUserNickname(walkRecodeEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecodeEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecodeEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecodeEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecodeEntity.getPet().getPetName());
        walkUsageDTO.setWalkTime(walkRecodeEntity.getWalkTime());
        walkUsageDTO.setEndTime(walkRecodeEntity.getEndTime());
        walkUsageDTO.setAmount(walkRecodeEntity.getAmount());
        return walkUsageDTO;
    }

    public WalkUsageDetailsDTO getUserWalkUsageDetail(String accessToken, int walkRecodeId) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        WalkRecodeEntity walkRecode = walkRecodeRepository.findById(walkRecodeId);
        if (walkRecode == null) {
            throw new IllegalArgumentException("해당 산책기록을 찾을 수 없습니다");
        }
        if (walkRecode.getUser().getId() != user.getId() && walkRecode.getWalker().getId() != user.getId()){
            throw new IllegalArgumentException("해당 산책기록의 작성자가 혹은 산책러가 아닙니다");
        }
        return getWalkUsageDetail(walkRecode);
    }
    private WalkUsageDetailsDTO getWalkUsageDetail(WalkRecodeEntity walkRecodeEntity) {
        WalkUsageDetailsDTO walkUsageDTO = new WalkUsageDetailsDTO();
        walkUsageDTO.setWalkRecodeId(walkRecodeEntity.getId());
        walkUsageDTO.setUserNickname(walkRecodeEntity.getUser().getNickname());
        walkUsageDTO.setUserImage(walkRecodeEntity.getUser().getProfileImage());
        walkUsageDTO.setWalkerNickname(walkRecodeEntity.getWalker().getNickname());
        walkUsageDTO.setWalkerImage(walkRecodeEntity.getWalker().getProfileImage());
        walkUsageDTO.setPetName(walkRecodeEntity.getPet().getPetName());
        walkUsageDTO.setPetImage(walkRecodeEntity.getPet().getPetImage());
        walkUsageDTO.setPetGender(walkRecodeEntity.getPet().getGender());
        walkUsageDTO.setPetSpecies(walkRecodeEntity.getPet().getSpecies());
        walkUsageDTO.setPetBirthYear(walkRecodeEntity.getPet().getBirthYear());
        walkUsageDTO.setWalkTime(walkRecodeEntity.getWalkTime());
        walkUsageDTO.setStartTime(walkRecodeEntity.getStartTime());
        walkUsageDTO.setEndTime(walkRecodeEntity.getEndTime());
        walkUsageDTO.setLatitude(walkRecodeEntity.getLatitude());
        walkUsageDTO.setLongitude(walkRecodeEntity.getLongitude());
        walkUsageDTO.setAddress(walkRecodeEntity.getAddress());
        walkUsageDTO.setDetailAddress(walkRecodeEntity.getDetailAddress());
        walkUsageDTO.setTitle(walkRecodeEntity.getTitle());
        walkUsageDTO.setContent(walkRecodeEntity.getContent());
        walkUsageDTO.setStatus(walkRecodeEntity.getStatus());
        walkUsageDTO.setReason(walkRecodeEntity.getReason());
        walkUsageDTO.setAmount(walkRecodeEntity.getAmount());
        return walkUsageDTO;
    }
    public List<WalkUsageSimpleDTO> getPartnerWalkUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<WalkRecodeEntity> walkRecodeList = walkRecodeRepository.findByWalker_IdOrderByCreateDateDesc(user.getId());
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
        List<CareRecodeEntity> careRecodeList = careRecodeRepository.findByOwner_IdOrderByEndDateDesc(user.getId());
        return careRecodeList.stream()
                .map(this::getCareUsageDetailSimplified)
                .collect(Collectors.toList());
    }
    private CareUsageSimpleDTO getCareUsageDetailSimplified(CareRecodeEntity careRecodeEntity) {
        CareUsageSimpleDTO careUsageDTO = new CareUsageSimpleDTO();
        careUsageDTO.setCareRecodeId(careRecodeEntity.getId());
        careUsageDTO.setUserNickname(careRecodeEntity.getOwner().getNickname());
        careUsageDTO.setUserImage(careRecodeEntity.getOwner().getProfileImage());
        careUsageDTO.setCaregiverNickname(careRecodeEntity.getCaregiver().getNickname());
        careUsageDTO.setCaregiverImage(careRecodeEntity.getCaregiver().getProfileImage());
        careUsageDTO.setPetName(careRecodeEntity.getPet().getPetName());
        careUsageDTO.setStartDate(careRecodeEntity.getStartDate());
        careUsageDTO.setAmount(careRecodeEntity.getAmount());
        return careUsageDTO;
    }
    public List<CareUsageSimpleDTO> getPartnerCareUsageList(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        List<CareRecodeEntity> careRecodeList = careRecodeRepository.findByCaregiver_IdOrderByEndDateDesc(user.getId());
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
        CareRecodeEntity careRecode = careRecodeRepository.findById(careRecodeId);
        if (careRecode == null) {
            throw new IllegalArgumentException("해당 돌봄기록을 찾을 수 없습니다");
        }
        if (careRecode.getOwner().getId() != user.getId() && careRecode.getCaregiver().getId() != user.getId()){
            throw new IllegalArgumentException("해당 돌봄기록의 작성자가 혹은 돌봄사가 아닙니다");
        }
        CarePostEntity carePost = carePostRepository.findById(careRecode.getCaregiver().getId());
        return getCareUsageDetail(careRecode, carePost);
    }
    private CareUsageDetailsDTO getCareUsageDetail(CareRecodeEntity careRecodeEntity, CarePostEntity carePostEntity) {
        CareUsageDetailsDTO careUsageDTO = new CareUsageDetailsDTO();
        careUsageDTO.setCareRecodeId(careRecodeEntity.getId());
        careUsageDTO.setUserNickname(careRecodeEntity.getOwner().getNickname());
        careUsageDTO.setUserImage(careRecodeEntity.getOwner().getProfileImage());
        careUsageDTO.setCaregiverNickname(careRecodeEntity.getCaregiver().getNickname());
        careUsageDTO.setCaregiverImage(careRecodeEntity.getCaregiver().getProfileImage());

        careUsageDTO.setPetName(careRecodeEntity.getPet().getPetName());
        careUsageDTO.setPetImage(careRecodeEntity.getPet().getPetImage());
        careUsageDTO.setPetGender(careRecodeEntity.getPet().getGender());
        careUsageDTO.setPetSpecies(careRecodeEntity.getPet().getSpecies());
        careUsageDTO.setPetBirthYear(careRecodeEntity.getPet().getBirthYear());
        careUsageDTO.setStartDate(careRecodeEntity.getStartDate());
        careUsageDTO.setEndDate(careRecodeEntity.getEndDate());

        careUsageDTO.setLatitude(carePostEntity.getLatitude());
        careUsageDTO.setLongitude(carePostEntity.getLongitude());
        careUsageDTO.setAdministrativeAddress1(carePostEntity.getAdministrativeAddress1());
        careUsageDTO.setAdministrativeAddress2(carePostEntity.getAdministrativeAddress2());
        careUsageDTO.setStreetAddress(carePostEntity.getStreetNameAddress());
        careUsageDTO.setDetailAddress(carePostEntity.getDetailAddress());

        careUsageDTO.setTitle(carePostEntity.getTitle());
        careUsageDTO.setContent(carePostEntity.getContent());

        careUsageDTO.setStatus(careRecodeEntity.getStatus());
        careUsageDTO.setReason(careRecodeEntity.getReason());
        careUsageDTO.setAmount(careRecodeEntity.getAmount());
        return careUsageDTO;
    }
}
