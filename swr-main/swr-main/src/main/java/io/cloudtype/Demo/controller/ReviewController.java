package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.Care.CareReviewDTO;
import io.cloudtype.Demo.Dto.Walk.WalkReviewDTO;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/review")
public class ReviewController {
    private final CommunityBoardService communityBoardService;
    private final ReviewService reviewService;
    @Autowired
    public ReviewController(CommunityBoardService communityBoardService,
                            ReviewService reviewService
    ) {
        this.communityBoardService = communityBoardService;
        this.reviewService = reviewService;
    }
    //유저의 산책리뷰쓰기
    @PostMapping("/create/walk")
    public ResponseEntity<?> createWalkReview(@RequestHeader("Authorization") String accessToken,
                                          @ModelAttribute("walk_recode_id") int walkRecodeId,
                                          @ModelAttribute("content") String content,
                                          @ModelAttribute("rating") double rating,
                                          @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 후기 작성
            reviewService.createReview(accessToken, walkRecodeId, content, rating, imageUrl);
            return ResponseEntity.ok("후기 작성 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저 본인이 쓴 산책리뷰내역
    @GetMapping("/walk/myWrite")
    public ResponseEntity<?> getUserWalkReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkReviewDTO> walkReviewList = reviewService.getUserWalkReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkReviewList", walkReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 특정 산책 리뷰 삭제
    @GetMapping("/delete/walk")
    public ResponseEntity<?> deleteWalkReview(@RequestHeader("Authorization") String accessToken,
                                             @RequestParam("walk_review_id") int walkReviewId
    ) {
        try {
            reviewService.deleteWalkReview(accessToken, walkReviewId);
            return ResponseEntity.ok("후기 삭제 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너에 해당하는 산책 리뷰내역 보기 - 받은 리뷰
    @GetMapping("/walk/partner/list")
    public ResponseEntity<?> getPartnerWalkReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkReviewDTO> walkReviewList = reviewService.getPartnerWalkReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkReviewList", walkReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //다른사람이 해당 파트너의 유저id로 산책 리뷰보기
    @GetMapping("/walk/list")
    public ResponseEntity<?> getWalkReviewList(@RequestHeader("Authorization") String accessToken,
                                             @RequestParam("partner_id") int partnerId
    ) {
        try {
            List<WalkReviewDTO> walkReviewList = reviewService.getWalkReviewList(partnerId, accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkReviewList", walkReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 리뷰 작성
    @PostMapping("/create/care")
    public ResponseEntity<?> createCareReview(@RequestHeader("Authorization") String accessToken,
                                             @ModelAttribute("care_recode_id") int careRecodeId,
                                             @ModelAttribute("content") String content,
                                             @ModelAttribute("rating") double rating,
                                             @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 후기 작성
            reviewService.createCareReview(accessToken, careRecodeId, content, rating, imageUrl);
            return ResponseEntity.ok("후기 작성 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //본인이 쓴 돌봄 리뷰 내역 보기
    @GetMapping("/care/myWrite")
    public ResponseEntity<?> getUserCareReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<CareReviewDTO> careReviewList = reviewService.getUserCareReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("careReviewList", careReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //본인이 쓴 돌봄 리뷰 삭제
    @GetMapping("/delete/care")
    public ResponseEntity<?> deleteCareReview(@RequestHeader("Authorization") String accessToken,
                                             @RequestParam("care_review_id") int careReviewId
    ) {
        try {
            reviewService.deleteCareReview(accessToken, careReviewId);
            return ResponseEntity.ok("후기 삭제 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너 본인이 받은 돌봄 리뷰 내역 보기
    @GetMapping("/care/partner/list")
    public ResponseEntity<?> getPartnerCareReviewList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<CareReviewDTO> careReviewList = reviewService.getPartnerCareReviewList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("careReviewList", careReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //다른사람이 해당 돌봄글의id로 파트너의 id를 찾아서 돌봄 리뷰 내역 보기
    @GetMapping("/care/list")
    public ResponseEntity<?> getCareReviewList(@RequestHeader("Authorization") String accessToken,
                                             @RequestParam("carePostId") int carePostId
    ) {
        try {
            List<CareReviewDTO> careReviewList = reviewService.getCareReviewList(carePostId, accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("careReviewList", careReviewList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
