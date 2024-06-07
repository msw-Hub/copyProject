package io.cloudtype.Demo.mypage;

import io.cloudtype.Demo.care.CareMatchingService;
import io.cloudtype.Demo.community.CommunityBoardService;
import io.cloudtype.Demo.login.JoinDetailsDTO;
import io.cloudtype.Demo.mypage.pet.PetDTO;
import io.cloudtype.Demo.walk.WalkMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final CommunityBoardService communityBoardService;
    private final WalkMatchingService walkMatchingService;
    private final CareMatchingService careMatchingService;

    @Autowired
    public MyPageController(MyPageService myPageService, CommunityBoardService communityBoardService,
                            WalkMatchingService walkMatchingService, CareMatchingService careMatchingService
    ) {
        this.myPageService = myPageService;
        this.communityBoardService = communityBoardService;
        this.walkMatchingService = walkMatchingService;
        this.careMatchingService = careMatchingService;
    }
    @PostMapping("")
    public ResponseEntity<?> myPage(@RequestHeader("Authorization") String accessToken,
                                    @RequestBody @NotNull Map<String, String> requestBody
    ) {
        try {
            // 프론트로부터 받은 핀 번호
            int pinNumber = Integer.parseInt(requestBody.get("pinNumber"));
            // 핀 번호 검증
            boolean result = myPageService.checkPinNumber(accessToken, pinNumber);
            if (!result) {
                return ResponseEntity.badRequest().body("핀번호가 일치하지 않습니다");
            }
            // 유저 정보 반환
            return ResponseEntity.ok(myPageService.getUserInfoByUsername(accessToken));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("핀번호는 숫자로 입력해주세요");
        }
    }
    @PostMapping("/edit/info")
    public ResponseEntity<String> editInfo(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody JoinDetailsDTO joinDetailsDTO
    ) {
        try {
            // 유저 정보 수정
            myPageService.editInfo(accessToken, joinDetailsDTO.getNickname(),
                    joinDetailsDTO.getPhoneNumber(), joinDetailsDTO.getPinNumber());
            return ResponseEntity.ok("정보 수정 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/edit/password")
    public ResponseEntity<String> editPassword(@RequestHeader("Authorization") String accessToken,
                                               @RequestBody Map<String, String> requestBody
    ) {
        try{
            //현재 비밀번호가 일치하는지 확인
            if(!myPageService.checkPassword(accessToken, requestBody.get("currentPassword"))){
                return ResponseEntity.badRequest().body("현재 비밀번호가 일치하지 않습니다.");
            }
            //바꿀 비밀번호로 비밀번호 변경
            myPageService.editPassword(accessToken, requestBody.get("changePassword"));
            return ResponseEntity.ok("비밀번호 수정 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/add/step1")
    public ResponseEntity<?> petAddStep1(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @ModelAttribute("gender") String gender,
            @RequestPart("pet_profile_image") MultipartFile petProfileImage
    ) {
        try {
            CompletableFuture<String> imageUrlFuture = null;
            if (petProfileImage != null && !petProfileImage.isEmpty()) {
                // 비동기 이미지 업로드 서비스 호출
                imageUrlFuture = communityBoardService.uploadImageAsync(petProfileImage);
            }

            String imageUrl = null;
            if (imageUrlFuture != null) {
                imageUrl = imageUrlFuture.join(); // 이미지 업로드가 완료될 때까지 대기하고 결과 받기
            }

            // 유저의 반려동물 정보 추가
            int petId = myPageService.addPetStep1(accessToken, petName, petAge, species, gender, imageUrl);
            // petId를 반환
            Map<String, Object> response = new HashMap<>();
            response.put("petId", petId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/edit/step1")
    public ResponseEntity<String> petEditStep1(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_id") int petId,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @ModelAttribute("gender") String gender,
            @ModelAttribute("image_change_check") Boolean imageChangeCheck,
            @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            // 주인인지 체크
            myPageService.checkOwner(accessToken, petId);
            String imageUrl = myPageService.findImageUrlById(petId);

            CompletableFuture<String> imageUrlFuture = null;

            if (imageChangeCheck && image == null) {
                throw new IllegalArgumentException("이미지 변경 시 반드시 이미지를 첨부해야 합니다.");
            } else if (imageChangeCheck) { // 이미지 삭제 후 새로운 이미지 등록 후 URL 받아오기
                if (imageUrl != null) {
                    communityBoardService.deleteImageGcs(imageUrl);
                }
                imageUrlFuture = communityBoardService.uploadImageAsync(image);
            }

            if (imageUrlFuture != null) {
                imageUrl = imageUrlFuture.join(); // 이미지 업로드가 완료될 때까지 대기하고 결과 받기
            }

            // 반려동물 정보 수정
            myPageService.editPetStep1(petId, petName, petAge, species,gender, imageUrl);
            return ResponseEntity.ok("반려동물 기본정보 수정 완료, 추가 정보 입력으로 이동");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/pet/edit/step2")
    public ResponseEntity<String> petEditStep2(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody PetDTO petDTO
    ) {
        try {
            //주인인지 체크
            myPageService.checkOwner(accessToken, petDTO.getId());

            myPageService.addPetStep2(accessToken, petDTO);
            return ResponseEntity.ok("반려동물 추가정보 수정 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/pet/list")
    public ResponseEntity<Map<String, Object>> petList(@RequestHeader("Authorization") String accessToken
    ) {
        try{
            List<PetDTO> petList = myPageService.getPetList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("petList", petList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage()); // 오류 메시지를 응답에 포함
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @GetMapping("/pet")
    public ResponseEntity<Map<String, Object>> getComment(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam("pet_id") int petId
    ) {
        try {
            PetDTO pet = myPageService.getPet(accessToken,petId);
            Map<String, Object> response = new HashMap<>();
            response.put("pet_info", pet);
            return ResponseEntity.ok(response);
        } catch (Exception e) { // 일반적인 예외 처리를 위해 Exception을 사용
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage()); // 예외 메시지를 오류 응답에 포함
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @PostMapping("/pet/remove")
    public ResponseEntity<String> petRemove(@RequestHeader("Authorization") String accessToken,
                                            @RequestParam("pet_id") int petId
    ) {
        try {
            // 반려동물 삭제
            myPageService.removePet(accessToken, petId);
            return ResponseEntity.ok("반려동물 삭제 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //마이페이지 접근시, 남은 코인 보여주기
    @GetMapping("/coin")
    public ResponseEntity<?> getCoin(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            int coin = myPageService.getCoin(accessToken);
            return ResponseEntity.ok(coin);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            Map<String, Object> status = new HashMap<>();
            //유저로서 산책 상태값이 0인 것이 있는가 - 0,1
            int userWalkStatus0 = walkMatchingService.getUserWalkStatus(accessToken,0);
            //유저로서 산책 상태값이 1인 것이 있는가 - 0,1
            int userWalkStatus1 = walkMatchingService.getUserWalkStatus(accessToken,1);
            //유저로서 산책 상태값이 2인 것이 있는가 - 0,1
            int userWalkStatus2 = walkMatchingService.getUserWalkStatus(accessToken,2);

            if(userWalkStatus2 == 1) status.put("userWalk", "유저로서 산책을 진행중");
            else if(userWalkStatus1 == 1) status.put("userWalk", "유저로서 산책매칭이 완료");
            else if(userWalkStatus0 == 1) status.put("userWalk", "유저로서 산책매칭을 신청중");
            else status.put("userWalk", "유저로서 산책매칭을 신청한 적이 없습니다");

            //유저로서 돌봄 상채값이 0인 것이 몇개 있는가
            int userCareStatus0 = careMatchingService.getUserCareStatus(accessToken,0);
            status.put("유저로서돌봄예약신청수", userCareStatus0);
            //유저로서 돌봄 상태값이 1인 것이 몇개 있는가
            int userCareStatus1 = careMatchingService.getUserCareStatus(accessToken,1);
            status.put("유저로서돌봄예약확정수", userCareStatus1);
            //유저로서 돌봄 상태값이 2인 것이 몇개 있는가
            int userCareStatus2 = careMatchingService.getUserCareStatus(accessToken,2);
            status.put("유저로서돌봄진행중수", userCareStatus2);

            //파트너로서 산책 상태값이 1인 것이 있는가
            int walkerWalkStatus1 = walkMatchingService.getWalkerStatus(accessToken,1);
            //파트너로서 산책 상태값이 2인 것이 있는가
            int walkerWalkStatus2 = walkMatchingService.getWalkerStatus(accessToken,2);
            if(walkerWalkStatus1 == 1) status.put("partnerWalk", "매칭만완료");
            else if(walkerWalkStatus2 == 1) status.put("partnerWalk", "현재산책중");
            else status.put("partnerWalk", "산책매칭없음");

            //파트너로서 돌봄 상태값이 1인 것이 몇개 있는가
            int caregiverCareStatus1 = careMatchingService.getCaregiverStatus(accessToken,1);
            status.put("파트너로서돌봄예약확정수", caregiverCareStatus1);
            //파트너로서 돌봄 상태값이 2인 것이 몇개 있는가
            int caregiverCareStatus2 = careMatchingService.getCaregiverStatus(accessToken,2);
            status.put("파트너로서돌봄진행중수", caregiverCareStatus2);

            return ResponseEntity.ok(status);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너 엔티티 예상 ( id, 유저엔티티, 파트너프로필사진, 거주지, 반려동물경력, 시험성적, 시험횟수, 시험날짜, 파트너등록일, 파트너등록단계(임시저장체크용) )
    //파트너 권한 신청 임시저장 체크 - 어느 과정까지 했는가 / 몇번째 시험인가 / 시험을 볼 수 있는가 등
    @GetMapping("/partner/apply/check")
    public ResponseEntity<?> partnerApplyCheck(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            int partnerStep = myPageService.getPartnerApplyCheck(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("partnerStep", partnerStep);       //0: 신청전, 1: 신분증인식완료, 2: 추가정보입력완료, 3: 시험본 횟수 4: 시험불가다음날짜 5:시험완료
            if(partnerStep == 3){
                response.put("testCount", myPageService.getPartnerTestCount(accessToken));
            }else if(partnerStep == 4){
                response.put("nextTestDate", myPageService.getNextTestDate(accessToken));
            }
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너 권한 신청과정 1 - 신청하는 유저의 신분증사진을 받아서 ocr을 하는 서브 서버의 api를 호출,
    // 데이터베이스의 정보(이름,생년월일,성별)와 비교하여 일치하면 성공 일치하지않거나 인식이 안되면 재요청 / 성공시 파트너 신청 엔티티에 유저엔티티 가져와서 저장
    @PostMapping("/partner/apply/step1")
    public ResponseEntity<String> partnerApplyStep1(@RequestHeader("Authorization") String accessToken,
                                                    @RequestPart("identification_image") MultipartFile identificationImage
    ) {
        try {
            if(identificationImage == null){
                throw new IllegalArgumentException("신분증 이미지를 첨부해주세요");
            }
            // 신분증 인식 서비스 호출
            myPageService.identifyIdentification(accessToken, identificationImage);
            return ResponseEntity.ok("신분증 인식 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너 권한 신청과정 2 - 신청 하는 유저의 추가 정보 입력 ( 얼굴사진(파트너프로필용), 거주지, 반려동물 경력 )
    @PostMapping("/partner/apply/step2")
    public ResponseEntity<String> partnerApplyStep2(@RequestHeader("Authorization") String accessToken,
                                                    @RequestPart("face_image") MultipartFile faceImage,
                                                    @ModelAttribute("address") String address,
                                                    @ModelAttribute("career") String career
    ) {
        try {
            if (faceImage == null || faceImage.isEmpty()) {
                throw new IllegalArgumentException("얼굴 사진을 첨부해주세요");
            }

            // 비동기 이미지 업로드 서비스 호출
            CompletableFuture<String> imageUrlFuture = communityBoardService.uploadImageAsync(faceImage);
            String imageUrl = imageUrlFuture.join(); // 이미지 업로드가 완료될 때까지 대기하고 결과 받기

            // 파트너 추가 정보 입력
            myPageService.addPartnerStep2(accessToken, imageUrl, address, career);
            return ResponseEntity.ok("추가 정보 입력 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너 권한 신청과정 3 - 문제풀기 ( 문제 풀고 서버에서 채점 / 패스시 파트너 등록 완료 / 논패스시 재요청 / 3회 논패스시 3일 지나야 재요청 가능 )
    @PostMapping("/partner/apply/step3")
    public ResponseEntity<?> partnerApplyStep3(@RequestHeader("Authorization") String accessToken,
                                                    @RequestBody QuizDTO quizDTO
    ) {
        try {
            // 문제 풀기
            int score = myPageService.checkQuiz(accessToken, quizDTO);
            if(score >= 80){
                return ResponseEntity.ok("파트너 등록 완료");
            }else{
                return ResponseEntity.badRequest().body("파트너 등록 실패");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}