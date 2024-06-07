package io.cloudtype.Demo.usage;

import io.cloudtype.Demo.care.DTO.CareUsageDetailsDTO;
import io.cloudtype.Demo.care.DTO.CareUsageSimpleDTO;
import io.cloudtype.Demo.walk.DTO.WalkUsageDetailsDTO;
import io.cloudtype.Demo.walk.DTO.WalkUsageSimpleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/usageDetails")
public class UsageDetailsController {
    private final UsageDetailsService usageDetailsService;
    @Autowired
    public UsageDetailsController(UsageDetailsService usageDetailsService) {
        this.usageDetailsService = usageDetailsService;
    }
    //유저의 산책이용내역보기
    @GetMapping("/walk/list")
    public ResponseEntity<?> getUserWalkList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkUsageSimpleDTO> walkUsageList = usageDetailsService.getUserWalkUsageList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkUsageList", walkUsageList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 산책이용상세내역보기 (파트너도 공용으로 사용)
    @PostMapping("/walk/detail")
    public ResponseEntity<?> getUserWalkDetail(@RequestHeader("Authorization") String accessToken,
                                               @RequestParam("walk_recode_id") int walkRecodeId
    ) {
        try {
            WalkUsageDetailsDTO walkUsage = usageDetailsService.getUserWalkUsageDetail(accessToken, walkRecodeId);
            return ResponseEntity.ok(walkUsage);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너의 산책이용내역보기
    @GetMapping("/walk/partner/list")
    public ResponseEntity<?> getPartnerWalkList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<WalkUsageSimpleDTO> walkUsageList = usageDetailsService.getPartnerWalkUsageList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("walkUsageList", walkUsageList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저의 돌봄 이용내역보기
    @GetMapping("/care/list")
    public ResponseEntity<?> getUserCareList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<CareUsageSimpleDTO> careUsageList = usageDetailsService.getUserCareUsageList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("careUsageList", careUsageList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //유저와 파트너의 돌봄 이용내역 상세보기
    @GetMapping("/care/detail")
    public ResponseEntity<?> geCareUsageDetail(@RequestHeader("Authorization") String accessToken,
                                              @RequestParam("care_record_id") int careRecordId
    ) {
        try {
            CareUsageDetailsDTO careUsage = usageDetailsService.getUserCareUsageDetail(accessToken, careRecordId);
            return ResponseEntity.ok(careUsage);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //파트너의 돌봄 이용내역보기
    @GetMapping("/care/partner/list")
    public ResponseEntity<?> getPartnerCareList(@RequestHeader("Authorization") String accessToken) {
        try {
            List<CareUsageSimpleDTO> careUsageList = usageDetailsService.getPartnerCareUsageList(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("careUsageList", careUsageList);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
