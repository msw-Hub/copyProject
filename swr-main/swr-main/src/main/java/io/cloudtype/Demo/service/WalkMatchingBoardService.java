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

    //받을데이터 nickname=owner_nickname,pet_id=choosing_pet, walk_time, walk_date, latitude, longitude, title,content, status=0
    public void writeWalkPost(String owner_nickname, Long choosing_pet, Integer walk_time, Date walk_date,
                              Double latitude, Double longitude, String title, String content) {
        String sql = "INSERT INTO mydb.walk_matching_board (owner_nickname, choosing_pet, walk_time, walk_date," +
                "latitude, longitude, title, content, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, owner_nickname, choosing_pet, walk_time, walk_date, latitude, longitude, title, content, 0);
    }
}
