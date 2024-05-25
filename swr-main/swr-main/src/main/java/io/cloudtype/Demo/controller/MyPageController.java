package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.JoinDetailsDTO;
import io.cloudtype.Demo.Dto.PetDTO;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.MyPageService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final CommunityBoardService communityBoardService;

    public MyPageController(MyPageService myPageService, CommunityBoardService communityBoardService) {
        this.myPageService = myPageService;
        this.communityBoardService = communityBoardService;
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
            String imageUrl = null;
            if(petProfileImage != null){
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(petProfileImage);
            }
            // 유저의 반려동물 정보 추가
            int petId = myPageService.addPetStep1(accessToken, petName, petAge, species, gender, imageUrl);
            //petId를 반환
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
            //주인인지 체크
            myPageService.checkOwner(accessToken, petId);
            String imageUrl = myPageService.findImageUrlById(petId);
            if(imageChangeCheck && image == null){
                throw new IllegalArgumentException("이미지 변경시 반드시 이미지를 첨부해야 합니다.");
            } else if (imageChangeCheck) {  //이미지 삭제후 새로운 이미지 등록후 url받아오기
                if(imageUrl != null) communityBoardService.deleteImageGcs(imageUrl);
                imageUrl = communityBoardService.uploadImage(image);
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
}