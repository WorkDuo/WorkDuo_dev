package com.workduo.group.group.service.impl;

import com.workduo.area.siggarea.entity.SiggArea;
import com.workduo.area.siggarea.repository.SiggAreaRepository;
import com.workduo.common.CommonRequestContext;
import com.workduo.error.group.exception.GroupException;
import com.workduo.error.member.exception.MemberException;
import com.workduo.group.group.dto.*;
import com.workduo.group.group.entity.Group;
import com.workduo.group.group.entity.GroupJoinMember;
import com.workduo.group.group.entity.GroupLike;
import com.workduo.group.group.repository.GroupJoinMemberRepository;
import com.workduo.group.group.repository.GroupLikeRepository;
import com.workduo.group.group.repository.GroupRepository;
import com.workduo.group.group.repository.query.GroupQueryRepository;
import com.workduo.group.group.service.GroupService;
import com.workduo.group.group.type.GroupStatus;
import com.workduo.group.group.entity.GroupCreateMember;
import com.workduo.group.group.repository.GroupCreateMemberRepository;
import com.workduo.group.groupmetting.repository.GroupMeetingParticipantRepository;
import com.workduo.member.member.entity.Member;
import com.workduo.member.member.repository.MemberRepository;
import com.workduo.member.membercalendar.repository.MemberCalendarRepository;
import com.workduo.sport.sport.entity.Sport;
import com.workduo.sport.sport.repository.SportRepository;
import com.workduo.util.AwsS3Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.workduo.error.group.type.GroupErrorCode.*;
import static com.workduo.error.member.type.MemberErrorCode.MEMBER_EMAIL_ERROR;
import static com.workduo.group.group.dto.CreateGroup.Request;
import static com.workduo.group.group.type.GroupJoinMemberStatus.GROUP_JOIN_MEMBER_STATUS_ING;
import static com.workduo.group.group.type.GroupRole.GROUP_ROLE_LEADER;
import static com.workduo.group.group.type.GroupRole.GROUP_ROLE_NORMAL;
import static com.workduo.group.group.type.GroupStatus.GROUP_STATUS_ING;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final MemberRepository memberRepository;
    private final MemberCalendarRepository memberCalendarRepository;
    private final GroupRepository groupRepository;
    private final GroupQueryRepository groupQueryRepository;
    private final GroupCreateMemberRepository groupCreateMemberRepository;
    private final GroupJoinMemberRepository groupJoinMemberRepository;
    private final GroupMeetingParticipantRepository groupMeetingParticipantRepository;
    private final GroupLikeRepository groupLikeRepository;
    private final SportRepository sportRepository;
    private final SiggAreaRepository siggAreaRepository;
    private final CommonRequestContext context;
    private final AwsS3Provider awsS3Provider;
    private final EntityManager entityManager;

    /**
     * ?????? ??????
     * @param request
     */
    @Override
    @Transactional
    public void createGroup(Request request, List<MultipartFile> multipartFiles) {
        Member member = getMember(context.getMemberEmail());
        SiggArea siggArea = getSiggArea(request.getSgg());

        Sport sport = getSport(request.getSportId());

        createGroupValidate(member);

        Group group = Group.builder()
                .siggArea(siggArea)
                .sport(sport)
                .name(request.getName())
                .limitPerson(request.getLimitPerson())
                .introduce(request.getIntroduce())
                .groupStatus(GROUP_STATUS_ING)
                .build();

        groupRepository.save(group);
        entityManager.flush();

        String path = generatePath(group.getId());
        List<String> files = awsS3Provider.uploadFile(multipartFiles, path);
        group.updateThumbnail(files.get(0));

        GroupJoinMember groupJoinMember = GroupJoinMember.builder()
                .member(member)
                .group(group)
                .groupJoinMemberStatus(GROUP_JOIN_MEMBER_STATUS_ING)
                .groupRole(GROUP_ROLE_LEADER)
                .build();

        groupJoinMemberRepository.save(groupJoinMember);

        GroupCreateMember groupCreateMember = GroupCreateMember.builder()
                .group(group)
                .member(member)
                .build();

        groupCreateMemberRepository.save(groupCreateMember);
    }

    /**
     * ?????? ?????? - ???????????? ??????
     * @param groupId
     */
    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        Member member = getMember(context.getMemberEmail());

        Group group = getGroup(groupId);

        boolean existsGroupLeader =
                groupCreateMemberRepository.existsByMemberAndGroup(member, group);

        if (!existsGroupLeader) {
            throw new GroupException(GROUP_NOT_LEADER);
        }

        groupCreateMemberRepository.deleteByMemberAndGroup(member, group);
        groupJoinMemberRepository.updateGroupJoinMemberStatusCancel(group);
        groupMeetingParticipantRepository.deleteAllByGroup(group);
        memberCalendarRepository.updateMemberCalendarMeetingActiveStatusGroupCancel(group);
        group.updateGroupStatusCancel();
    }

    /**
     * ?????? ?????? - ???????????? ?????????
     * @param groupId
     */
    @Override
    @Transactional
    public void withdrawGroup(Long groupId) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);
        GroupJoinMember groupJoinMember = getGroupJoinMember(member, group);

        withdrawGroupValidate(member, group, groupJoinMember);

        groupMeetingParticipantRepository.deleteByGroupAndMember(group, member);
        memberCalendarRepository.updateMemberCalendarMemberAndGroupWithdraw(member, group);
        groupJoinMember.withdrawGroup();
    }

    /**
     * ?????? ??????
     * @param groupId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public GroupDto groupDetail(Long groupId) {
//        getMember(context.getMemberEmail());

        Group group = getGroup(groupId);
        if (group.getGroupStatus() != GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        return groupQueryRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GROUP_NOT_FOUND));
    }

    /**
     * ?????? ?????????
     *
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Page<GroupDto> groupList(Pageable pageable, ListGroup.Request condition) {
        String memberEmail = context.getMemberEmail();
        Long memberId = null;
        if (hasText(memberEmail)) {
            Member member = getMember(memberEmail);
            memberId = member.getId();
        }

        return groupQueryRepository.findByGroupList(pageable, memberId, condition);
    }

    /**
     * ?????? ?????????
     * @param groupId
     */
    @Override
    @Transactional
    public void groupLike(Long groupId) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);

        groupLikeValidate(member, group);

        GroupLike groupLike = GroupLike.builder()
                .group(group)
                .member(member)
                .build();

        groupLikeRepository.save(groupLike);
    }

    /**
     * ?????? ????????? ??????
     * @param groupId
     */
    @Override
    @Transactional
    public void groupUnLike(Long groupId) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);

        groupUnLikeValidate(member, group);

        groupLikeRepository.deleteByGroupAndMember(group, member);
    }

    /**
     * ?????? ??????
     * @param groupId
     */
    @Override
    @Transactional
    public void groupParticipant(Long groupId) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);

        groupJoinValidate(member, group);

        GroupJoinMember groupJoinMember = GroupJoinMember.builder()
                .group(group)
                .member(member)
                .groupJoinMemberStatus(GROUP_JOIN_MEMBER_STATUS_ING)
                .groupRole(GROUP_ROLE_NORMAL)
                .build();

        groupJoinMemberRepository.save(groupJoinMember);
    }

    /**
     * ?????? ????????? ?????????
     * @param pageable
     * @param groupId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public Page<GroupParticipantsDto> groupParticipantList(Pageable pageable, Long groupId) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);
        GroupJoinMember groupJoinMember = getGroupJoinMember(member, group);

        groupParticipantValidate(member, group, groupJoinMember);
        return groupQueryRepository.findByGroupParticipantList(pageable, groupId);
    }

    /**
     * ?????? ????????? ??????
     * @param groupId
     * @param multipartFiles
     * @return
     */
    @Override
    @Transactional
    public GroupThumbnail groupThumbnailUpdate(Long groupId, List<MultipartFile> multipartFiles) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);

        boolean existsGroupLeader =
                groupCreateMemberRepository.existsByMemberAndGroup(member, group);

        if (!existsGroupLeader) {
            throw new GroupException(GROUP_NOT_LEADER);
        }

        List<String> paths = new ArrayList<>(
                Arrays.asList(AwsS3Provider.parseAwsUrl(group.getThumbnailPath()))
        );

        awsS3Provider.deleteFile(paths);

        String path = generatePath(group.getId());
        List<String> files = awsS3Provider.uploadFile(multipartFiles, path);
        group.updateThumbnail(files.get(0));

        return GroupThumbnail.fromEntity(group);
    }

    @Override
    @Transactional
    public UpdateGroup.Response groupUpdate(Long groupId, UpdateGroup.Request request) {
        Member member = getMember(context.getMemberEmail());
        Group group = getGroup(groupId);

        boolean existsGroupLeader =
                groupCreateMemberRepository.existsByMemberAndGroup(member, group);

        if (!existsGroupLeader) {
            throw new GroupException(GROUP_NOT_LEADER);
        }

        Integer countByGroup = groupJoinMemberRepository.countByGroup(group);
        group.groupUpdate(request, countByGroup);

        return UpdateGroup.Response.fromEntity(group);
    }

    private void groupParticipantValidate(Member member, Group group, GroupJoinMember groupJoinMember) {
        if (group.getGroupStatus() != GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        boolean existsByMember = groupJoinMemberRepository.existsByGroupAndMember(group, member);
        if (!existsByMember) {
            throw new GroupException(GROUP_NOT_FOUND_USER);
        }

        if (groupJoinMember.getGroupJoinMemberStatus() != GROUP_JOIN_MEMBER_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_WITHDRAW);
        }
    }

    private void groupJoinValidate(Member member, Group group) {

        boolean exists = groupJoinMemberRepository.existsByGroupAndMember(group, member);
        if (exists) {
            throw new GroupException(GROUP_ALREADY_PARTICIPANT) ;
        }

        if (group.getGroupStatus() != GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        Integer groupParticipants = groupJoinMemberRepository.countByGroup(group);
        if (groupParticipants >= group.getLimitPerson()) {
            throw new GroupException(GROUP_MAXIMUM_PARTICIPANT);
        }
    }

    private void createGroupValidate(Member member) {

        Long groupCreateMemberCount = groupCreateMemberCount(member);
        if (groupCreateMemberCount >= 3) {
            throw new GroupException(GROUP_MAXIMUM_EXCEEDED);
        }
    }

    private void withdrawGroupValidate(Member member, Group group, GroupJoinMember groupJoinMember) {

        if (group.getGroupStatus() != GroupStatus.GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        boolean existsGroupLeader =
                groupCreateMemberRepository.existsByMemberAndGroup
                        (member, group);

        if (existsGroupLeader) {
            throw new GroupException(GROUP_LEADER_NOT_WITHDRAW);
        }

        if (groupJoinMember.getGroupJoinMemberStatus() != GROUP_JOIN_MEMBER_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_WITHDRAW);
        }

    }

    private void groupLikeValidate(Member member, Group group) {

        boolean exists = groupLikeRepository.existsByGroupAndMember(group, member);
        if (exists) {
            throw new GroupException(GROUP_ALREADY_LIKE);
        }

        if (group.getGroupStatus() != GroupStatus.GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        boolean existsByMember = groupJoinMemberRepository.existsByGroupAndMember(group, member);
        if (!existsByMember ) {
            throw new GroupException(GROUP_NOT_FOUND_USER);
        }

        GroupJoinMember groupJoinMember = getGroupJoinMember(member, group);
        if (groupJoinMember.getGroupJoinMemberStatus() != GROUP_JOIN_MEMBER_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_WITHDRAW);
        }
    }

    private void groupUnLikeValidate(Member member, Group group) {

        if (group.getGroupStatus() != GroupStatus.GROUP_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_DELETE_GROUP);
        }

        boolean existsByMember = groupJoinMemberRepository.existsByGroupAndMember(group, member);
        if (!existsByMember ) {
            throw new GroupException(GROUP_NOT_FOUND_USER);
        }

        GroupJoinMember groupJoinMember = getGroupJoinMember(member, group);
        if (groupJoinMember.getGroupJoinMemberStatus() != GROUP_JOIN_MEMBER_STATUS_ING) {
            throw new GroupException(GROUP_ALREADY_WITHDRAW);
        }
    }

    private GroupJoinMember getGroupJoinMember(Member member, Group group) {
        return groupJoinMemberRepository.findByMemberAndGroup(member, group)
                .orElseThrow(() -> new GroupException(GROUP_NOT_FOUND_USER));
    }

    private Member getMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MEMBER_EMAIL_ERROR));
    }

    private Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GROUP_NOT_FOUND));
    }

    private Long groupCreateMemberCount(Member member) {
        return groupCreateMemberRepository.countByMember(member);
    }

    private Sport getSport(Integer id) {
        return sportRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("?????? ????????? ?????? ???????????????."));
    }

    private SiggArea getSiggArea(String sgg) {
        return siggAreaRepository.findBySgg(sgg)
                .orElseThrow(() -> new IllegalStateException("?????? ????????? ?????? ???????????????."));
    }

    public String generatePath(Long groupId) {
        return "group/" + groupId + "/";
    }
}
