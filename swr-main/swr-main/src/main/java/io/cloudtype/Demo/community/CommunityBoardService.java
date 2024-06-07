package io.cloudtype.Demo.community;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.cloudtype.Demo.community.DTO.CommentDTO;
import io.cloudtype.Demo.community.DTO.CommunityBoardDTO;
import io.cloudtype.Demo.community.entity.CommentEntity;
import io.cloudtype.Demo.community.entity.CommunityBoardEntity;
import io.cloudtype.Demo.community.entity.LikeEntity;
import io.cloudtype.Demo.community.repository.CommentRepository;
import io.cloudtype.Demo.community.repository.CommunityBoardRepository;
import io.cloudtype.Demo.community.repository.LikeRepository;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityBoardService {

    private final UserRepository userRepository;
    private final CommunityBoardRepository communityBoardRepository;
    private final LikeRepository likeRepository;
    private final JWTUtil jwtUtil;
    private final CommentRepository commentRepository;

    @Value("${spring.cloud.gcp.storage.credentials.location}")
    private final String keyFileName;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private final String bucketName;

    public CommunityBoardService(@Value("${spring.cloud.gcp.storage.credentials.location}") String keyFileName,
                                 @Value("${spring.cloud.gcp.storage.bucket}") String bucketName,
                                 UserRepository userRepository, JWTUtil jwtUtil, CommunityBoardRepository communityBoardRepository,
                                 CommentRepository commentRepository,
                                 LikeRepository likeRepository) {
        this.keyFileName = keyFileName;
        this.bucketName = bucketName;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.communityBoardRepository = communityBoardRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    @Async("taskExecutor")
    public CompletableFuture<String> uploadImageAsync(MultipartFile image) {
        try {
            // 이미지가 비어 있는지 확인
            if (image.isEmpty()) {
                log.warn("Empty image provided for upload.");
                return CompletableFuture.completedFuture(null);
            }
            byte[] imageBytes = image.getBytes(); // 파일을 ByteArray로 읽음

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
            Blob blob = storage.create(blobInfo, imageBytes); // ByteArray를 사용하여 업로드

            String imgUrl = "https://storage.googleapis.com/" + bucketName + "/" + uuid;
            log.info("Image uploaded successfully. URL: {}", imgUrl);
            return CompletableFuture.completedFuture(imgUrl);
        } catch (IOException e) {
            log.error("Failed to upload image to Google Cloud Storage.", e);
            return CompletableFuture.completedFuture(null);
        }
    }
    // 동기 메서드 유지
    public String uploadImage(MultipartFile image) {
        try {
            if (image.isEmpty()) {
                log.warn("Empty image provided for upload.");
                return null;
            }

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

            String imgUrl = "https://storage.googleapis.com/" + bucketName + "/" + uuid;
            log.info("Image uploaded successfully. URL: {}", imgUrl);
            return imgUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Google Cloud Storage.", e);
            return null;
        }
    }
    public String findImageUrlById(int communityBoardId) {
        return communityBoardRepository.findImageUrlById(communityBoardId);
    }

    public void deleteImageGcs(String imgUrl) {
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

    @Transactional
    public void writePost(String accessToken, String title, String content, String imgUrl) {
        UserEntity user = getUserEntity(accessToken);
        // 새로운 CommunityBoardEntity 인스턴스 생성 및 세부 정보 설정
        CommunityBoardEntity post = new CommunityBoardEntity();
        post.setTitle(title);
        post.setContent(content);
        post.setImgUrl(imgUrl);
        // CommunityBoardEntity에 UserEntity를 연결
        post.setUser(user);  // UserEntity 객체를 직접 연결
        // Repository를 사용해 CommunityBoardEntity를 저장
        communityBoardRepository.save(post);
    }
    public List<CommunityBoardDTO> getCommunityBoardPosts(int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, 10, Sort.by("createDate").descending());
        Page<CommunityBoardEntity> page = communityBoardRepository.findAllByOrderByCreateDateDesc(pageable);

        // 엔티티 객체를 DTO로 변환하여 반환
        return page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    private CommunityBoardDTO mapToDTO(CommunityBoardEntity entity) {
        CommunityBoardDTO dto = new CommunityBoardDTO();
        dto.setId(entity.getId());
        dto.setNickname(entity.getUser().getNickname());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setCreateDate(entity.getCreateDate());
        dto.setImgUrl(entity.getImgUrl());
        dto.setLikeCount(entity.getLikeCount());
        dto.setCommentCount(entity.getCommentCount());
        dto.setViewCount(entity.getViewCount());
        return dto;
    }
    public List<CommunityBoardDTO> getPost(int communityBoardId) {
        // 게시글 ID를 사용하여 해당 게시글을 찾습니다.
        Optional<CommunityBoardEntity> postOptional = Optional.ofNullable(communityBoardRepository.findById(communityBoardId));
        // 옵셔널에서 게시글을 가져오거나 게시글이 없는 경우 예외를 던집니다.
        CommunityBoardEntity post = postOptional.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + communityBoardId));
        // 조회수 증가
        post.setViewCount(post.getViewCount() + 1);
        communityBoardRepository.save(post);
        // 게시글을 DTO로 변환하여 반환
        return List.of(mapToDTO(post));
    }
    public List<CommentDTO> getComments(int communityBoardId) {
        List<CommentEntity> comments = commentRepository.findAllByCommunityBoard_Id(communityBoardId);
        return comments.stream()
                .map(this::mapToDTO2)
                .collect(Collectors.toList());
    }
    private CommentDTO mapToDTO2(CommentEntity entity) {
        CommentDTO dto = new CommentDTO();
        dto.setId(entity.getId());
        dto.setNickname(entity.getUser().getNickname());
        dto.setContent(entity.getContent());
        dto.setCreateDate(entity.getCreateDate());
        dto.setLikeCount(entity.getLikeCount());
        return dto;
    }

    @Transactional
    public void writeComment(String accessToken, int communityBoardId, String content) {
        UserEntity user = getUserEntity(accessToken);

        Optional<CommunityBoardEntity> board = Optional.ofNullable(communityBoardRepository.findById(communityBoardId));
        // 옵셔널에서 게시글을 가져오거나 게시글이 없는 경우 예외를 던집니다.
        CommunityBoardEntity post = board.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다 ID: " +communityBoardId));

        // 게시글의 댓글 수 증가
        post.setCommentCount(post.getCommentCount() + 1);
        communityBoardRepository.save(post);

        //댓글 저장
        CommentEntity comment = new CommentEntity();
        comment.setUser(user);
        comment.setCommunityBoard(post);
        comment.setContent(content);
        commentRepository.save(comment);
    }

    @Transactional
    public void removeComment(String accessToken, int commentId) {
        UserEntity user = getUserEntity(accessToken);
        // 댓글 ID를 사용하여 해당 댓글을 찾습니다.
        Optional<CommentEntity> commentOptional = Optional.ofNullable(commentRepository.findById(commentId));
        // 옵셔널에서 댓글을 가져오거나 댓글이 없는 경우 예외
        CommentEntity comment = commentOptional.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다 ID: " + commentId));
        // 자신만이 삭제가능
        if (comment.getUser().getId() != user.getId()) {
            throw new IllegalStateException("본인 댓글만 삭제할 수 있습니다");
        }
        // 댓글 삭제하기 전에 좋아요를 누른 사용자들을 목록에서 삭제
        likeRepository.deleteByComment_Id(commentId);
        // 게시글의 댓글 수 감소
        CommunityBoardEntity post = comment.getCommunityBoard();
        post.setCommentCount(post.getCommentCount() - 1);
        communityBoardRepository.save(post);
        // 댓글 삭제
        commentRepository.delete(comment);
    }
    @Transactional
    public void removePost(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        // 게시글 ID를 사용하여 해당 게시글을 찾습니다.
        Optional<CommunityBoardEntity> postOptional = Optional.ofNullable(communityBoardRepository.findById(postId));
        // 옵셔널에서 게시글을 가져오거나 게시글이 없는 경우 예외를 던집니다.
        CommunityBoardEntity post = postOptional.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다 ID: " + postId));
        // 자신만이 삭제가능
        if (post.getUser().getId() != user.getId()) {
            throw new IllegalStateException("본인 게시글만 삭제할 수 있습니다");
        }
        // 게시글 삭제하기 전에 해당 게시글의 좋아요를 누른 사용자들을 목록에서 삭제
        likeRepository.deleteByCommunityBoard_Id(postId);
        // 게시글에 있는 모든 댓글 삭제
        List<CommentEntity> comments = commentRepository.findByCommunityBoard_Id(postId);
        comments.forEach(comment -> removeCommentForPost(comment.getId()));
        // 게시글 삭제
        communityBoardRepository.delete(post);
    }
    public void removeCommentForPost(int commentId) {
        // 댓글 ID를 사용하여 해당 댓글을 찾습니다.
        Optional<CommentEntity> commentOptional = Optional.ofNullable(commentRepository.findById(commentId));
        // 옵셔널에서 댓글을 가져오거나 댓글이 없는 경우 예외
        CommentEntity comment = commentOptional.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다 ID: " + commentId));
        // 댓글 삭제하기 전에 좋아요를 누른 사용자들을 목록에서 삭제
        likeRepository.deleteByComment_Id(commentId);
        // 게시글의 댓글 수 감소
        CommunityBoardEntity post = comment.getCommunityBoard();
        post.setCommentCount(post.getCommentCount() - 1);
        communityBoardRepository.save(post);
        // 댓글 삭제
        commentRepository.delete(comment);
    }

    @Transactional
    public void likePost(String accessToken, int postId) {
        UserEntity user = getUserEntity(accessToken);
        // 게시글 ID를 사용하여 해당 게시글을 찾습니다.
        Optional<CommunityBoardEntity> postOptional = Optional.ofNullable(communityBoardRepository.findById(postId));
        // 옵셔널에서 게시글을 가져오거나 게시글이 없는 경우 예외를 던집니다.
        CommunityBoardEntity post = postOptional.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다 ID: " + postId));
        // 자추 안됌
        if (post.getUser().getId() == user.getId()) {
            throw new IllegalStateException("본인 게시글에는 좋아요를 누를 수 없습니다");
        }
        // 좋아요를 누른 사용자 목록에 사용자가 이미 있는지 확인, user.getId()와 post.getId()를 이용하여 LikeEntity를 조회
        boolean liked = likeRepository.existsByUser_IdAndCommunityBoard_Id(user.getId(), post.getId());
        if (liked) {
            throw new IllegalStateException("이미 좋아요를 누른 게시글입니다");
        }
        // 좋아요를 누른 사용자 목록에 사용자 추가
        LikeEntity like = new LikeEntity();
        like.setUser(user);
        like.setCommunityBoard(post);
        likeRepository.save(like);
        // 게시글의 좋아요 수 증가
        post.setLikeCount(post.getLikeCount() + 1);
        communityBoardRepository.save(post);
    }
    @Transactional
    public void likeComment(String accessToken, int commentId){
        UserEntity user = getUserEntity(accessToken);
        // 댓글 ID를 사용하여 해당 댓글을 찾습니다.
        Optional<CommentEntity> commentOptional = Optional.ofNullable(commentRepository.findById(commentId));
        // 옵셔널에서 댓글을 가져오거나 댓글이 없는 경우 예외
        CommentEntity comment = commentOptional.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다 ID: " + commentId));
        // 자신의 댓글에는 좋아요를 누를 수 없음
        if (comment.getUser().getId() == user.getId()) {
            throw new IllegalStateException("본인 댓글에는 좋아요를 누를 수 없습니다");
        }
        // 좋아요를 누른 사용자 목록에 사용자가 이미 있는지 확인
        boolean liked = likeRepository.existsByUser_IdAndComment_Id(user.getId(), comment.getId());
        if (liked) {
            throw new IllegalStateException("이미 좋아요를 누른 댓글입니다");
        }
        // 좋아요를 누른 사용자 목록에 사용자 추가
        LikeEntity like = new LikeEntity();
        like.setUser(user);
        like.setComment(comment);
        likeRepository.save(like);
        //댓글의 좋아요 수 증가
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }
    @Transactional
    public void editPost(String accessToken, int postId, String title, String content, String imgUrl) {
        UserEntity user = getUserEntity(accessToken);
        // 게시글 ID를 사용하여 해당 게시글을 찾습니다.
        Optional<CommunityBoardEntity> postOptional = Optional.ofNullable(communityBoardRepository.findById(postId));
        // 옵셔널에서 게시글을 가져오거나 게시글이 없는 경우 예외를 던집니다.
        CommunityBoardEntity post = postOptional.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다 ID: " + postId));
        // 자신만이 수정가능
        if (post.getUser().getId() != user.getId()) {
            throw new IllegalStateException("본인 게시글만 수정할 수 있습니다");
        }
        // 게시글 수정
        post.setTitle(title);
        post.setContent(content);
        post.setImgUrl(imgUrl);
        communityBoardRepository.save(post);
    }
    @Transactional
    public void editComment(String accessToken, int commentId, String content) {
        UserEntity user = getUserEntity(accessToken);
        // 댓글 ID를 사용하여 해당 댓글을 찾습니다.
        Optional<CommentEntity> commentOptional = Optional.ofNullable(commentRepository.findById(commentId));
        // 옵셔널에서 댓글을 가져오거나 댓글이 없는 경우 예외
        CommentEntity comment = commentOptional.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다 ID: " + commentId));
        // 자신만이 수정가능
        if (comment.getUser().getId() != user.getId()) {
            throw new IllegalStateException("본인 댓글만 수정할 수 있습니다");
        }
        // 댓글 수정
        comment.setContent(content);
        commentRepository.save(comment);
    }
    private UserEntity getUserEntity(String accessToken){
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}