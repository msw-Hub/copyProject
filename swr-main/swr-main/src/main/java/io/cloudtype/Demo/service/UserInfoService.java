package io.cloudtype.Demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Year;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserInfoService {

    private final JdbcTemplate jdbcTemplate;
    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private final String projectId;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    @Autowired
    public CommunityBoardService communityBoardService;

    // 생성자를 통한 주입
    @Autowired
    public UserInfoService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                @Value("${spring.cloud.gcp.storage.project-id}") String projectId,
                JdbcTemplate jdbcTemplate) {
            this.keyFileName = keyFileName;
            this.bucketName = bucketName;
            this.projectId = projectId;
            this.jdbcTemplate = jdbcTemplate;
    }

    //유저 고유id를 통해서 유저정보를 가져오는 메서드
    public Map<String, Object> getUserInfoById(Long userId) {
        String sql = "SELECT * FROM mydb.user_info WHERE user_id = ?";
        return jdbcTemplate.queryForMap(sql, userId);
    }

    //db에 유저정보 저장하는 메서드
    public void saveUserInfo(Map<String, Object> userInfo) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String sql = "INSERT INTO mydb.user_info (user_id, nickname, profile_image, email, name, gender, age_range) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                (Long) userInfo.get("userId"), // userId를 Long으로 캐스팅
                userInfo.get("nickname"),
                userInfo.get("profileImage"),
                userInfo.get("email"),
                userInfo.get("name"),
                userInfo.get("gender"),
                userInfo.get("ageRange")
        );
    }

    // 회원가입 시 추가 정보를 저장하는 메서드
    public void saveAdditionalUserInfo(Long userId, String nickName , String phoneNumber, String pinNumber, String birthday) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }
        String updateSql = "UPDATE mydb.user_info SET nickname=?, phone_number = ?, pin_number = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(updateSql, nickName ,phoneNumber, pinNumber, birthday, userId);
    }

    //개인정보 수정시 pin번호를 조회하는 메서드
    public String getPinNumberByUserId(Long userId) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }

        // 데이터베이스에서 userId에 해당하는 사용자의 핀 번호를 조회하는 쿼리 작성
        String sql = "SELECT pin_number FROM mydb.user_info WHERE user_id = ?";

        // 쿼리 실행하여 결과 가져오기
        return jdbcTemplate.queryForObject(sql, String.class, userId);
    }

    //글 삭제시, 게시글아이디로 작성자 확인하는 메서드
    public String getUserIdByCommunityBoardNickname(Long communityBoardId) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate이 올바르게 주입되지 않았습니다.");
        }

        // 데이터베이스에서 userId에 해당하는 사용자의 핀 번호를 조회하는 쿼리 작성
        String sql = "SELECT writer_nickname FROM mydb.community_board WHERE community_board_id = ?";

        // 쿼리 실행하여 결과 가져오기
        return jdbcTemplate.queryForObject(sql, String.class, communityBoardId);
    }

    public void updateUserInfo(Long userId, String column, String value, String nowNickname) {
        switch (column) {
            case "nickname" -> {
                String sqlUserInfo = "UPDATE user_info SET nickname = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlUserInfo, value, userId);

                String sqlCommunityBoard = "UPDATE community_board SET writer_nickname = ? WHERE writer_nickname = ?";
                jdbcTemplate.update(sqlCommunityBoard, value, nowNickname);

                String sqlCommunityReply = "UPDATE community_reply SET writer_nickname = ? WHERE writer_nickname = ?";
                jdbcTemplate.update(sqlCommunityReply, value, nowNickname);

                String sqlLike = "UPDATE likes SET likes_nickname = ? WHERE likes_nickname = ?";
                jdbcTemplate.update(sqlLike, value, nowNickname);
            }
            case "pin_number" -> {
                String sqlPinNumber = "UPDATE user_info SET pin_number = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlPinNumber, value, userId);
            }
            case "phone_number" -> {
                String sqlPhoneNumber = "UPDATE user_info SET phone_number = ? WHERE user_id = ?";
                jdbcTemplate.update(sqlPhoneNumber, value, userId);
            }
            default -> throw new IllegalArgumentException("유효하지 않은 column입니다: " + column);
        }
    }
    public List<String> getAllNicknames() {
        String sql = "SELECT nickname FROM mydb.user_info";
        return jdbcTemplate.queryForList(sql, String.class);
    }
    public Long savePetInfo1(String petName, Integer petAge, String species, String imageUrl, String ownerNickname, boolean insertOrUpdate, Long petId){
        int currentYear = Year.now().getValue();
        int birthYear = currentYear - petAge;
        if(insertOrUpdate){
            String sql = "INSERT INTO mydb.pet_info (owner_nickname, pet_profile_image, pet_name, birth_year, species) " + "VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, ownerNickname, imageUrl ,petName, birthYear, species);
            // 삽입된 행의 auto-increment 칼럼의 값을 가져옴
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        else{
            String sql = "UPDATE mydb.pet_info SET pet_profile_image = ?, pet_name = ?, birth_year = ?, species = ? WHERE pet_info_id = ?";
            jdbcTemplate.update(sql, imageUrl, petName, birthYear, species, petId);
            return petId;
        }
    }
    public String savePetInfo2(String ownerNickname, Map<String, Object> petInfo){
        //petInfo로 받은 petId로 ownerNickname을 찾아서 인자로 받은 ownerNickname과 비교
        String sql = "SELECT owner_nickname FROM mydb.pet_info WHERE pet_info_id = ?";
        String ownerNicknameFromDB = jdbcTemplate.queryForObject(sql, String.class, petInfo.get("pet_info_id"));
        //ownerNickname과 ownerNicknameFromDB가 같으면 petInfo를 업데이트
        if(ownerNickname.equals(ownerNicknameFromDB)){
            String updateSql = "UPDATE mydb.pet_info SET weight=?, neutering = ?, animal_hospital=?,vaccination=?,etc=? WHERE pet_info_id = ?";
            jdbcTemplate.update(updateSql, petInfo.get("weight"), petInfo.get("neutering"),
                    petInfo.get("animal_hospital"), petInfo.get("vaccination"), petInfo.get("etc"), petInfo.get("pet_info_id"));
            return "success";
        }else {
            return "해당 pet_info_id의 주인이 아닙니다.";
        }
    }
    public Map<String, Object> petInfoList(String ownerNickname) {
        try {
            String sql = "SELECT * FROM mydb.pet_info WHERE owner_nickname = ?";
            return jdbcTemplate.queryForMap(sql, ownerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 Map 반환
            return new HashMap<>();
        }
    }
    public  String getOwnerNicknameByPetId(Long petInfoId){
        String sql = "SELECT owner_nickname FROM mydb.pet_info WHERE pet_info_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, petInfoId);
    }
    public String getImageUrlByPetId(Long petInfoId){
        String sql = "SELECT pet_profile_image FROM mydb.pet_info WHERE pet_info_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, petInfoId);
    }
    public void deletePetInfo(Long petInfoId, String imageUrl){
        communityBoardService.deleteImageGcs(imageUrl);
        String sql = "DELETE FROM mydb.pet_info WHERE pet_info_id = ?";
        jdbcTemplate.update(sql, petInfoId);
    }
    public Map<String, Object> getPetInfoByOwnerNickname(String ownerNickname) {
        try {
            String sql = "SELECT pet_info_id, pet_profile_image, pet_name FROM mydb.pet_info WHERE owner_nickname = ?";
            return jdbcTemplate.queryForMap(sql, ownerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 Map 반환
            return new HashMap<>();
        }
    }
    //글작성자임을 확인하는 메서드
    public  boolean checkWriter(Long walkMatchingBoardId, String ownerNickname){
        //walkMatchingBoardId로 ownerNickname을 찾아서 인자로 받은 ownerNickname과 비교
        try{
            String sql = "SELECT writer_nickname FROM mydb.walk_matching_board WHERE walk_matching_board_id = ?";
            String writerNickname = jdbcTemplate.queryForObject(sql, String.class, walkMatchingBoardId);
            return ownerNickname.equals(writerNickname);
        }catch (EmptyResultDataAccessException e){
            return false;
        }
    }
    //매칭이 되지않은 status=0인 신청글 반환(0~1개뿐임)
    public List<Map<String, Object>> getWalkPostByNickname2(String writerNickname) {
        try {
            String sql = "SELECT * FROM mydb.walk_matching_board WHERE writer_nickname = ? AND status = 2";
                return jdbcTemplate.queryForList(sql, writerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 리스트반환
            return new ArrayList<>();
        }
    }
    //매칭상태확인하고 해당글 반환
    public Map<String, Object> getWalkPostByNickname(String writerNickname, Integer walkPost) {
        try {
            //매칭전 글
            if(walkPost ==0){
                String sql = "SELECT * FROM mydb.walk_matching_board WHERE writer_nickname = ? AND status = 0";
                return jdbcTemplate.queryForMap(sql, writerNickname);
            } else if (walkPost == 1){
                //매칭완료 후 진행중인 글
                String sql = "SELECT * FROM mydb.walk_matching_board WHERE writer_nickname = ? AND status = 1";
                return jdbcTemplate.queryForMap(sql, writerNickname);
            }else {
                return new HashMap<>();
            }
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈
            return new HashMap<>();
        }
    }

    //매칭전 글 반환시, 매칭신청자들도 반환 (작성자 관점에서 리스트반환) >>나중에 돌봄은 인자 받아서 구분
    public List<Map<String, Object>> getWaitListByNickname(String writerNickname) {
        try {
            String sql = "SELECT waiting_list_id, waiter_nickname, rating FROM mydb.waiting_list WHERE writer_nickname = ?";
            return jdbcTemplate.queryForList(sql, writerNickname);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 리스트반환
            return new ArrayList<>();
        }
    }
    //신청한 사람 닉네임으로 신청한글 정보조회 0~1개뿐임 >>나중에 돌봄은 인자 받아서 구분
    public Map<String, Object> getWaitListByWaiterNickname(String waiterNickname) {
        try {
            //waiterNickname으로 waiting_list테이블에서 글id를 찾아서 walk_matching_board테이블에서 해당글을 찾아옴
            String sql = "SELECT walk_board_id FROM mydb.waiting_list WHERE waiter_nickname = ?";
            Long walkBoardId = jdbcTemplate.queryForObject(sql, Long.class, waiterNickname);
            //해당 id로 글정보 조회, 만일 status가 0이면 매칭전 글, 1이면 매칭완료 후 진행중인 글
            String sql2 = "SELECT * FROM mydb.walk_matching_board WHERE walk_matching_board_id = ?";
            return jdbcTemplate.queryForMap(sql2, walkBoardId);
        } catch (EmptyResultDataAccessException e) {
            // 검색 결과가 없을 경우 빈 리스트반환
            return new HashMap<>();
        }
    }
    //petInfoId로 petInfo 테이블에서 해당 행의 정보가 모두 기입되었는지 확인하는 매서드
    public boolean checkPetInfo(Long petInfoId) {
        try {
            String sql = "SELECT * FROM mydb.pet_info WHERE pet_id = ?";
            Map<String, Object> petInfo = jdbcTemplate.queryForMap(sql, petInfoId);

            // 각 열의 값을 확인하여 null이 없는지 확인
            for (Map.Entry<String, Object> entry : petInfo.entrySet()) {
                // etc 열은 확인하지 않음
                if (!entry.getKey().equals("etc") && entry.getValue() == null) {
                    log.info("null 값이 있습니다: " + entry.getKey());
                    return false; // null 값이 하나라도 있으면 false 반환
                }
            }
            return true; // 모든 열에 값이 있으면 true 반환
        } catch (EmptyResultDataAccessException e) {
            return false; // 해당하는 행이 없는 경우
        }
    }
}
