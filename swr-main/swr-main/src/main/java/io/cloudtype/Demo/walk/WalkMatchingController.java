package io.cloudtype.Demo.walk;

import io.cloudtype.Demo.walk.DTO.WaiterDTO;
import io.cloudtype.Demo.walk.DTO.WalkMatchingDTO;
import io.cloudtype.Demo.walk.DTO.WalkMatchingDetailsDTO;
import io.cloudtype.Demo.walk.entity.WalkMatchingEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/walk")
public class WalkMatchingController {
    private final WalkMatchingService walkMatchingService;

    @Autowired
    public WalkMatchingController(WalkMatchingService walkMatchingService){
        this.walkMatchingService = walkMatchingService;
    }


    //  /mypage/pet/list에서 펫정보를 받아온 이후 고른 펫을 받음
    //저장되면 DB 트리거에 의해서 자동 5분이 지나면 삭제, 단 status가 0인 경우에만 해당. 1로 바뛰면 유지
    @PostMapping("/create/post")
    public ResponseEntity<?> createPost(@RequestHeader("Authorization") String accessToken,
                                        @RequestBody WalkMatchingEntity walkMatchingEntity
    ) {
        try {
            walkMatchingService.createPost(accessToken, walkMatchingEntity,0);
            return ResponseEntity.ok("게시글 작성 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/list")
    public ResponseEntity<?> getList(@RequestHeader("Authorization") String accessToken,
                                          @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int page = (int) requestBody.get("page");
            double nowLatitude = ((Number) requestBody.get("now_latitude")).doubleValue();
            double nowLongitude = ((Number) requestBody.get("now_longitude")).doubleValue();
            double maxDistance = ((Number) requestBody.get("max_distance")).doubleValue();

            Page<WalkMatchingDTO> pageResult = walkMatchingService.getList(accessToken, page - 1, nowLatitude, nowLongitude, maxDistance);
            return ResponseEntity.ok(pageResult);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/edit/post")
    public ResponseEntity<?> editPost(@RequestHeader("Authorization") String accessToken,
                                      @RequestBody WalkMatchingEntity walkMatchingEntity
    ) {
        try {
            walkMatchingService.createPost(accessToken, walkMatchingEntity,1);
            return ResponseEntity.ok("게시글 수정 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/apply/{postId}")
    public ResponseEntity<?> apply(@RequestHeader("Authorization") String accessToken,
                                      @PathVariable int postId
    ) {
        try {
            walkMatchingService.apply(accessToken, postId);
            return ResponseEntity.ok("신청 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/delete/apply/{postId}")
    public ResponseEntity<?> deleteApply(@RequestHeader("Authorization") String accessToken,
                                      @PathVariable int postId
    ) {
        try {
            walkMatchingService.deleteApply(accessToken, postId);
            return ResponseEntity.ok("신청 취소 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/myPost")
    public ResponseEntity<?> myPost(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            WalkMatchingDetailsDTO myPost = walkMatchingService.myPost(accessToken);
            return ResponseEntity.ok(myPost);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/myPost/applier")
    public ResponseEntity<?> myPostApplier(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<WaiterDTO> applierList = walkMatchingService.getApplierList(accessToken);
            return ResponseEntity.ok(applierList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/myApply")
    public ResponseEntity<?> myApply(@RequestHeader("Authorization") String accessToken,
                                        @RequestBody Map<String, Object> requestBody
    ) {
        try {
            double nowLatitude = (double) requestBody.get("now_latitude");
            double nowLongitude = (double) requestBody.get("now_longitude");

            List<WalkMatchingDTO> myApply = walkMatchingService.myApply(accessToken, nowLatitude, nowLongitude);
            return ResponseEntity.ok(myApply);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/myApply/progress")
    public ResponseEntity<?> myApplyProgress(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            WalkMatchingDetailsDTO myApplyProgress = walkMatchingService.myApplyProgress(accessToken);
            return ResponseEntity.ok(myApplyProgress);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/accept")
    public ResponseEntity<?> accept(@RequestHeader("Authorization") String accessToken,
                                   @RequestBody Map<String, Object> requestBody
    ) {
        try {
            int waiterListId = (int) requestBody.get("waiter_list_id");
            int postId = (int) requestBody.get("post_id");
            int waiterId = (int) requestBody.get("waiter_id");
            walkMatchingService.accept(accessToken, waiterListId, postId, waiterId);
            return ResponseEntity.ok("수락 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/delete/{postId}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String accessToken,
                                   @PathVariable int postId
    ) {
        try {
            walkMatchingService.deletePost(accessToken, postId);
            return ResponseEntity.ok("삭제 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/start/{postId}")
    public ResponseEntity<?> start(@RequestHeader("Authorization") String accessToken,
                                   @PathVariable int postId
    ) {
        try {   //파트너의 산책시작버튼
            walkMatchingService.start(accessToken, postId);
            return ResponseEntity.ok("산책 시작");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/complete/{postId}")
    public ResponseEntity<?> end(@RequestHeader("Authorization") String accessToken,
                                   @PathVariable int postId
    ) {
        try {   //주인의 산책완료버튼
            int walkRecordId = walkMatchingService.end(accessToken, postId);
            Map<String, Object> response = new HashMap<>();
            response.put("walk_record_id", walkRecordId);
            response.put("message", "산책 정상종료, walk_record_id를 이용하여 후기작성을 진행해주세요");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/incomplete/{postId}")
    public ResponseEntity<?> incomplete(@RequestHeader("Authorization") String accessToken,
                                        @PathVariable int postId,
                                        @RequestBody Map<String, Object> requestBody
    ) {
        try {   //주인의 산책미완료버튼   ex) 문제가 발생하여 해당 산책건에 대하여 문제발생버튼을 누름 -> 산책 레코드로 넘김
            String reason = (String) requestBody.get("reason");
            walkMatchingService.incomplete(accessToken, postId, reason);
            return ResponseEntity.ok("산책 문제발생 버튼을 눌렀습니다 고객센터로 연결해주세요");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
