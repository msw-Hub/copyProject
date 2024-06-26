package io.cloudtype.Demo.community.repository;

import io.cloudtype.Demo.community.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    boolean existsByUser_IdAndCommunityBoard_Id(int userId, int communityBoardId);
    boolean existsByUser_IdAndComment_Id(int userId, int commentId);

    void deleteByComment_Id(int commentId);
    void deleteByCommunityBoard_Id(int communityBoardId);
}