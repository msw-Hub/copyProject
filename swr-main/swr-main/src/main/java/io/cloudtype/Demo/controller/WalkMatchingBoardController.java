package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.WalkMatchingBoardService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/walk")
@Tag(name = "matching-walk", description = "매칭 게시판 관련 API")
public class WalkMatchingBoardController {
    private final WalkMatchingBoardService walkMatchingBoardService;
    @Autowired
    private KakaoService kakaoService;
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    public WalkMatchingBoardController(WalkMatchingBoardService walkMatchingBoardService) {
        this.walkMatchingBoardService = walkMatchingBoardService;
    }


    @Operation(summary = "매칭-산책 글 작성전, 반려동물 id, name, profile 반환", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "반려동물 id, name, profile 반환성공", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/post1")
    public ResponseEntity<String> walkPost1(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 가입된 사람만 사용하도록 확인
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");

            //등록한 반려동물 리스트 반환
            Map<String, Object> response = userInfoService.getPetInfoByOwnerNickname(nickname);
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "매칭-산책 글 작성", description = "매칭 게시판에 산책 요청글을 작성합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_time", description = "산책시간 / INT(30/60/90/120)", required = true)
    @Parameter(name = "walk_date", description = "산책예약날짜 / DATE(년월일)", required = true)
    @Parameter(name = "pet_id",description = "서비스 이용할 반려동물 고유id / Long", required = true)
    @Parameter(name = "latitude",description = "신청위치의 위도 / Double", required = true)
    @Parameter(name = "longitude",description = "신청위치의 경도 / Double", required = true)
    @Parameter(name = "title", description = "신청글제목 / String", required = true)
    @Parameter(name = "content", description = "산책 요청사항 및 주의사항 / String", required = true)
    @ApiResponse(responseCode = "200", description = "반려동물 기초 정보 등록완료", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/post2")
    public ResponseEntity<String> walkPost2(
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
            //산책글 작성 서비스 호출
            //넘길데이터 nickname=owner_nickname,pet_id=choosing_pet, walk_time, walk_date, latitude, longitude, title,content, status=0
            Long choosing_pet = (Long) requestBody.get("pet_id");
            //petId가 유효한지 검색

            Integer walk_time = (Integer) requestBody.get("walk_time");
            Date walk_date = (Date) requestBody.get("walk_date");
            Double latitude = (Double) requestBody.get("latitude");
            Double longitude = (Double) requestBody.get("longitude");
            String title = (String) requestBody.get("title");
            String content = (String) requestBody.get("content");
            walkMatchingBoardService.writeWalkPost(nickname, choosing_pet, walk_time, walk_date, latitude, longitude, title, content);

            Map<String, Object> response = new HashMap<>();
            response.put("success", "산책글 작성 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
