package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.Community.CommentDTO;
import io.cloudtype.Demo.Dto.Community.CommunityBoardDTO;
import io.cloudtype.Demo.service.CommunityBoardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/community")
public class CommunityBoardController {
    private final CommunityBoardService communityBoardService;

    public CommunityBoardController(CommunityBoardService communityBoardService) {
        this.communityBoardService = communityBoardService;
    }

    @GetMapping("/{page}")
    public ResponseEntity<Map<String, Object>> getCommunityBoard(
            @PathVariable("page") int page
    ) {
        List<CommunityBoardDTO> pageResult = communityBoardService.getCommunityBoardPosts(page-1);
        Map<String, Object> response = new HashMap<>();
        response.put("posts", pageResult);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/{community_board_id}")
    public ResponseEntity<Map<String, Object>> getComment(
            @PathVariable("community_board_id") int communityBoardId
    ) {
        try {
            List<CommunityBoardDTO> post = communityBoardService.getPost(communityBoardId);
            List<CommentDTO> comments = communityBoardService.getComments(communityBoardId);
            Map<String, Object> response = new HashMap<>();
            response.put("post", post);
            response.put("comments", comments);
            return ResponseEntity.ok(response);
        } catch (Exception e) { // 일반적인 예외 처리를 위해 Exception을 사용
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage()); // 예외 메시지를 오류 응답에 포함
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/create/post")
    public ResponseEntity<?> writePost(@RequestHeader("Authorization") String accessToken,
                                       @ModelAttribute("title") String title,
                                       @ModelAttribute("content") String content,
                                       @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // 이미지 업로드 서비스 호출
                imageUrl = communityBoardService.uploadImage(image);
            }
            // 게시글 작성
            communityBoardService.writePost(accessToken, title, content, imageUrl);
            return ResponseEntity.ok("게시글 작성 성공");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/create/comment")
    public ResponseEntity<?> writeComment(@RequestHeader("Authorization") String accessToken,
                                          @RequestBody Map<String, String> requestBody
    ) {
        try {
            String content = requestBody.get("content");
            int communityBoardId = Integer.parseInt(requestBody.get("community_board_id"));

            communityBoardService.writeComment(accessToken, communityBoardId, content);
            return ResponseEntity.ok("댓글 작성 성공");
        }catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/like/post")
    public ResponseEntity<?> likePost(@RequestHeader("Authorization") String accessToken,
                                      @RequestBody Map<String, Integer> requestBody
    ) {
        try {
            int communityBoardId = requestBody.get("community_board_id");
            communityBoardService.likePost(accessToken, communityBoardId);
            return ResponseEntity.ok().body("좋아요를 성공적으로 등록했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/like/comment")
    public ResponseEntity<?> likeComment(@RequestHeader("Authorization") String accessToken,
                                         @RequestBody Map<String, Integer> requestBody
    ) {
        try {
            int commentId = requestBody.get("comment_id");
            communityBoardService.likeComment(accessToken, commentId);
            return ResponseEntity.ok().body("댓글 좋아요를 성공적으로 등록했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/remove/comment")
    public ResponseEntity<?> removeComment(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody Map<String, Integer> requestBody
    ) {
        try {
            int commentId = requestBody.get("comment_id");
            communityBoardService.removeComment(accessToken, commentId);
            return ResponseEntity.ok().body("댓글 삭제를 성공적으로 완료했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/remove/post")
    public ResponseEntity<?> removePost(@RequestHeader("Authorization") String accessToken,
                                        @RequestBody Map<String, Integer> requestBody
    ) {
        try {
            int communityBoardId = requestBody.get("community_board_id");
            communityBoardService.removePost(accessToken, communityBoardId);
            return ResponseEntity.ok().body("게시글 삭제를 성공적으로 완료했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/edit/post")
    public ResponseEntity<?> editPost(@RequestHeader("Authorization") String accessToken,
                                      @ModelAttribute("title") String title,
                                      @ModelAttribute("content") String content,
                                      @ModelAttribute("community_board_id") int communityBoardId,
                                      @ModelAttribute("image_change_check") Boolean imageChangeCheck,
                                      @RequestPart(value = "image", required=false) MultipartFile image
    ) {
        try {
            String imgUrl = communityBoardService.findImageUrlById(communityBoardId);   //ingUrl을 반환 없으면 null
            if (imageChangeCheck && image == null) {    //이미지를 삭제해서 올린경우 - gcs에서 이미지 삭제후 imgUrl 변경
                if(imgUrl != null) {  //기존 이미지가 존재하면 삭제
                    communityBoardService.deleteImageGcs(imgUrl);
                    imgUrl= null;   //삭제했으니 null로 변경
                }else {
                    throw new IllegalStateException("db내 삭제할 이미지가 없습니다. image_change_check를 false로 변경해주세요");
                }
            }else if (imageChangeCheck) {
                if(imgUrl!= null) { //이미지를 변경해서 올린경우 - gcs에서 이미지 삭제후 새이미지업로두 후 imgUrl 변경
                    communityBoardService.deleteImageGcs(imgUrl);
                }
                imgUrl = communityBoardService.uploadImage(image);  //변경된url임
            }
            communityBoardService.editPost(accessToken, communityBoardId, title, content, imgUrl);
            return ResponseEntity.ok().body("게시글 수정을 성공적으로 완료했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/edit/comment")
    public ResponseEntity<?> editComment(@RequestHeader("Authorization") String accessToken,
                                         @RequestBody Map<String, String> requestBody
    ) {
        try {
            String content = requestBody.get("content");
            int commentId = Integer.parseInt(requestBody.get("comment_id"));
            communityBoardService.editComment(accessToken, commentId, content);
            return ResponseEntity.ok().body("댓글 수정을 성공적으로 완료했습니다");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}