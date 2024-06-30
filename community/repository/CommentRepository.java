package io.cloudtype.Demo.community.repository;

import io.cloudtype.Demo.community.entity.CommentEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    CommentEntity findById(int id);
    List<CommentEntity> findAllByCommunityBoard_Id(int communityBoardId);

    void delete(@NotNull CommentEntity commentEntity);
    List<CommentEntity> findByCommunityBoard_Id(int postId);
}
