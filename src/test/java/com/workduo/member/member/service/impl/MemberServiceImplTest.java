package com.workduo.member.member.service.impl;

import com.workduo.area.siggarea.repository.SiggAreaRepository;
import com.workduo.error.member.exception.MemberException;
import com.workduo.error.member.type.MemberErrorCode;
import com.workduo.member.existmember.entity.ExistMember;
import com.workduo.member.existmember.repository.ExistMemberRepository;
import com.workduo.member.member.dto.MemberCreate;
import com.workduo.member.member.dto.MemberLogin;
import com.workduo.member.member.entity.Member;
import com.workduo.member.member.repository.MemberRepository;
import com.workduo.member.member.type.MemberStatus;
import com.workduo.member.memberrole.repository.MemberRoleRepository;
import com.workduo.sport.sport.repository.SportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.workduo.member.member.type.MemberStatus.MEMBER_STATUS_ING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ExistMemberRepository existMemberRepository;
    @Mock
    private MemberRoleRepository memberRoleRepository;

    @Mock
    private SiggAreaRepository siggAreaRepository;

    @Mock
    private SportRepository sportRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberServiceImpl memberService;
    @Nested
    class TestLoginMethod{
        @Test
        @DisplayName("유저 검증 [메일이 없는경우]")
        void failAuthenticateDoesNotExistEmail(){
            MemberLogin.Request req = MemberLogin.Request.builder()
                    .email("abc")
                    .password("something")
                    .build();

            //when
            MemberException exception =  assertThrows(MemberException.class,
                    ()->memberService.authenticateUser(req));
            //then
            assertEquals(MemberErrorCode.MEMBER_EMAIL_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("유저 검증 [정지된 회원]")
        void failAuthenticateBannedUser(){
            MemberLogin.Request req = MemberLogin.Request.builder()
                    .email("abc")
                    .password("something")
                    .build();
            //given
            given(memberRepository.findByEmail(any())).willReturn(
                    Optional.of(Member.builder().memberStatus(MemberStatus.MEMBER_STATUS_STOP).build())
            );
            //when
            MemberException exception =  assertThrows(MemberException.class,
                    ()->memberService.authenticateUser(req));
            //then
            assertEquals(MemberErrorCode.MEMBER_STOP_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("유저 검증 [탈퇴한 회원]")
        void failAuthenticateWithdrawUser(){
            MemberLogin.Request req = MemberLogin.Request.builder()
                    .email("abc")
                    .password("something")
                    .build();
            //given
            given(memberRepository.findByEmail(any())).willReturn(
                    Optional.of(Member.builder().memberStatus(MemberStatus.MEMBER_STATUS_WITHDRAW).build())
            );
            //when
            MemberException exception =  assertThrows(MemberException.class,
                    ()->memberService.authenticateUser(req));
            //then
            assertEquals(MemberErrorCode.MEMBER_WITHDRAW_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("유저 검증 [비밀번호가 다름]")
        void failAuthenticatePasswordDifferent(){
            MemberLogin.Request req = MemberLogin.Request.builder()
                    .email("test@test.com")
                    .password("something")
                    .build();
            //given
            given(memberRepository.findByEmail(any())).willReturn(
                    Optional.of(Member.builder().memberStatus(MemberStatus.MEMBER_STATUS_ING).build())
            );
            given(passwordEncoder.matches(any(),any())).willReturn(false);
            //when
            MemberException exception =  assertThrows(MemberException.class,
                    ()->memberService.authenticateUser(req));
            //then
            assertEquals(MemberErrorCode.MEMBER_PASSWORD_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("유저 롤 받아오기")
        void successGetMemberRole(){
            MemberLogin.Request req = MemberLogin.Request.builder()
                    .email("test@test.com")
                    .password("something")
                    .build();
            Member m = Member.builder().memberStatus(MemberStatus.MEMBER_STATUS_ING).build();
            //given
            given(memberRepository.findByEmail(any())).willReturn(
                    Optional.of(m)
            );
            given(passwordEncoder.matches(any(),any())).willReturn(true);
            memberService.authenticateUser(req);
            verify(memberRoleRepository,times(1)).findByMember(m);
        }
    }

    @Nested
    class TestSignInMethod{
        List<Integer> createList = new ArrayList<>(List.of(1));
        MemberCreate.Request createReqeust = MemberCreate.Request
                .builder()
                .email("test@test.com")
                .password("12345abc@")
                .username("test")
                .phoneNumber("1")
                .siggAreaList(createList)
                .nickname("feelingGood")
                .sportList(createList)
                .build();

        @Test
        @DisplayName("회원가입 실패[이메일 폼 형태가 아닌경우]")
        void emailFormCheckFail(){
            createReqeust.setEmail("amIEmail?");
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_EMAIL_FORM,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[이메일 중복인경우]")
        void emailDuplicateCheckFail(){
            //given
            doReturn(true).when(existMemberRepository).existsByMemberEmail(any());
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_EMAIL_DUPLICATE,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[닉네임 중복인경우]")
        void nicknameDuplicateCheckFail(){
            doReturn(true).when(memberRepository).existsByNickname(any());
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_NICKNAME_DUPLICATE,exception.getErrorCode());
        }
        @Test
        @DisplayName("회원가입 실패[전화번호 중복인경우]")
        void mobileDuplicateCheckFail(){
            doReturn(true).when(memberRepository).existsByPhoneNumber(any());
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_PHONE_DUPLICATE,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[비밀번호 정책 위반] [패스워드 길이]")
        void passwordPolicyCheckFail(){
            createReqeust.setPassword("1");
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_PASSWORD_POLICY,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[비밀번호 정책 위반] [문자 만 있는경우]")
        void passwordPolicyCheckFailOnlyLetters(){
            createReqeust.setPassword("abcabcabc");
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_PASSWORD_POLICY,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[비밀번호 정책 위반] [숫자 만 있는경우]")
        void passwordPolicyCheckFailOnlyNumbers(){
            createReqeust.setPassword("1231231123");
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_PASSWORD_POLICY,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[비밀번호 정책 위반] [특수문자가 없는 경우]")
        void passwordPolicyCheckFailWithoutSpecialCharacter(){
            createReqeust.setPassword("abccsdfasbc");
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_PASSWORD_POLICY,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[지역 이 데이터 상에 없는경우]")
        void siggCheckFail(){
            doReturn(false).when(siggAreaRepository).existsById(any());
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_SIGG_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 실패[운동 이 데이터 상에 없는경우]")
        void sportCheckFail(){
            doReturn(true).when(siggAreaRepository).existsById(any());
            //when
            MemberException exception = assertThrows(
                    MemberException.class,
                    ()->memberService.createUser(createReqeust)
            );
            //then
            assertEquals(MemberErrorCode.MEMBER_SPORT_ERROR,exception.getErrorCode());
        }

        @Test
        @DisplayName("회원가입 성공")
        void successSignIn(){
            doReturn(true).when(siggAreaRepository).existsById(any());
            doReturn(true).when(sportRepository).existsById(any());
            ExistMember e = ExistMember.builder()
                    .memberEmail(createReqeust.getEmail())
                    .build();
            //when
            memberService.createUser(createReqeust);
            //then
            verify(memberRepository,times(1)).save(any());
            verify(existMemberRepository,times(1)).save(any());
        }
    }
}