package com.workduo.member.membercalendar.repository;

import com.workduo.group.group.entity.Group;
import com.workduo.member.member.entity.Member;
import com.workduo.member.membercalendar.entity.MemberCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberCalendarRepository extends JpaRepository<MemberCalendar, Long> {

    @Modifying
    @Query("update MemberCalendar  mc " +
            "set mc.meetingActiveStatus = 'MEETING_ACTIVE_STATUS_GROUP_CANCEL' " +
            "where mc.group = :group")
    void updateMemberCalendarMeetingActiveStatusGroupCancel(@Param("group") Group group);

    @Modifying
    @Query("update MemberCalendar  mc " +
            "set mc.meetingActiveStatus = 'MEETING_ACTIVE_STATUS_CANCEL' " +
            "where mc.member = :member")
    void updateMemberCalendarMemberWithdraw(@Param("member") Member member);
}