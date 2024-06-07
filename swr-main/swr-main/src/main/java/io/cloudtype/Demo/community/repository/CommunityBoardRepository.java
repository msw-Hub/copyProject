package io.cloudtype.Demo.community.repository;

import io.cloudtype.Demo.community.entity.CommunityBoardEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityBoardRepository extends JpaRepository<CommunityBoardEntity, Long> {
    //최신글 기준 인덱스 나열해서 시작게시글로부터 10개 반환
    Page<CommunityBoardEntity> findAllByOrderByCreateDateDesc(Pageable pageable);
    //게시글 번호로 게시글 조회
    CommunityBoardEntity findById(int boardId);
    //게시글 번호로 게시글 삭제
    void delete(@NotNull CommunityBoardEntity communityBoardEntity);
    //게시글 번호로 imageUrl 조회
    @Query("SELECT c.imgUrl FROM CommunityBoardEntity c WHERE c.id = :id")
    String findImageUrlById(int id);

    List<CommunityBoardEntity> findByTitle(String keywordTitle);
}
