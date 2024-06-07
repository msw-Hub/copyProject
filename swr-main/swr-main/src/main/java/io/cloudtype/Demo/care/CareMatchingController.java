package io.cloudtype.Demo.care;

import io.cloudtype.Demo.care.DTO.CareApplyDTO;
import io.cloudtype.Demo.care.DTO.CareApplyDetailDTO;
import io.cloudtype.Demo.care.DTO.CarePostDetailsDTO;
import io.cloudtype.Demo.care.DTO.CarePostListDTO;
import io.cloudtype.Demo.community.CommunityBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/care")
public class CareMatchingController {
    private final CareMatchingService careMatchingService;
    private final CommunityBoardService communityBoardService;
    @Autowired
    public CareMatchingController(CareMatchingService careMatchingService,
                                  CommunityBoardService communityBoardService){
        this.communityBoardService = communityBoardService;
        this.careMatchingService = careMatchingService;
    }
    //  /mypage/pet/list에서 펫정보를 받아온 이후 고른 펫을 받음
    @PostMapping("/create/post")
    public ResponseEntity<?> createPost(@RequestHeader("Authorization") String accessToken,
                                        @ModelAttribute("title") String title,
                                        @ModelAttribute("content") String content,
                                        @ModelAttribute("administrativeAddress1") String administrativeAddress1,
                                        @ModelAttribute("administrativeAddress2") String administrativeAddress2,
                                        @ModelAttribute("streetNameAddress") String streetNameAddress,
                                        @ModelAttribute("detailAddress") String detailAddress,
                                        @ModelAttribute("latitude") double latitude,
                                        @ModelAttribute("longitude") double longitude,
                                        @RequestPart(value = "images", required = true) List<MultipartFile> images,
                                        @RequestPart(value = "unavailableDates", required = false) List<String> unavailableDates
    ) {
        try {
            int carePostId = careMatchingService.createPost(accessToken, title, content,
                    administrativeAddress1, administrativeAddress2, streetNameAddress, detailAddress, latitude, longitude);

            if (images == null || images.isEmpty()) {
                throw new IllegalArgumentException("돌봄 장소 이미지를 필수적으로 등록해주세요");
            }

            // 비동기 이미지 업로드 호출
            List<CompletableFuture<String>> futures = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(communityBoardService::uploadImageAsync)
                    .collect(Collectors.toList());

            // 이미지 업로드가 완료될 때까지 대기하고 결과 받기
            List<String> imageUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 이미지 URL을 CareImgEntity에 저장
            for (String imageUrl : imageUrls) {
                careMatchingService.addImage(carePostId, imageUrl, accessToken);
            }

            if (unavailableDates != null) {
                careMatchingService.addUnavailableDates(carePostId, unavailableDates);
            }

            return ResponseEntity.ok("돌봄글 작성 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/apply")
    public ResponseEntity<?> applyCare(@RequestHeader("Authorization") String accessToken,
                                       @RequestBody CareApplyDTO careApplyDTO
    ) {
        try {
            careMatchingService.applyCare(accessToken, careApplyDTO);
            return ResponseEntity.ok("돌봄 예약 신청");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //수정은 글제목, 내용, 돌봄이미지, 이용불가날짜만 가능 - 예약 신청이 없는 경우에만 수정 가능
    @PostMapping("/edit/post")
    public ResponseEntity<?> editPost(@RequestHeader("Authorization") String accessToken,
                                      @ModelAttribute("carePostId") int carePostId,
                                      @ModelAttribute("title") String title,
                                      @ModelAttribute("content") String content,
                                      @ModelAttribute("imageChangeCheck") boolean imageChangeCheck,
                                      @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                      @ModelAttribute("datesChangeCheck") boolean datesChangeCheck,
                                      @RequestPart(value = "unavailableDates", required = false) List<String> unavailableDates
    ) {
        try {
            // 예약 요청이 있는지를 확인해서 없으면 수정
            if (careMatchingService.checkReservation(carePostId)) {
                throw new IllegalStateException("예약신청이 있는 글은 수정할 수 없습니다 예약신청취소 후 다시 진행해주세요");
            }

            careMatchingService.editPost(accessToken, carePostId, title, content);

            if (imageChangeCheck) { // 기존 이미지 삭제 후 재업로드
                careMatchingService.deleteImages(carePostId, accessToken);

                if (images != null && !images.isEmpty()) {
                    // 비동기 이미지 업로드 호출
                    List<CompletableFuture<String>> futures = images.stream()
                            .filter(image -> image != null && !image.isEmpty())
                            .map(communityBoardService::uploadImageAsync)
                            .collect(Collectors.toList());

                    // 이미지 업로드가 완료될 때까지 대기하고 결과 받기
                    List<String> imageUrls = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    // 이미지 URL을 CareImgEntity에 저장
                    for (String imageUrl : imageUrls) {
                        careMatchingService.addImage(carePostId, imageUrl, accessToken);
                    }
                } else {
                    throw new IllegalArgumentException("이미지가 없어 이미지를 변경할 수 없습니다");
                }
            }
            if (datesChangeCheck) { // 기존 날짜와 비교하여 변경된 부분만 업데이트
                careMatchingService.updateUnavailableDates(carePostId, unavailableDates);
            }
            return ResponseEntity.ok("돌봄글 수정 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/delete/post")
    public ResponseEntity<?> deletePost(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("carePostId") int carePostId
    ) {
        try {
            careMatchingService.deletePost(accessToken, carePostId);
            return ResponseEntity.ok("돌봄글 삭제 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestHeader("Authorization") String accessToken,
                                  @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int page = (int) requestBody.get("page");
            String administrativeAddress1 = (String) requestBody.get("administrativeAddress1"); // 시/도
            String administrativeAddress2 = (String) requestBody.get("administrativeAddress2"); // 시/군/구
            double homeLatitude = ((Number) requestBody.get("homeLatitude")).doubleValue();
            double homeLongitude = ((Number) requestBody.get("homeLongitude")).doubleValue();

            //주소로 필터링해서 거리가 가까운순으로 정렬
            Page<CarePostListDTO> pageResult = careMatchingService.list(accessToken, page-1, administrativeAddress1,
                    administrativeAddress2, homeLatitude, homeLongitude);
            Map<String, Object> response = new HashMap<>();
            response.put("posts", pageResult);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/detail")
    public ResponseEntity<?> detail(@RequestHeader("Authorization") String accessToken,
                                    @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int carePostId = (int) requestBody.get("carePostId");
            double homeLatitude = ((Number) requestBody.get("homeLatitude")).doubleValue();
            double homeLongitude = ((Number) requestBody.get("homeLongitude")).doubleValue();

            CarePostDetailsDTO carePost = careMatchingService.detail(accessToken, carePostId, homeLatitude, homeLongitude);
            return ResponseEntity.ok(carePost);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //내 돌봄글 보기
    @GetMapping("/myPost")
    public ResponseEntity<?> myPost(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            CarePostDetailsDTO myPost = careMatchingService.myPost(accessToken);
            return ResponseEntity.ok(myPost);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //내 돌봄글의 신청자 목록
    @GetMapping("/myPost/applier")
    public ResponseEntity<?> myPostApplier(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> applierList = careMatchingService.getApplierList(accessToken);
            return ResponseEntity.ok(applierList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //내 돌봄글의 예약확정자 목록
    @GetMapping("/myPost/reservation")
    public ResponseEntity<?> myPostReservation(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> reservationList = careMatchingService.getReservationList(accessToken);
            return ResponseEntity.ok(reservationList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //나의 예약 대기 목록
    @GetMapping("/myReservation/apply")
    public ResponseEntity<?> myReservationApply(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> applyList = careMatchingService.myApplyList(accessToken);
            return ResponseEntity.ok(applyList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //careMatchingId(예약번호)로 해당 돌봄글 보기
    @PostMapping("/myReservation/post")
    public ResponseEntity<?> reservationPost(@RequestHeader("Authorization") String accessToken,
                                            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int careMatchingId = (int) requestBody.get("careMatchingId");
            double homeLatitude = ((Number) requestBody.get("homeLatitude")).doubleValue();
            double homeLongitude = ((Number) requestBody.get("homeLongitude")).doubleValue();
            CarePostDetailsDTO carePost = careMatchingService.reservationPost(accessToken, careMatchingId, homeLatitude,homeLongitude);
            return ResponseEntity.ok(carePost);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //나의 예약 확정 목록
    @GetMapping("/myReservation/confirmed")
    public ResponseEntity<?> myReservationConfirmed(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> confirmedList = careMatchingService.myConfirmedList(accessToken);
            return ResponseEntity.ok(confirmedList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //현재 진행중인 돌봄 목록 - 일반유저
    @GetMapping("/myReservation/progress")
    public ResponseEntity<?> myReservationProgress(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> progressList = careMatchingService.myProgressList(accessToken);
            return ResponseEntity.ok(progressList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //현재 진행중인 돌봄 목록 - 파트너
    @GetMapping("/myPost/progress")
    public ResponseEntity<?> myPostProgress(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<CareApplyDetailDTO> progressList = careMatchingService.myPostProgressList(accessToken);
            return ResponseEntity.ok(progressList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 예약 수락(파트너만) 예약확정 ->예약수도 증가해야함
    @GetMapping("/accept")
    public ResponseEntity<?> acceptCare(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            careMatchingService.acceptCare(accessToken, careMatchingId);
            return ResponseEntity.ok("돌봄 예약 수락 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 예약신청 거절(파트너만)
    @GetMapping("/reject")
    public ResponseEntity<?> rejectCare(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            careMatchingService.rejectCare(accessToken, careMatchingId);
            return ResponseEntity.ok("돌봄 예약 거절 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 예약신청 취소(일반유저)
    @GetMapping("/cancel/apply")
    public ResponseEntity<?> cancelApply(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            careMatchingService.cancelApply(accessToken, careMatchingId);
            return ResponseEntity.ok("돌봄 예약신청 취소 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 예약확정 취소(일반유저)
    @GetMapping("/cancel/reservation")
    public ResponseEntity<?> cancelReservation(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            careMatchingService.cancelReservation(accessToken, careMatchingId);
            return ResponseEntity.ok("돌봄 예약확정 취소 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 예약신청 수정은 없음
    //돌봄 시작
    @GetMapping("/start")
    public ResponseEntity<?> startCare(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            careMatchingService.startCare(accessToken, careMatchingId);
            return ResponseEntity.ok("돌봄 시작 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 종료 (정상)
    @GetMapping("/complete")
    public ResponseEntity<?> completeCare(@RequestHeader("Authorization") String accessToken,
                                        @RequestParam("careMatchingId") int careMatchingId
    ) {
        try {
            int careRecordId = careMatchingService.completeCare(accessToken, careMatchingId);
            Map<String, Object> response = new HashMap<>();
            response.put("care_record_id", careRecordId);
            response.put("message", "돌봄 이용종료, care_record_id를 이용하여 후기작성을 진행해주세요");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //돌봄 종료 (비정상)
    @PostMapping("/incomplete")
    public ResponseEntity<?> incompleteCare(@RequestHeader("Authorization") String accessToken,
                                        @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int careMatchingId = (int) requestBody.get("careMatchingId");
            String reason = (String) requestBody.get("reason");
            careMatchingService.incompleteCare(accessToken, careMatchingId, reason);
            return ResponseEntity.ok("돌봄 서비스 중 문제발생으로 인해 비정상종료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
