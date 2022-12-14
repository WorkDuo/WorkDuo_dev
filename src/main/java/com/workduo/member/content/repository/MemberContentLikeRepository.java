package com.workduo.member.content.repository;

import com.workduo.member.content.entity.MemberContent;
import com.workduo.member.content.entity.MemberContentLike;
import com.workduo.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberContentLikeRepository extends JpaRepository<MemberContentLike,Long> {
    @Modifying
    @Query("delete from MemberContentLike mcl where mcl.memberContent = :contentId")
    void deleteAllByMemberContent(@Param("contentId") MemberContent memberContent);

    boolean existsByMemberAndMemberContent(Member m,MemberContent mc);

    @Modifying
    @Query("delete from MemberContentLike mcl where mcl.memberContent = :contentId and mcl.member = :memberId")
    void deleteByMemberAndMemberContent(@Param("memberId")Member m,@Param("contentId") MemberContent mc);
}
