package io.cloudtype.Demo.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalkMatchingBoardService {
    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService communityBoardService;
    @Autowired
    public UserInfoService userInfoService;

    @Autowired
    public WalkMatchingBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                                    @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                                    @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                                    JdbcTemplate jdbcTemplate) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
        this.projectId = projectId;
        this.jdbcTemplate = jdbcTemplate;
    }

    //신청글 작성하기
    //받을데이터 nickname=owner_nickname,pet_id=choosing_pet, walk_time, walk_date, latitude, longitude, title,content, status=0
    public String writeWalkPost(String writerNickname, Long petInfoId, Integer walkTime,
                              double latitude, double longitude, String title, String content) {
        // walk_matching_board 테이블에 writer_nickname으로 검색한 결과중 status가 0 혹은 1인 것이 있는지 확인, 없어야 정상동작
        String checkSql = "SELECT COUNT(*) FROM mydb.walk_matching_board WHERE writer_nickname = ? AND (status = 0 OR status = 1)";
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, writerNickname);
        if (count != 0) {
            return "이미 작성한 신청글이 있습니다. 삭제 후 다시 작성해주세요.";
        }
        String sql = "INSERT INTO mydb.walk_matching_board (writer_nickname, choosing_pet, walk_time,walk_date," +
                "latitude, longitude, title, content, status) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, writerNickname, petInfoId, walkTime, latitude, longitude, title, content, 0);
        return "신청글 작성 완료";
    }
    //신청글 목록 반환하기 (페이지네이션 추가)
    public List<Map<String, Object>> getWalkPostList(int page) {
        // 한 페이지에 표시할 게시글 수
        int pageSize = 10;
        // 페이지 번호에 따라 시작 게시글의 인덱스 계산
        int start = (page - 1) * pageSize;

        //신청글이 아직 매칭이 안된 것만 반환
        String sql = "SELECT * FROM mydb.walk_matching_board WHERE status = 0 ORDER BY walk_date DESC LIMIT ?, ?";
        // SQL 쿼리 실행
        return jdbcTemplate.queryForList(sql, start, pageSize);
    }

    //신청글 거리계산(1/3/5km)한뒤 목록을 반환하는 메서드
    public List<Map<String, Object>> getWalkPostListWithDistance(Integer page, double clientLatitude, double clientLongitude, double maxDistance) {
        List<Map<String, Object>> walkPostList = getWalkPostList(page);
        if (walkPostList.isEmpty()) {
            return walkPostList;
        }
        List<Map<String, Object>> filteredWalkPostList = new ArrayList<>();
        for (Map<String, Object> walkPost : walkPostList) {
            double postLatitude = (double) walkPost.get("latitude");
            double postLongitude = (double) walkPost.get("longitude");
            double distance = calculateDistance(clientLatitude, clientLongitude, postLatitude, postLongitude);
            if (distance <= maxDistance) {
                walkPost.put("distance", distance);
                filteredWalkPostList.add(walkPost);
            }
        }
        return filteredWalkPostList;
    }
    //거리계산 매서드
    public double calculateDistance ( double lat1, double lon1, double lat2, double lon2){
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to km
    }
    //산책요청글 수정하기
    public void updateWalkPost(Long walkMatchingBoardId, Long petInfoId, Integer walkTime,
                               double latitude, double longitude, String title, String content) {
        // waiting_list 테이블에 walk_board_id가 있는지 확인
        String waitingListSql = "SELECT COUNT(*) FROM mydb.waiting_list WHERE walk_board_id = ?";
        int count = jdbcTemplate.queryForObject(waitingListSql, Integer.class, walkMatchingBoardId);
        if (count != 0) {
            throw new RuntimeException("이미 신청한 사람이 있어 수정할 수 없습니다.");
        } else {
            // walk_matching_board 테이블 업데이트
            String updateWalkMatchingBoardSql = "UPDATE mydb.walk_matching_board SET choosing_pet = ?, walk_time = ?, walk_date = CURRENT_TIMESTAMP, latitude = ?, " +
                    "longitude = ?, title = ?, content = ? WHERE walk_matching_board_id = ?";
            jdbcTemplate.update(updateWalkMatchingBoardSql, petInfoId, walkTime, latitude, longitude, title, content, walkMatchingBoardId);
            // delete_queue 테이블의 delete_time 값을 업데이트
            String updateDeleteQueueSql = "UPDATE mydb.delete_queue SET delete_time = ADDTIME(CURRENT_TIMESTAMP, '00:05:00') " +
                    "WHERE walk_matching_board_id = ?";
            jdbcTemplate.update(updateDeleteQueueSql, walkMatchingBoardId);
        }
    }
    //산책요청글 삭제 - 매칭전에 삭제가능
    public void deleteWalkPost(Long walkMatchingBoardId) {
        // waiting_list 테이블에 walk_board_id가 있는지 확인
        String waitingListSql = "SELECT COUNT(*) FROM mydb.waiting_list WHERE walk_board_id = ?";
        int count = jdbcTemplate.queryForObject(waitingListSql, Integer.class, walkMatchingBoardId);
        if(count >0){
            String deleteWaitingListSql = "DELETE FROM mydb.waiting_list WHERE walk_board_id = ?";
            jdbcTemplate.update(deleteWaitingListSql, walkMatchingBoardId);
        }
        //delete_queue 테이블에서 삭제
        String queueSql = "DELETE FROM mydb.delete_queue WHERE walk_matching_board_id = ?";
        jdbcTemplate.update(queueSql, walkMatchingBoardId);
        //walk_matching_board 테이블에서 삭제
        String sql = "DELETE FROM mydb.walk_matching_board WHERE walk_matching_board_id = ?";
        jdbcTemplate.update(sql, walkMatchingBoardId);
    }
    //산책요청글 파트너의 매칭신청
    public String walkApply(Long walkMatchingBoardId, String partnerNickname){
        //해당 글의 작성자가 본인이면 안됌.
        String nicknameSql = "SELECT writer_nickname FROM mydb.walk_matching_board WHERE walk_matching_board_id = ?";
        String nickname = jdbcTemplate.queryForObject(nicknameSql, String.class, walkMatchingBoardId);
        assert nickname != null;
        if(nickname.equals(partnerNickname)){
            return "본인의 글에는 신청할 수 없습니다.";
        }
        //waiting_list 테이블에 waiter_nickname = partnerNickname 이 있는지 확인 >>신청은 1번만 가능(현재 진행중인 매칭이 있어서도 안됌)
        String checkSql = "SELECT COUNT(*) FROM mydb.waiting_list WHERE waiter_nickname = ?";
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, partnerNickname);
        if(count != 0){
            return "이미 신청한 글이 있습니다.";
        }
        //walk_matching_board 테이블에서 walk_matching_board_id로 검색해서 status가 0인 글이 있는지를 확인 >>해당글이 유효한가(유효하면 1반환)
        String walkMatchingBoardSql = "SELECT COUNT(*) FROM mydb.walk_matching_board WHERE walk_matching_board_id = ? AND status = 0";
        int walkMatchingBoardCount = jdbcTemplate.queryForObject(walkMatchingBoardSql, Integer.class, walkMatchingBoardId);
        if(walkMatchingBoardCount == 0){
            return "매칭이 완료된 글이거나 존재하지 않는 글입니다.";
        }
        //walk_matching_board 테이블에서 walk_matching_board_id로 검색해서 writer_nickname을 가져옴
        String writerNicknameSql = "SELECT writer_nickname FROM mydb.walk_matching_board WHERE walk_matching_board_id = ?";
        String writerNickname = jdbcTemplate.queryForObject(writerNicknameSql, String.class, walkMatchingBoardId);
        //waiting_list 테이블에 walk_board_id와 waiter_nickname, writer_nickname을 추가
        String sql = "INSERT INTO mydb.waiting_list (walk_board_id, waiter_nickname, writer_nickname) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, walkMatchingBoardId, partnerNickname, writerNickname);
        return "매칭 신청 완료";
    }
    //매칭전 해당 글이 존재하는 지 + status값이 0인지를 확인
    public boolean checkWalkPost(Long walkMatchingBoardId) {
        try {
            String sql = "SELECT COUNT(*) FROM mydb.walk_matching_board WHERE walk_matching_board_id = ? AND status = 0";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, walkMatchingBoardId);
            return count == 1;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    //신청자 및 신청정보 리스트id로 신청한 사람이 맞는지 확인
    public boolean checkWalkApply(Long walkMatchingBoardId, String partnerNickname) {
        try {
            String sql = "SELECT COUNT(*) FROM mydb.waiting_list WHERE walk_board_id = ? AND waiter_nickname = ?";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, walkMatchingBoardId, partnerNickname);
            return count == 1;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
    //매칭신청수락 walkMatchingBoardId, waiterNickname
    //status=1로 변경, delete_queue에 해당 id의 행을 삭제, walk_matching_board 테이블에서 matching_partner_nickname,matching_date 추가
    public String walkAccept(Long walkMatchingBoardId, String partnerNickname) {
        //앞서서 전부 유효한것을 확인한 상태
        //walk_matching_board 테이블에서 status를 1로 변경하고 matching_partner_nickname, matching_date 추가
        String updateSql = "UPDATE mydb.walk_matching_board SET status = 1, " +
                "matching_partner_nickname = ?, matching_date = CURRENT_TIMESTAMP WHERE walk_matching_board_id = ?";
        jdbcTemplate.update(updateSql, partnerNickname, walkMatchingBoardId);
        //delete_queue 테이블에서 해당 walk_matching_board_id의 행을 삭제
        String deleteQueueSql = "DELETE FROM mydb.delete_queue WHERE walk_matching_board_id = ?";
        jdbcTemplate.update(deleteQueueSql, walkMatchingBoardId);
        return "매칭 수락 완료";
    }
}
