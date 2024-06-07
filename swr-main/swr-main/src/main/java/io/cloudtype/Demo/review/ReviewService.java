package io.cloudtype.Demo.review;


import io.cloudtype.Demo.care.DTO.CareReviewDTO;
import io.cloudtype.Demo.care.entity.CareRecordEntity;
import io.cloudtype.Demo.care.entity.CareReviewEntity;
import io.cloudtype.Demo.care.repository.CarePostRepository;
import io.cloudtype.Demo.care.repository.CareRecordRepository;
import io.cloudtype.Demo.care.repository.CareReviewRepository;
import io.cloudtype.Demo.community.CommunityBoardService;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.cloudtype.Demo.walk.DTO.WalkReviewDTO;
import io.cloudtype.Demo.walk.entity.WalkRecordEntity;
import io.cloudtype.Demo.walk.entity.WalkReviewEntity;
import io.cloudtype.Demo.walk.repository.WalkRecordRepository;
import io.cloudtype.Demo.walk.repository.WalkReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReviewService {
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final WalkRecordRepository walkRecordRepository;
    private final WalkReviewRepository walkReviewRepository;
    private final CommunityBoardService communityBoardService;
    private final CareRecordRepository careRecordRepository;
    private final CareReviewRepository careReviewRepository;
    private final CarePostRepository carePostRepository;
    @Autowired
    public ReviewService(UserRepository userRepository, JWTUtil jwtUtil,
                         WalkRecordRepository walkRecordRepository, WalkReviewRepository walkReviewRepository,
                         CommunityBoardService communityBoardService,
                         CareRecordRepository careRecordRepository,
                         CareReviewRepository careReviewRepository,
                         CarePostRepository carePostRepository
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.walkRecordRepository = walkRecordRepository;
        this.walkReviewRepository = walkReviewRepository;
        this.communityBoardService = communityBoardService;
        this.careRecordRepository = careRecordRepository;
        this.careReviewRepository = careReviewRepository;
        this.carePostRepository = carePostRepository;
    }
    @Transactional
    public void createReview(String accessToken, int walkRecordId, String content, double rating, String imgUrl) {
        UserEntity user = getUserEntity(accessToken);
        WalkRecordEntity walkRecord = walkRecordRepository.findById(walkRecordId);
        if (walkRecord == null) {
            throw new IllegalArgumentException("해당 산책기록을 찾을 수 없습니다");
        }
        if (walkRecord.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 산책기록의 작성자가 아닙니다");
        }
        if(walkRecord.getStatus()!=3){
            throw new IllegalArgumentException("완료된 산책건에 대해서만 가능합니다");
        }
        if (walkRecord.isReview()) {
            throw new IllegalArgumentException("이미 리뷰를 작성하였습니다");
        }
        if(rating<0 || rating>5) {
            throw new IllegalArgumentException("평점은 0~5 사이의 값이어야 합니다");
        }
        //산책종료 24시간 내에 가능
        if(walkRecord.getEndTime().plusDays(1).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("산책종료 후 24시간 이내에만 작성 가능합니다");
        }
        //리뷰 작성여부 수정
        walkRecord.setReview(true);
        walkRecordRepository.save(walkRecord);
        //리뷰 작성
        WalkReviewEntity review = new WalkReviewEntity();
        review.setWalkRecord(walkRecord);
        review.setRating(rating);
        review.setContent(content);
        review.setCreateDate(LocalDateTime.now());
        review.setImgUrl(imgUrl);
        walkReviewRepository.save(review);
        //파트너의 산책 평점 및 리뷰수 업데이트
        UserEntity walker = walkRecord.getWalker();
        int reviewCount = walker.getWalkReviewCount();
        double walkRating = walker.getWalkRating();
        int newReviewCount = reviewCount + 1;
        double newRating = (walkRating * reviewCount + rating) / newReviewCount;
        walker.setWalkRating(newRating);
        walker.setWalkReviewCount(newReviewCount);
        userRepository.save(walker);
    }
    public List<WalkReviewDTO> getUserWalkReviewList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        List<WalkReviewEntity> walkReviewList = walkReviewRepository.findByWalkRecord_User_IdOrderByCreateDateDesc(user.getId());
        return walkReviewList.stream()
                .map(this::mapToWalkReviewDTO)
                .collect(Collectors.toList());
    }
    private WalkReviewDTO mapToWalkReviewDTO(WalkReviewEntity walkReview) {
        WalkReviewDTO walkReviewDTO = new WalkReviewDTO();
        walkReviewDTO.setId(walkReview.getId());
        walkReviewDTO.setWalkRecodeId(walkReview.getWalkRecord().getId());
        walkReviewDTO.setUserNickname(walkReview.getWalkRecord().getUser().getNickname());
        walkReviewDTO.setWalkerNickname(walkReview.getWalkRecord().getWalker().getNickname());
        walkReviewDTO.setWalkTime(walkReview.getWalkRecord().getWalkTime());
        walkReviewDTO.setRating(walkReview.getRating());
        walkReviewDTO.setContent(walkReview.getContent());
        walkReviewDTO.setReviewDate(walkReview.getCreateDate());
        walkReviewDTO.setImgUrl(walkReview.getImgUrl());
        return walkReviewDTO;
    }
    public List<WalkReviewDTO> getPartnerWalkReviewList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        List<WalkReviewEntity> walkReviewList = walkReviewRepository.findByWalkRecord_Walker_IdOrderByCreateDateDesc(user.getId());
        return walkReviewList.stream()
                .map(this::mapToWalkReviewDTO)
                .collect(Collectors.toList());
    }
    public List<WalkReviewDTO> getWalkReviewList(int partnerId,String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        List<WalkReviewEntity> walkReviewList = walkReviewRepository.findByWalkRecord_Walker_IdOrderByCreateDateDesc(partnerId);
        return walkReviewList.stream()
                .map(this::mapToWalkReviewDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public void deleteWalkReview(String accessToken, int walkReviewId) {
        UserEntity user = getUserEntity(accessToken);
        WalkReviewEntity walkReview = walkReviewRepository.findById(walkReviewId);
        if (walkReview == null) {
            throw new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다");
        }
        if (walkReview.getWalkRecord().getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 리뷰의 작성자가 아닙니다");
        }
        String imgUrl = walkReview.getImgUrl();
        if (imgUrl != null) {
            communityBoardService.deleteImageGcs(imgUrl);
        }
        //파트너의 산책 평점 및 리뷰수 업데이트
        UserEntity walker = walkReview.getWalkRecord().getWalker();
        int reviewCount = walker.getWalkReviewCount();
        double walkRating = walker.getWalkRating();
        int newReviewCount = reviewCount - 1;
        if (newReviewCount > 0) {
            double newRating = (walkRating * reviewCount - walkReview.getRating()) / newReviewCount;
            walker.setWalkRating(newRating);
        } else {
            // 리뷰가 더 이상 없을 때 평점을 초기화하거나 기본 값으로 설정
            walker.setWalkRating(0);  // 예를 들어 0 또는 다른 기본값으로 설정할 수 있습니다.
        }
        walker.setWalkReviewCount(newReviewCount);
        userRepository.save(walker);
        //리뷰 삭제
        walkReviewRepository.delete(walkReview);
    }
    @Transactional
    public void createCareReview(String accessToken, int careRecordId, String content, double rating, String imgUrl) {
        UserEntity user = getUserEntity(accessToken);
        CareRecordEntity careRecord = careRecordRepository.findById(careRecordId);
        if (careRecord == null) {
            throw new IllegalArgumentException("해당 돌봄기록을 찾을 수 없습니다");
        }
        if (careRecord.getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 돌봄기록의 작성자가 아닙니다");
        }
        if (careRecord.isReview()) {
            throw new IllegalArgumentException("이미 리뷰를 작성하였습니다");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("평점은 0~5 사이의 값이어야 합니다");
        }
        //돌봄종료 24시간 내에 가능
        if (careRecord.getEndDate().plusDays(1).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("돌봄종료 후 24시간 이내에만 작성 가능합니다");
        }
        //리뷰 작성여부 수정
        careRecord.setReview(true);
        careRecordRepository.save(careRecord);
        //리뷰 작성
        CareReviewEntity review = new CareReviewEntity();
        review.setCareRecord(careRecord);
        review.setRating(rating);
        review.setContent(content);
        review.setCreateDate(LocalDateTime.now());
        review.setImgUrl(imgUrl);
        careReviewRepository.save(review);

        //파트너의 돌봄 평점 및 리뷰수 업데이트
        UserEntity caregiver = careRecord.getCaregiver();
        int reviewCount = caregiver.getCareReviewCount();
        double careRating = caregiver.getCareRating();
        int newReviewCount = reviewCount + 1;
        double newRating = (careRating * reviewCount + rating) / newReviewCount;
        caregiver.setCareRating(newRating);
        caregiver.setCareReviewCount(newReviewCount);
        userRepository.save(caregiver);
    }
    public List<CareReviewDTO> getUserCareReviewList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        //care_recode_id로 접근해서
        List<CareReviewEntity> careReviewList = careReviewRepository.findByCareRecord_Owner_IdOrderByCreateDateDesc(user.getId());
        return careReviewList.stream()
                .map(this::mapToCareReviewDTO)
                .collect(Collectors.toList());
    }
    private CareReviewDTO mapToCareReviewDTO(CareReviewEntity careReview) {
        CareReviewDTO careReviewDTO = new CareReviewDTO();
        careReviewDTO.setId(careReview.getId());
        careReviewDTO.setCareRecodeId(careReview.getCareRecord().getId());
        careReviewDTO.setUserNickname(careReview.getCareRecord().getOwner().getNickname());
        careReviewDTO.setCaregiverNickname(careReview.getCareRecord().getCaregiver().getNickname());
        careReviewDTO.setContent(careReview.getContent());
        careReviewDTO.setRating(careReview.getRating());
        careReviewDTO.setReviewDate(careReview.getCreateDate());
        careReviewDTO.setPetSpecies(careReview.getCareRecord().getPet().getSpecies());
        careReviewDTO.setImgUrl(careReview.getImgUrl());
        return careReviewDTO;
    }
    @Transactional
    public void deleteCareReview(String accessToken, int careReviewId) {
        UserEntity user = getUserEntity(accessToken);
        CareReviewEntity careReview = careReviewRepository.findById(careReviewId);
        if (careReview == null) {
            throw new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다");
        }
        if (careReview.getCareRecord().getOwner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 리뷰의 작성자가 아닙니다");
        }
        String imgUrl = careReview.getImgUrl();
        if (imgUrl != null) {
            communityBoardService.deleteImageGcs(imgUrl);
        }
        //파트너의 돌봄 평점 및 리뷰수 업데이트
        UserEntity caregiver = careReview.getCareRecord().getCaregiver();
        int reviewCount = caregiver.getCareReviewCount();
        double careRating = caregiver.getCareRating();
        int newReviewCount = reviewCount - 1;
        if (newReviewCount > 0) {
            double newRating = (careRating * reviewCount - careReview.getRating()) / newReviewCount;
            caregiver.setCareRating(newRating);
        } else {
            // 리뷰가 더 이상 없을 때 평점을 초기화하거나 기본 값으로 설정
            caregiver.setCareRating(0);  // 예를 들어 0 또는 다른 기본값으로 설정할 수 있습니다.
        }
        caregiver.setCareReviewCount(newReviewCount);
        userRepository.save(caregiver);
        //리뷰 삭제
        careReviewRepository.delete(careReview);
    }
    public List<CareReviewDTO> getPartnerCareReviewList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);
        List<CareReviewEntity> careReviewList = careReviewRepository.findByCareRecord_Caregiver_IdOrderByCreateDateDesc(user.getId());
        return careReviewList.stream()
                .map(this::mapToCareReviewDTO)
                .collect(Collectors.toList());
    }
    public List<CareReviewDTO> getCareReviewList(int carePostId,String accessToken) {
        getUserEntity(accessToken);
        int caregiverId = carePostRepository.findById(carePostId).getCaregiver().getId();
        List<CareReviewEntity> careReviewList = careReviewRepository.findByCareRecord_Caregiver_IdOrderByCreateDateDesc(caregiverId);
        return careReviewList.stream()
                .map(this::mapToCareReviewDTO)
                .collect(Collectors.toList());
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
