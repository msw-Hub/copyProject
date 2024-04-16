package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.CommunityBoardService;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/community")
@Tag(name = "community", description = "커뮤니티 게시판 관련 API")
public class CommunityBoardController {

    private final CommunityBoardService communityBoardService;

    @Autowired
    private KakaoService kakaoService;
    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    public CommunityBoardController(CommunityBoardService communityBoardService) {
        this.communityBoardService = communityBoardService;
    }

    @PostMapping("")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "커뮤니티 게시판 글 목록 가져오기", description = "최신 글부터 페이지별로 반환합니다.")
    @Parameter(name = "page", description = "페이지 번호 (기본값 1)", required = true)
    @ApiResponse(responseCode = "200", description = "커뮤니티 게시판 글 목록 반환 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, Object>> getCommunityBoard(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int page = (int) requestBody.get("page");
            // 가입된 사람만 사용하도록 확인
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Map<String, Object> communityBoardList = communityBoardService.getCommunityBoardPosts(page);
            return ResponseEntity.ok(communityBoardList);
        } catch (Exception e) {
            log.error("Failed to fetch community board posts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "게시글 작성", description = "게시글을 작성합니다.")
    @Parameter(name = "title", description = "게시글 제목", required = true)
    @Parameter(name = "content", description = "게시글 내용", required = true)
    @Parameter(name = "image", description = "이미지파일 / 없으면 null",required=false)
    @ApiResponse(responseCode = "200", description = "글 작성 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, String>> writePost(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute("title") String title,
            @ModelAttribute("content") String content,
            @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");

            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 게시글 작성
            communityBoardService.writePost(nickname, title, content, imageUrl);

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "글 작성 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to write post", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/replies")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "해당글의 댓글모음", description = "요청하면 해당글의 댓글들을 불러옵니다.")
    @Parameter(name = "communityBoardId", description = "게시글 ID", required = true)
    @ApiResponse(responseCode = "200", description = "댓글들 반환",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<Map<String, Object>>> getReplies(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            // requestBody에서 communityBoardId 추출
            Long communityBoardId = Long.valueOf(requestBody.get("communityBoardId").toString());

            //조회수 카운팅 +1
            communityBoardService.plusViews(communityBoardId);

            // 게시글 ID를 기반으로 해당 게시글의 댓글 가져오기
            List<Map<String, Object>> replies = communityBoardService.getCommentsForPost(communityBoardId);
            return ResponseEntity.ok(replies);
        } catch (Exception e) {
            log.error("Failed to fetch replies for community board post", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/remove/board")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "해당글 삭제", description = "요청하면 해당글에 관련된 모든것이 삭제됩니다.")
    @Parameter(name = "communityBoardId", description = "게시글 ID", required = true)
    @ApiResponse(responseCode = "200", description = "삭제완료",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<Map<String, String>> deleteCommunityBoardPost(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam Long communityBoardId
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String dbNickname = (String) dbUserInfo.get("nickname");
            String boardNickname = userInfoService.getUserIdByCommunityBoardNickname(communityBoardId);
            if(Objects.equals(boardNickname, dbNickname)) {
                // 게시글 삭제
                communityBoardService.deleteCommunityBoard(communityBoardId);
            } else {
                return ResponseEntity.badRequest().build();
            }

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "삭제 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
        log.error("Failed to delete", e);
        return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/edit/board")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "해당글 수정", description = "요청하면 해당글이 수정됩니다.")
    @Parameter(name = "communityBoardId", description = "게시글 ID", required = true)
    @Parameter(name = "title", description = "게시글 제목", required = true)
    @Parameter(name = "content", description = "게시글 내용", required = true)
    @Parameter(name = "imageChangeCheck", description = "이미지파일 바뀌면 true, 안바뀌면 false", required = true)
    @Parameter(name = "image", description = "이미지파일 / 없으면 null")
    @ApiResponse(responseCode = "200", description = "글 수정 성공",content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    public ResponseEntity<Map<String, String>> editPost(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam("communityBoardId") Long communityBoardId,
            @ModelAttribute("title") String title,
            @ModelAttribute("content") String content,
            @ModelAttribute("imageChangeCheck") Boolean imageChangeCheck,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String dbNickname = (String) dbUserInfo.get("nickname");

            String boardNickname = userInfoService.getUserIdByCommunityBoardNickname(communityBoardId);
            if(Objects.equals(boardNickname, dbNickname)) {
                // 게시글 수정
                communityBoardService.editCommunityBoard(communityBoardId, title, content, image, imageChangeCheck);
            } else {
                log.error("userId not matched");
                return ResponseEntity.badRequest().build();
            }
            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "글 수정 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to edit", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/create/reply")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "댓글 작성", description = "요청하면 해당 댓글이 작성됩니다.")
    @Parameter(name = "communityBoardId", description = "게시글 ID", required = true)
    @Parameter(name = "reply", description = "댓글내용", required = true)
    @ApiResponse(responseCode = "200", description = "댓글작성완료",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<Map<String, String>> createReply(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");

            Long communityBoardId = Long.valueOf(requestBody.get("communityBoardId").toString());
            String reply = (String) requestBody.get("reply");

            communityBoardService.writeReply(communityBoardId, nickname, reply);

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "댓글 작성 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to write", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/like/board")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "좋아요 클릭", description = "요청하면 게시글의 좋아요가 올라갑니다.")
    @Parameter(name = "communityBoardId", description = "게시글 ID", required = true)
    @ApiResponse(responseCode = "200", description = "좋아요확인",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<Map<String, String>> likeBoard(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Long communityBoardId = Long.valueOf(requestBody.get("communityBoardId").toString());
            String check = communityBoardService.plusBoardLikes(communityBoardId, nickname);
            Map<String, String> response = new HashMap<>();
            if(check.equals("success")){
                // 성공 메시지 반환
                response.put("success", "게시글좋아요 성공");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("bad", check);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Failed to plus-like", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/like/reply")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "좋아요 클릭", description = "요청하면 댓글의 좋아요가 올라갑니다.")
    @Parameter(name = "communityReplyId", description = "댓글 ID", required = true)
    @ApiResponse(responseCode = "200", description = "좋아요확인",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<Map<String, String>> likeReply(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            Long communityReplyId = Long.valueOf(requestBody.get("communityReplyId").toString());
            String check = communityBoardService.plusReplyLikes(communityReplyId, nickname);
            Map<String, String> response = new HashMap<>();
            if(check.equals("success")){
                // 성공 메시지 반환
                response.put("success", "댓글좋아요 성공");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("bad", check);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Failed to plus-like", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @DeleteMapping("/remove/reply")
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @Operation(summary = "댓글삭제", description = "요청하면 댓글및 댓글의 좋아요를 삭제합니다.")
    @Parameter(name = "communityReplyId", description = "댓글 ID", required = true)
    @ApiResponse(responseCode = "200", description = "댓글삭제완료",content = @Content(mediaType = "application/json",schema = @Schema(implementation = List.class)))
    public ResponseEntity<Map<String, String>> removeReply(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam Long communityReplyId
    ) {
        try {
            // 카카오 API를 통해 사용자 정보 가져오기
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            int count = kakaoService.processUser(userInfo);
            if (count == 0) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = (Long) userInfo.get("userId");
            Map<String, Object> dbUserInfo = userInfoService.getUserInfoById(userId);
            String nickname = (String) dbUserInfo.get("nickname");
            communityBoardService.deleteReplyBeforeCheck(communityReplyId, nickname);

            // 성공 메시지 반환
            Map<String, String> response = new HashMap<>();
            response.put("success", "댓글 삭제 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to minus", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
