package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mypage")
@Tag(name = "mypage", description = "마이페이지 관련 API")
@CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
public class MyPageController {

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private CommunityBoardService communityBoardService;


    @Operation(summary = "마이페이지 정보 조회", description = "마이페이지 정보를 조회하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "조회 성공_닉네임,이메일 반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = Map.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> myPage(@RequestHeader("Authorization") String accessToken) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 확인된 고유 ID를 가지고 데이터베이스에 해당 사용자가 이미 존재하는지 확인하고 가져오기
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);

            if (dbUserInfo != null) {
                // 클라이언트에게 전달할 Map 객체 생성
                Map<String, Object> responseUserInfo = new HashMap<>();

                // 가져온 사용자 정보 중에서 닉네임과 이메일 정보만을 추출하여 클라이언트로 보냄
                responseUserInfo.put("nickname", dbUserInfo.get("nickname"));
                responseUserInfo.put("email", dbUserInfo.get("email"));

                // JSON 형식으로 반환
                return ResponseEntity.ok().body(responseUserInfo);
            } else {
                // 사용자가 데이터베이스에 존재하지 않는 경우, 에러 응답을 반환하거나 다른 처리를 수행할 수 있음
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "핀 번호 확인", description = "핀 번호를 확인하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pin_number", description = "핀 번호", required = true)
    @ApiResponse(responseCode = "200", description = "핀 번호 일치_메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pin-check")
    public ResponseEntity<String> pinCheck(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody @NotNull Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 프론트로부터 받은 핀 번호
            String pinNumber = requestBody.get("pin_number");
            log.info("pinNumber : "+pinNumber);

            // 데이터베이스에서 해당 사용자의 핀 번호 가져오기
            String dbPinNumber = userInfoService.getPinNumberByUserId(userId);
            log.info("dbPinNumber : "+dbPinNumber);

            // 받은 핀 번호와 데이터베이스의 핀 번호 비교
            Map<String, Object> jsonResponse = new HashMap<>();
            if (pinNumber.equals(dbPinNumber)) {
                Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
                String nickname = (String) dbUserInfo.get("nickname");
                String phoneNumber = (String) dbUserInfo.get("phone_number");
                jsonResponse.put("success", "핀번호가 일치함");
                jsonResponse.put("nickname", nickname);
                jsonResponse.put("phone_number", phoneNumber);
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(jsonResponse);
                return ResponseEntity.ok().body(jsonString);
            } else {
                // 핀 번호가 일치하지 않는 경우, 400 상태 코드와 메시지 반환
                jsonResponse.put("bad", "핀번호가 불일치함");
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(jsonResponse);
                return ResponseEntity.badRequest().body(jsonString);
            }
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "정보 수정", description = "사용자 정보를 수정하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "nickname", description = "변경할 닉네임", required = false)
    @Parameter(name = "pin_number", description = "변경할 핀 번호", required = false)
    @Parameter(name = "phone_number", description = "변경할 전화번호", required = false)
    @ApiResponse(responseCode = "200", description = "정보 수정 성공_성공메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/edit-info")
    public ResponseEntity<String> editInfo(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody  Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);

            Map<String, Object> jsonResponse = new HashMap<>();
            int changeCount=0;  //0=변경없음, 1=이름변경, 10=핀번호변경, 100=전화번호변경

            // 닉네임을 수정할 경우, 중복된 닉네임이 있는지 확인
            String changeNickname = requestBody.get("nickname");
            String nowNickname= (String) dbUserInfo.get("nickname");
            if (changeNickname != null && !changeNickname.trim().isEmpty()) {
                // 모든 사용자의 닉네임을 가져와서 중복 검사
                List<String> allNicknames = userInfoService.getAllNicknames();
                boolean isNicknameDuplicate = allNicknames.stream()
                        .anyMatch(nickname -> nickname.equalsIgnoreCase(changeNickname));
                if (isNicknameDuplicate) {
                    jsonResponse.put("bad", "이미 사용중인 닉네임입니다.");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonString = objectMapper.writeValueAsString(jsonResponse);
                    return ResponseEntity.badRequest().body(jsonString);
                } else changeCount+=1;
            }

            // 핀 번호를 수정할 경우
            String pinNumber = requestBody.get("pin_number");
            if (pinNumber != null && !pinNumber.trim().isEmpty() && pinNumber.matches("\\d{6}")) {
                int nowPinNunber = (int) dbUserInfo.get("pin_number");
                if(nowPinNunber == Integer.parseInt(pinNumber)){
                    jsonResponse.put("bad", "현재 사용중인 핀번호입니다.");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonString = objectMapper.writeValueAsString(jsonResponse);
                    return ResponseEntity.badRequest().body(jsonString);
                }else changeCount+=10;
            }

            // 전화번호를 수정할 경우
            String phoneNumber = requestBody.get("phone_number");
            if (phoneNumber != null && !phoneNumber.trim().isEmpty() && phoneNumber.matches("01[0-9]-\\d{4}-\\d{4}")) {
                String nowPhoneNumber = (String) dbUserInfo.get("phone_number");
                if(nowPhoneNumber.equals(pinNumber)){
                    jsonResponse.put("bad", "현재 사용중인 전화번호입니다.");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonString = objectMapper.writeValueAsString(jsonResponse);
                    return ResponseEntity.badRequest().body(jsonString);
                }else changeCount+=100;
            }
            if(changeCount==1 || changeCount==11 || changeCount==101 || changeCount==111){
                userInfoService.updateUserInfo(userId,"nickname",changeNickname,nowNickname);
            }
            if(changeCount==10 || changeCount==11 || changeCount==110 || changeCount==111){
                userInfoService.updateUserInfo(userId,"pin_number",pinNumber,nowNickname);
            }
            if(changeCount==100 ||changeCount==101 || changeCount==110 || changeCount==111){
                userInfoService.updateUserInfo(userId,"phone_number",phoneNumber,nowNickname);
            }
            jsonResponse.put("success", "정보가 성공적으로 수정되었습니다.");
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(jsonResponse);
            return ResponseEntity.ok().body(jsonString);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "반려동물 기초정보 등록", description = "반려동물을 등록하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pet_name", description = "반려동물 이름", required = true)
    @Parameter(name = "pet_age", description = "반려동물 나이(출생년도를 계산하기 때문에 올해 태어났다면 0살)", required = true)
    @Parameter(name = "species", description = "반려동물 종", required = true)
    @Parameter(name = "pet_profile_image", description = "이미지파일", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info/profile")
    public ResponseEntity<String> petInfoProfile(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @RequestPart("pet_profile_image") MultipartFile petProfileImage
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String ownerNickname = (String) dbUserInfo.get("nickname");

            String imageUrl = null;
            if(petProfileImage != null){
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(petProfileImage);
                log.info("imageUrl: " + imageUrl);
            }
            boolean insertOrUpdate = true;
            Long petId = userInfoService.savePetInfo1(petName,petAge,species,imageUrl, ownerNickname, insertOrUpdate, 0L);
            Map<String, Object> response = new HashMap<>();
            response.put("pet_id", petId);
            response.put("success", "반려동물 기본 프로필 저장 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "반려동물 추가정보 등록 및 수정", description = "반려동물을 등록과 수정하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pet_id",description = "반려동물 고유id / Long", required = true)
    @Parameter(name = "weight", description = "반려동물 무게 / Integer")
    @Parameter(name = "neutering", description = "반려동물 중성화여부 / Boolean")
    @Parameter(name = "animal_hospital", description = "이용하는 동물병원 / String")
    @Parameter(name = "vaccination", description = "예방접종 / String")
    @Parameter(name = "etc", description = "특이사항 / String")
    @ApiResponse(responseCode = "200", description = "반려동물 추가 정보 등록 및 수정완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info/detail")
    public ResponseEntity<String> petInfoDetail(@RequestHeader("Authorization") String accessToken,
                                          @RequestBody  Map<String, Object> petInfo) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String ownerNickname = (String) dbUserInfo.get("nickname");
            String check = userInfoService.savePetInfo2(ownerNickname, petInfo);
            Map<String, String> response = new HashMap<>();
            if(check.equals("success")){
                // 성공 메시지 반환
                response.put("success", "추가정보 등록 성공");
                return ResponseEntity.ok(response.toString());
            }
            else{
                response.put("bad", check);
                return ResponseEntity.badRequest().body(response.toString());
            }
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "반려동물 목록", description = "등록한 반려동물 목록을 반환하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "등록 반려동물 목록 반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info")
    public ResponseEntity<Map<String, Object>> petInfo(@RequestHeader("Authorization") String accessToken) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            // 가입된 사람만 사용하도록 확인
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String ownerNickname = (String) dbUserInfo.get("nickname");
            Map<String,Object> petInfoList = userInfoService.petInfoList(ownerNickname);
            if(petInfoList.isEmpty()){
                Map<String, Object> response = new HashMap<>();
                response.put("message", "등록된 반려동물이 없습니다.");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(petInfoList);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "반려동물 기초정보 수정", description = "기본프로필을 수정하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pet_name", description = "반려동물 이름", required = true)
    @Parameter(name = "pet_age", description = "반려동물 나이(출생년도를 계산하기 때문에 올해 태어났다면 0살)", required = true)
    @Parameter(name = "species", description = "반려동물 종", required = true)
    @Parameter(name = "pet_profile_image", description = "이미지파일", required = true)
    @Parameter(name = "pet_id",description = "반려동물 고유id / Long", required = true)
    @Parameter(name = "imageChangeCheck",description = "이미지 변경 여부 / Boolean", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info/profile/edit")
    public ResponseEntity<String> petInfoProfileEdit(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("pet_name") String petName,
            @ModelAttribute("pet_age") Integer petAge,
            @ModelAttribute("species") String species,
            @ModelAttribute("pet_id") Long petId,
            @ModelAttribute("imageChangeCheck") Boolean imageChangeCheck,
            @RequestPart("pet_profile_image") MultipartFile petProfileImage
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Map<String,Object> petInfo = userInfoService.getPetInfoByPetId(petId);
            String ownerNickname = (String) petInfo.get("owner_nickname");
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            String imageUrl = userInfoService.getImageUrlByPetId(petId);

            if(petProfileImage != null && imageChangeCheck){
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(petProfileImage);
                log.info("imageUrl: " + imageUrl);
            }
            boolean insertOrUpdate = false;
            Long petId2 = userInfoService.savePetInfo1(petName,petAge,species,imageUrl, ownerNickname, insertOrUpdate, petId);
            Map<String, Object> response = new HashMap<>();
            response.put("pet_id", petId2);
            response.put("success", "반려동물 기본 프로필 저장 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "반려동물 정보 삭제", description = "반려동물 프로필 삭제하는 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "pet_id",description = "반려동물 고유id / Long", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/pet-info/profile/delete")
    public ResponseEntity<String> petInfoDelete(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Long petId = (Long) requestBody.get("pet_id");
            Map<String,Object> petInfo = userInfoService.getPetInfoByPetId(petId);
            String ownerNickname = (String) petInfo.get("owner_nickname");
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            String imageUrl = userInfoService.getImageUrlByPetId(petId);
            userInfoService.deletePetInfo(petId, imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("success", "반려동물 프로필 삭제 성공, 반려동물 목록 페이지로 이동하세요");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
