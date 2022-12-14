package com.workduo.group.group.repository;

import com.workduo.group.group.entity.Group;
import com.workduo.group.group.entity.GroupJoinMember;
import com.workduo.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinMemberRepository extends JpaRepository<GroupJoinMember, Long> {

    @Modifying
    @Query("update GroupJoinMember gjm " +
            "set gjm.groupJoinMemberStatus = 'GROUP_JOIN_MEMBER_STATUS_LEADER_WITHDRAW', " +
            "gjm.deletedAt = current_timestamp " +
            "where gjm.group = :group")
    void updateGroupJoinMemberStatusCancel(@Param("group") Group group);
    Optional<GroupJoinMember> findByMemberAndGroup(Member member, Group group);
    boolean existsByGroupAndMember(Group group, Member member);
    Integer countByGroup(Group group);
    List<GroupJoinMember> findAllByMember(Member m);
}
