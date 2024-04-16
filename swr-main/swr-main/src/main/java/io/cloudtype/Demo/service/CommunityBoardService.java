package io.cloudtype.Demo.service;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityBoardService {

    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                                 @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                                 @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                                 JdbcTemplate jdbcTemplate) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
        this.projectId = projectId;
        this.jdbcTemplate = jdbcTemplate;
    }


    public Map<String, Object> getCommunityBoardPosts(int page) {
        // 한 페이지에 표시할 게시글 수
        int pageSize = 10;
        // 페이지 번호에 따라 시작 게시글의 인덱스 계산
        int start = (page - 1) * pageSize;
        // SQL 쿼리 생성
        String sql = "SELECT * FROM community_board ORDER BY created_date DESC LIMIT ?, ?";
        // SQL 쿼리 실행
        try {
            List<Map<String, Object>> lists = jdbcTemplate.queryForList(sql, start, pageSize);

            // 결과 맵 생성
            Map<String, Object> result = new HashMap<>();
            result.put("posts", lists);
            result.put("page", page);
            result.put("pageSize", pageSize);

            return result;
        } catch (Exception e) {
            log.error("Failed to fetch community board posts", e);
            return null;
        }
    }
    public void writePost(String nickname, String title, String content, String imgUrl){
        // 게시글 작성 SQL 쿼리 실행
        String sql = "INSERT INTO community_board (writer_nickname, title, content, created_date, img_url) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";
        jdbcTemplate.update(sql,nickname, title, content, imgUrl);
    }
    public String uploadImage(MultipartFile image) {
        try {
            // 이미지가 비어 있는지 확인
            if (image.isEmpty()) {
                log.warn("Empty image provided for upload.");
                return null;
            }

            // Google Cloud Storage에 이미지 업로드
            InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
            String uuid = UUID.randomUUID().toString();
            String ext = StringUtils.getFilenameExtension(image.getOriginalFilename());
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build()
                    .getService();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, uuid)
                    .setContentType(ext)
                    .build();
            Blob blob = storage.create(blobInfo, image.getBytes());

            // 업로드된 이미지의 URL 반환
            String imgUrl = "https://storage.googleapis.com/" + bucketName + "/" + uuid;

            // 성공 시 정보 로그 작성
            log.info("Image uploaded successfully. URL: {}", imgUrl);
            return imgUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Google Cloud Storage.", e);
            return null;
        }
    }
    public List<Map<String, Object>> getCommentsForPost(Long postId) {
        String sql = "SELECT * FROM community_reply WHERE community_board_id = ?";
        return jdbcTemplate.queryForList(sql, postId);
    }
    public void deleteCommunityBoard(Long communityBoardId) {
        // 게시글 id를 토대로 이미지 URL을 가져옴
        String sql = "SELECT img_url FROM community_board WHERE community_board_id = ?";
        String imgUrl = jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
        if (imgUrl != null && !imgUrl.isEmpty()) {
            deleteImageGcs(imgUrl);
        }

        // 게시글에 연결된 모든 댓글 ID 찾기
        String findRepliesSql = "SELECT community_reply_id FROM community_reply WHERE community_board_id = ?";
        List<Long> replyIds = jdbcTemplate.queryForList(findRepliesSql, new Object[]{communityBoardId}, Long.class);

        if (!replyIds.isEmpty()) {
            // 모든 댓글 ID를 쉼표로 구분된 문자열로 변환
            String replyIdsStr = replyIds.stream().map(Object::toString).collect(Collectors.joining(","));

            // 모든 댓글에 연결된 좋아요 삭제
            String deleteLikesSql = "DELETE FROM likes WHERE likes_reply_id IN (" + replyIdsStr + ")";
            jdbcTemplate.update(deleteLikesSql);

            // 모든 댓글 삭제
            String deleteRepliesSql = "DELETE FROM community_reply WHERE community_reply_id IN (" + replyIdsStr + ")";
            jdbcTemplate.update(deleteRepliesSql);
        }

        // 게시글에 연결된 좋아요 삭제
        String deleteLikesSql2 = "DELETE FROM likes WHERE likes_board_id = ?";
        jdbcTemplate.update(deleteLikesSql2, communityBoardId);

        // 게시글 삭제
        String deleteSql = "DELETE FROM community_board WHERE community_board_id = ?";
        jdbcTemplate.update(deleteSql, communityBoardId);
    }
    public void deleteImageGcs(String imgUrl){
        // imgUrl에서 파일 이름(객체 이름)을 추출
        String objectName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
        log.info("Object name: {}", objectName);
        try {
            // 인증 정보를 사용하여 Storage 객체 생성
            InputStream keyFile = ResourceUtils.getURL(keyFileName).openStream();
            Storage storage = StorageOptions.newBuilder().setCredentials(GoogleCredentials.fromStream(keyFile)).build().getService();

            // GCS에서 이미지 파일(객체) 삭제
            Blob blob = storage.get(bucketName, objectName);
            if (blob != null) {
                blob.delete();
                log.info("Object {} was deleted from {}", objectName, bucketName);
            } else {
                log.info("The object {} wasn't found in {}", objectName, bucketName);
            }
        } catch (FileNotFoundException e) {
            log.error("Key file for GCP credentials not found: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Error occurred while accessing GCP credentials: {}", e.getMessage());
        }
    }
    public void deleteReply(Long communityReplyId) {
        //댓글과 관련된 좋아요 먼저 삭제
        String likesSql = "DELETE FROM likes WHERE likes_reply_id = ?";
        jdbcTemplate.update(likesSql, communityReplyId);
        //댓글로 해당 게시글 id를 찾아둠
        String sql2 = "SELECT community_board_id FROM community_reply WHERE community_reply_id = ?";
        Long communityBoardId = jdbcTemplate.queryForObject(sql2, Long.class, communityReplyId);
        //댓글 삭제
        String sql = "DELETE FROM community_reply WHERE community_reply_id = ?";
        jdbcTemplate.update(sql, communityReplyId);
        //댓글을 삭제했으니, 해당 게시글의 댓글수를 감소시킴
        String sql3 = "UPDATE community_board SET replies = replies - 1 WHERE community_board_id = ?";
        jdbcTemplate.update(sql3, communityBoardId);
    }
    public void editCommunityBoard(Long communityBoardId,  String title, String content, MultipartFile image, Boolean imageChangeCheck) {
        String sql = "SELECT img_url FROM community_board WHERE community_board_id = ?";
        String imgUrl = jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
        if( imageChangeCheck && image==null ){  //게시글의 이미지 삭제
            if(imgUrl == null) log.error("Failed to fetch image URL from community board post"); //기존 url이 없었는데 삭제한다니 오류
            else deleteImageGcs(imgUrl);
            String updateSql = "UPDATE community_board SET title = ?, content = ?, img_url = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, null, communityBoardId);
        } else if (imageChangeCheck) {  //게시글의 이미지 변경 (1.gcs이미지 삭제 후 변경이미지 업로드)
            if(imgUrl != null) deleteImageGcs(imgUrl);
            String newImgUrl = uploadImage(image);
            String updateSql = "UPDATE community_board SET title = ?, content = ?, img_url = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, newImgUrl, communityBoardId);
        } else {   //게시글의 이미지 변경 없이 수정
            String updateSql = "UPDATE community_board SET title = ?, content = ? WHERE community_board_id = ?";
            jdbcTemplate.update(updateSql, title, content, communityBoardId);
        }
    }
    public void writeReply(Long communityBoardId, String nickname, String content) {
        String sql = "INSERT INTO community_reply (community_board_id,reply_content,reply_date,writer_nickname) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
        jdbcTemplate.update(sql, communityBoardId,content, nickname);
        //해당글의 댓글수 증가
        String sql2 = "UPDATE community_board SET replies = replies + 1 WHERE community_board_id = ?";
        jdbcTemplate.update(sql2, communityBoardId);
    }
    public void plusViews(Long communityBoardId) {
        String sql = "UPDATE community_board SET views = views + 1 WHERE community_board_id = ?";
        jdbcTemplate.update(sql, communityBoardId);
    }
    public String plusBoardLikes(Long communityBoardId, String nickname) {
        //좋아요를 이미 누르면 안되도록 중복체크
        String sql = "SELECT likes_nickname FROM likes WHERE likes_board_id = ?";
        List<String> nicknames = jdbcTemplate.queryForList(sql, String.class, communityBoardId);

        if (!nicknames.contains(nickname)) {
            //communityBoardId로 community_board 테이블에서 community_board_id가 존재하는지를 확인하고 진행
            String sql3 = "SELECT community_board_id FROM community_board WHERE community_board_id = ?";
            Long communityBoardId2 = jdbcTemplate.queryForObject(sql3, Long.class, communityBoardId);
            if(communityBoardId2 == null) {
                log.error("유효한 community_board_id가 아닙니다.");
                return "유효한 community_board_id가 아닙니다.";
            }
            else {
                String sql2 = "INSERT INTO mydb.likes (likes_nickname, likes_board_id)" + "VALUES (?, ?)";
                jdbcTemplate.update(sql2, nickname, communityBoardId);

                String updateSql = "UPDATE community_board SET likes = likes + 1 WHERE community_board_id = ?";
                jdbcTemplate.update(updateSql, communityBoardId);
                return "success";
            }
        }else return "이미 좋아요를 눌렀습니다";
    }
    public String plusReplyLikes(Long communityReplyId, String nickname) {
        //좋아요를 이미 누르면 안되도록 중복체크
        String sql = "SELECT likes_nickname FROM likes WHERE likes_reply_id = ?";
        List<String> nicknames = jdbcTemplate.queryForList(sql, String.class, communityReplyId);

        if (!nicknames.contains(nickname)) {
            //communityReplyId로 community_reply 테이블에서 community_reply_id가 존재하는지를 확인하고 진행
            String sql3 = "SELECT community_reply_id FROM community_reply WHERE community_reply_id = ?";
            Long communityReplyId2 = jdbcTemplate.queryForObject(sql3, Long.class, communityReplyId);
            if(communityReplyId2 == null) {
                log.error("유효한 community_reply_id가 아닙니다.");
                return "유효한 community_reply_id가 아닙니다.";
            }
            else {
                String sql2 = "INSERT INTO mydb.likes (likes_nickname, likes_reply_id)" + "VALUES (?, ?)";
                jdbcTemplate.update(sql2, nickname, communityReplyId);
                String updateSql = "UPDATE community_reply SET likes = likes + 1 WHERE community_reply_id = ?";
                jdbcTemplate.update(updateSql, communityReplyId);
                return "success";
            }
        }else {
            return "이미 좋아요를 눌렀습니다";
        }
    }
    public void deleteReplyBeforeCheck(Long communityReplyId, String nickname) {
        String sql = "SELECT writer_nickname FROM mydb.community_reply WHERE community_reply_id = ?";
        String writerNickname =  jdbcTemplate.queryForObject(sql, String.class, communityReplyId);
        if(nickname.equals(writerNickname)) deleteReply(communityReplyId);
        else log.error("You are not the writer of this reply.");
    }
}