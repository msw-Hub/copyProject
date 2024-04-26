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
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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


    @Operation(summary = "매칭-산책 글 작성전, 반려동물 id, name, profile 전달하며 반려동물 고르기", description = "매칭을 위한 반려동물 정보반환")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "반려동물 id, name, profile 반환")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/post1")
    public ResponseEntity<String> walkPost1(
            @RequestHeader("Authorization") String accessToken
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

    @Operation(summary = "매칭-산책 글 작성", description = "/post1을 먼저 거친후 와주세요 + etc를 제외한 정보가 모두 기입된 반려동물만 이용가능합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_time", description = "산책시간 / INT(30/60/90/120)", required = true)
    @Parameter(name = "pet_info_id",description = "서비스 이용할 반려동물 고유id / Long", required = true)
    @Parameter(name = "latitude",description = "신청위치의 위도 / Double", required = true)
    @Parameter(name = "longitude",description = "신청위치의 경도 / Double", required = true)
    @Parameter(name = "title", description = "신청글제목 / String", required = true)
    @Parameter(name = "content", description = "산책전 만날 상세주소, 요청사항, 주의사항 / String", required = true)
    @ApiResponse(responseCode = "200", description = "성공메세지반환")
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
            Long petInfoId = Long.valueOf(requestBody.get("pet_info_id").toString());
            String ownerNickname = userInfoService.getOwnerNicknameByPetId(petInfoId);
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            //산책글 작성 서비스 호출
            //넘길데이터 nickname=writerNickname,pet_info_id, walk_time, walk_date, latitude, longitude, title,content, status=0
            //petInfoId로 선택한 반려동물의 정보가 모두 기입이 되었는지 확인
            if(!userInfoService.checkPetInfo(petInfoId))
                return ResponseEntity.badRequest().build();
            Integer walkTime = (Integer) requestBody.get("walk_time");
            double latitude = ((Number) requestBody.get("latitude")).doubleValue();
            double longitude = ((Number) requestBody.get("longitude")).doubleValue();
            String title = (String) requestBody.get("title");
            String content = (String) requestBody.get("content");
            String message = walkMatchingBoardService.writeWalkPost(nickname, petInfoId, walkTime, latitude, longitude, title, content);

            //글이 작성되고 5분뒤에 자동 삭제되는 시스템은 트리거 및 이벤트로 완료
            //글을 작성되면, delete_queue 테이블에 해당 글의 id와 5분을 더한 시간을 넣어주고 1분마다 status값을 확인해서 삭제
            //만일 5분이 지나면, delete_queue의 id로 walk_matching_board 테이블에서 해당 id의 글을 삭제. 이때, 삭제된다면 waiting_list 테이블은 트리거로 삭제
            //이후 delete_queue 테이블에서 해당 id의 행을 삭제
            //만일 5분전에 status가 1이 되면, delete_queue에서만 해당 id의 행을 삭제
            //글을 중간에 삭제하면, 아래의 삭제 매서드 동작

            Map<String, Object> response = new HashMap<>();
            response.put("success", message);
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 목록반환, 파트너만 볼 수 있도록", description = "매칭 게시판에 산책 요청글을 반환합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "page", description = "페이지 번호 / Integer", required = true)
    @Parameter(name = "now_latitude", description = "현재 위도 / Double", required = true)
    @Parameter(name = "now_longitude",description = "현재 경도 / Double", required = true)
    @Parameter(name = "max_distance",description = "최대 거리 / Double(1/3/5)", required = true)
    @ApiResponse(responseCode = "200", description = "산책 신청글 객체로 반환")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("")
    public ResponseEntity<String> walkList(
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
            // 가져온 유저 정보에서 고유 ID를 확인. 해당 ID를 가진 유저가 파트너권한이 있는지를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            if ((int) dbUserInfo.get("partnership") == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "파트너 권한이 없습니다.");
                return ResponseEntity.ok(response.toString());
            }
            //산책 신청글 목록 반환 / 위치 계산
            double now_latitude = ((Number) requestBody.get("now_latitude")).doubleValue();
            double now_longitude = ((Number) requestBody.get("now_longitude")).doubleValue();
            double max_distance = ((Number) requestBody.get("max_distance")).doubleValue();
            List<Map<String, Object>> response = walkMatchingBoardService.getWalkPostListWithDistance((int) requestBody.get("page"),
                    now_latitude, now_longitude, max_distance);
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 수정", description = "/post1을 먼저 거친후 와주세요, 수정시 등록시간이 수정시간으로 변경됩니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_matching_board_id", description = "산책글 고유id / Long", required = true)
    @Parameter(name = "walk_time", description = "산책시간 / INT(30/60/90/120)", required = true)
    @Parameter(name = "pet_info_id",description = "서비스 이용할 반려동물 고유id / Long", required = true)
    @Parameter(name = "latitude",description = "신청위치의 위도 / Double", required = true)
    @Parameter(name = "longitude",description = "신청위치의 경도 / Double", required = true)
    @Parameter(name = "title", description = "신청글제목 / String", required = true)
    @Parameter(name = "content", description = "산책전 만날 상세주소, 요청사항, 주의사항 / String", required = true)
    @ApiResponse(responseCode = "200", description = "산책글 수정 성공")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/edit")
    public ResponseEntity<String> walkEdit(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            //본인 펫이 맞는지 확인
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Long petInfoId = Long.valueOf(requestBody.get("pet_info_id").toString());
            String ownerNickname = userInfoService.getOwnerNicknameByPetId(petInfoId);
            if(!nickname.equals(ownerNickname)){
                return ResponseEntity.badRequest().build();
            }
            //본인이 작성한 글인지를 확인
            Long walkMatchingBoardId = Long.valueOf(requestBody.get("walk_matching_board_id").toString());
            if(!userInfoService.checkWriter(walkMatchingBoardId, ownerNickname))
                return ResponseEntity.badRequest().build();

            //산책글 수정 서비스 호출
            Integer walkTime = (Integer) requestBody.get("walk_time");
            double latitude = ((Number) requestBody.get("latitude")).doubleValue();
            double longitude = ((Number) requestBody.get("longitude")).doubleValue();
            String title = (String) requestBody.get("title");
            String content = (String) requestBody.get("content");
            walkMatchingBoardService.updateWalkPost(walkMatchingBoardId,petInfoId, walkTime, latitude, longitude, title, content);

            Map<String, Object> response = new HashMap<>();
            response.put("success", "산책글 수정 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 삭제", description = "매칭전에만 삭제가 가능합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_matching_board_id", description = "산책글 고유id / Long", required = true)
    @ApiResponse(responseCode = "200", description = "삭제 성공 메세지반환")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/delete")
    public ResponseEntity<String> walkDelete(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            //본인 펫이 맞는지 확인
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            //본인이 작성한 글인지를 확인
            Long walkMatchingBoardId = Long.valueOf(requestBody.get("walk_matching_board_id").toString());
            if(!userInfoService.checkWriter(walkMatchingBoardId, nickname))
                return ResponseEntity.badRequest().build();

            //산책글 삭제 서비스 호출
            walkMatchingBoardService.deleteWalkPost(walkMatchingBoardId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", "산책요청글 삭제 성공");
            return ResponseEntity.ok(response.toString());
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 신청 - 파트너만", description = "주의사항 - 신청을 취소할 수 없으니 신중히 눌러주세요")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_matching_board_id", description = "산책글 고유id / Long", required = true)
    @ApiResponse(responseCode = "200", description = "매칭 성공 메세지반환")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/accept")
    public ResponseEntity<Map<String, String>> walkAccept(
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
            // 가져온 유저 정보에서 고유 ID를 확인. 해당 ID를 가진 유저가 파트너권한이 있는지를 확인
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            if ((int) dbUserInfo.get("partnership") == 0) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "파트너 권한이 없습니다.");
                return ResponseEntity.ok(response);
            }
            //산책글 신청 서비스 호출
            String message = walkMatchingBoardService.walkApply(Long.valueOf(requestBody.get("walk_matching_board_id").toString()),
                    dbUserInfo.get("nickname").toString());

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", message);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "매칭-산책 글 신청수락 - 글작성자만", description = "산책 요청을 수락합니다.")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "walk_matching_board_id", description = "산책글 고유id / Long", required = true)
    @Parameter(name = "waiting_list_id", description = "신청자 및 신청정보 리스트id / Long", required = true)
    @Parameter(name = "waiter_nickname", description = "신청자 닉네임 / String", required = true)
    @ApiResponse(responseCode = "200", description = "삭제 성공 메세지반환")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/apply")
    public ResponseEntity<Map<String, String>> walkApply(
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
            String nickname = userInfo.get("nickname").toString();
            Long walkMatchingBoardId = Long.valueOf(requestBody.get("walk_matching_board_id").toString());
            //글 작성자인지 확인
            if(!userInfoService.checkWriter(walkMatchingBoardId, nickname))
                return ResponseEntity.badRequest().build();
            //글이 유효한지확인(글존재o + status=0)
            if (!walkMatchingBoardService.checkWalkPost(walkMatchingBoardId))
                return ResponseEntity.badRequest().build();
            //신청자 닉네임과 신청정보 리스트id로 신청한 사람이 유효한지 교차검증
            Long waitingListId = Long.valueOf(requestBody.get("waiting_list_id").toString());
            String waiterNickname = requestBody.get("waiter_nickname").toString();
            if(!walkMatchingBoardService.checkWalkApply(waitingListId, waiterNickname))
                return ResponseEntity.badRequest().build();
            //모든것이 유효하다면 매칭 수락 서비스 호출
            //status=1로 변경, delete_queue에 해당 id의 행을 삭제, walk_matching_board 테이블에서 matching_partner_nickname,matching_date 추가
            String message = walkMatchingBoardService.walkAccept(walkMatchingBoardId, waiterNickname);

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", message);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
