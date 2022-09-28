package com.workduo.group.groupmetting.service;

import com.workduo.group.groupmetting.dto.CreateMeeting;
import com.workduo.group.groupmetting.dto.TimeDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GroupMeetingService {

    /**
     * 유저 모임 일정
     * @param startDate
     */
    TimeDto meetingInquire(LocalDate startDate);

    /**
     * 그룹 모임 생성
     * @param groupId
     */
    void createMeeting(CreateMeeting.Request request, Long groupId);
}