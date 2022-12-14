package com.workduo.group.group.repository;

import com.workduo.group.group.entity.Group;
import com.workduo.group.group.entity.GroupCreateMember;
import com.workduo.group.group.entity.GroupCreateMemberId;
import com.workduo.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCreateMemberRepository extends JpaRepository<GroupCreateMember, GroupCreateMemberId> {
    Long countByMember(Member member);
    boolean existsByMemberAndGroup(Member member, Group group);
    void deleteByMemberAndGroup(Member member, Group group);
}
