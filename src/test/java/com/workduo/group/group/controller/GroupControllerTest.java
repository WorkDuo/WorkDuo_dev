package com.workduo.group.group.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workduo.area.sidoarea.dto.SidoAreaDto;
import com.workduo.area.sidoarea.entity.SidoArea;
import com.workduo.area.siggarea.dto.siggarea.SiggAreaDto;
import com.workduo.area.siggarea.entity.SiggArea;
import com.workduo.common.CommonRequestContext;
import com.workduo.configuration.jwt.JwtAuthenticationFilter;
import com.workduo.configuration.jwt.TokenProvider;
import com.workduo.error.global.handler.GlobalExceptionHandler;
import com.workduo.error.group.exception.GroupException;
import com.workduo.error.group.handler.GroupExceptionHandler;
import com.workduo.error.member.exception.MemberException;
import com.workduo.error.member.handler.MemberExceptionHandler;
import com.workduo.group.group.dto.CreateGroup;
import com.workduo.group.group.dto.GroupDto;
import com.workduo.group.group.dto.GroupParticipantsDto;
import com.workduo.group.group.dto.ListGroup;
import com.workduo.group.group.entity.Group;
import com.workduo.group.group.service.GroupService;
import com.workduo.group.group.type.GroupStatus;
import com.workduo.member.member.entity.Member;
import com.workduo.sport.sport.dto.SportDto;
import com.workduo.sport.sport.entity.Sport;
import com.workduo.sport.sportcategory.dto.SportCategoryDto;
import com.workduo.sport.sportcategory.entity.SportCategory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;

import static com.workduo.error.group.type.GroupErrorCode.*;
import static com.workduo.error.member.type.MemberErrorCode.MEMBER_EMAIL_ERROR;
import static com.workduo.group.group.type.GroupRole.GROUP_ROLE_LEADER;
import static com.workduo.group.group.type.GroupStatus.GROUP_STATUS_ING;
import static com.workduo.member.member.type.MemberStatus.MEMBER_STATUS_ING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(
        {
                TokenProvider.class,
                CommonRequestContext.class,
                JwtAuthenticationFilter.class,
                MemberException.class,
                MemberExceptionHandler.class,
                GroupException.class,
                GroupExceptionHandler.class,
                GlobalExceptionHandler.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
public class GroupControllerTest {

    @MockBean
    private GroupService groupService;
    @MockBean
    private TokenProvider tokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static ValidatorFactory factory;
    private static Validator validator;

    static Member member;
    static SportCategory sportCategory;
    static Sport sport;
    static SidoArea sidoArea;
    static SiggArea siggArea;
    static Group group;
    static GroupDto groupDto;
    static MockMultipartFile image;

    @BeforeEach
    public void setup() {
        GroupController groupController = new GroupController(groupService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(groupController)
                .setControllerAdvice(
                        GroupExceptionHandler.class,
                        MemberExceptionHandler.class,
                        GlobalExceptionHandler.class
                ).
                setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .setViewResolvers(
                        (viewName, locale) -> new MappingJackson2JsonView()
                )
                .build();
    }

    @BeforeAll
    public static void init() {
        image = new MockMultipartFile(
                "multipartFiles",
                "imagefile.jpeg",
                "image/jpeg",
                "<<jpeg data>>".getBytes()
        );

        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        sport = Sport.builder()
                .id(1)
                .sportCategory(SportCategory.builder()
                        .id(1)
                        .name("??????")
                        .build())
                .name("??????")
                .build();

        siggArea = SiggArea.builder()
                .sgg("11110")
                .sidonm("11")
                .sggnm("?????????")
                .sidonm("???????????????")
                .sidoArea(SidoArea.builder()
                        .sido("11")
                        .sidonm("???????????????")
                        .build())
                .build();

        member = Member.builder()
                .id(1L)
                .username("?????????")
                .phoneNumber("01011111111")
                .nickname("??????")
                .password("1234")
                .email("rbsks147@naver.com")
                .memberStatus(MEMBER_STATUS_ING)
                .build();

        group = Group.builder()
                .id(1L)
                .name("test")
                .sport(sport)
                .limitPerson(10)
                .siggArea(siggArea)
                .thumbnailPath("test")
                .groupStatus(GROUP_STATUS_ING)
                .introduce("test")
                .build();

        groupDto = GroupDto.builder()
                .groupId(1L)
                .introduce("test")
                .thumbnailPath("test")
                .limitPerson(10)
                .sport(SportDto.builder().id(1).build())
                .sportCategory(SportCategoryDto.builder().id(1).build())
                .sidoArea(SidoAreaDto.builder().sido("11").build())
                .siggArea(SiggAreaDto.builder().sgg("11110").build())
                .name("test")
                .build();

        sportCategory = SportCategory.builder()
                .id(1)
                .name("??????")
                .build();

        sidoArea = SidoArea.builder()
                .sido("11")
                .sidonm("???????????????")
                .build();
    }

    @AfterAll
    public static void close() {
        factory.close();
    }

    @Nested
    public class createGroup {
        @Test
        @DisplayName("?????? ?????? - ???????????? ???????????? ?????? ??????")
        public void createGroupRequestFail() throws Exception {
            // given
            List<String> errors = new ArrayList<>(List.of(
                    "??????????????? ?????? ?????? ???????????????.",
                    "?????? ????????? ?????? 10????????????.",
                    "?????? ????????? ?????? 200????????????.",
                    "????????? ?????? ?????? ???????????????.",
                    "????????? ?????? ?????? ???????????????.",
                    "?????? ???????????? ?????? ?????? ???????????????."
            ));

            CreateGroup.Request request = CreateGroup.Request.builder().build();
            // when
            Set<ConstraintViolation<CreateGroup.Request>> violations =
                    validator.validate(request);

            // then
            violations.forEach(
                    error -> assertThat(error.getMessage()).isIn(errors)
            );
        }

        @Test
        @DisplayName("?????? ?????? ??????")
        public void createGroup() throws Exception {
            // given

            // when

            // then
            mockMvc.perform(multipart("/api/v1/group")
                            .file(image)
                            .param("name", "test")
                            .param("limitPerson", "10")
                            .param("sportId", "1")
                            .param("sgg", "11110")
                            .param("introduce", "test")
                            .with(request -> {
                                request.setMethod("POST");
                                return request;
                            })
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());

            verify(groupService, times(1))
                    .createGroup(any(), any());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void createGroupFailNotFoundUser() throws Exception {
            // given

            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                    .createGroup(any(), any());

            // then
            mockMvc.perform(multipart("/api/v1/group")
                            .file(image)
                            .param("name", "test")
                            .param("limitPerson", "10")
                            .param("sportId", "1")
                            .param("sgg", "11110")
                            .param("introduce", "test")
                            .with(request -> {
                                request.setMethod("POST");
                                return request;
                            })
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void createGroupFailNotFoundSigg() throws Exception {
            // given

            // when
            doThrow(new IllegalStateException("?????? ????????? ?????? ???????????????.")).when(groupService)
                    .createGroup(any(), any());

            // then
            mockMvc.perform(multipart("/api/v1/group")
                            .file(image)
                            .param("name", "test")
                            .param("limitPerson", "10")
                            .param("sportId", "1")
                            .param("sgg", "11110")
                            .param("introduce", "test")
                            .with(request -> {
                                request.setMethod("POST");
                                return request;
                            })
                    )
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.message").value("?????? ????????? ?????? ???????????????."))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void createGroupFailNotFoundSport() throws Exception {
            // given

            // when
            doThrow(new IllegalStateException("?????? ????????? ?????? ???????????????.")).when(groupService)
                    .createGroup(any(), any());

            // then
            mockMvc.perform(multipart("/api/v1/group")
                            .file(image)
                            .param("name", "test")
                            .param("limitPerson", "10")
                            .param("sportId", "1")
                            .param("sgg", "11110")
                            .param("introduce", "test")
                            .with(request -> {
                                request.setMethod("POST");
                                return request;
                            })
                    )
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.message").value("?????? ????????? ?????? ???????????????."))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ???????????? ??????")
        public void createGroupFailMaximumExceeded() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_MAXIMUM_EXCEEDED)).when(groupService)
                    .createGroup(any(), any());

            // then
            mockMvc.perform(multipart("/api/v1/group")
                            .file(image)
                            .param("name", "test")
                            .param("limitPerson", "10")
                            .param("sportId", "1")
                            .param("sgg", "11110")
                            .param("introduce", "test")
                            .with(request -> {
                                request.setMethod("POST");
                                return request;
                            })
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_MAXIMUM_EXCEEDED.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class deleteGroup {

        @Test
        @DisplayName("?????? ?????? ??????")
        public void deleteGroup() throws Exception {
            // given

            // when

            // then
            mockMvc.perform(post("/api/v1/group/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());

            verify(groupService, times(1))
                    .deleteGroup(anyLong());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void deleteGroupNotFoundUser() throws Exception {
            // given

            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                            .deleteGroup(anyLong());

            // then
            mockMvc.perform(post("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());

        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void deleteGroupNotFoundGroup() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                    .deleteGroup(anyLong());

            // then
            mockMvc.perform(post("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());

        }

        @Test
        @DisplayName("?????? ?????? ?????? - ???????????? ?????? ??????")
        public void deleteGroupNotLeader() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_NOT_LEADER)).when(groupService)
                    .deleteGroup(anyLong());

            // then
            mockMvc.perform(post("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_LEADER.getMessage()))
                    .andDo(print());

        }
    }

    @Nested
    public class withdrawGroup {

        @Test
        @DisplayName("?????? ?????? ??????")
        public void withdrawGroup() throws Exception {
            // given

            // when

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void withdrawGroupNotFoundUser() throws Exception {
            // given

            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                            .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void withdrawGroupNotFoundGroup() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                    .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ????????? ?????? ????????? ?????? ???")
        public void withdrawGroupNotFoundGroupUser() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_NOT_FOUND_USER)).when(groupService)
                    .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND_USER.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ????????? ????????? ??????")
        public void withdrawGroupAlreadyDeleteGroup() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP)).when(groupService)
                    .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ????????? ??????")
        public void withdrawGroupLeaderNotWithdraw() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_LEADER_NOT_WITHDRAW)).when(groupService)
                    .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_LEADER_NOT_WITHDRAW.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ????????? ????????? ??????")
        public void withdrawGroupAlreadyWithdraw() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_ALREADY_WITHDRAW)).when(groupService)
                    .withdrawGroup(anyLong());

            // then
            mockMvc.perform(delete("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_WITHDRAW.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class detailGroup {

        @Test
        @DisplayName("?????? ??????")
        public void detailGroup() throws Exception {
            // given
            doReturn(groupDto).when(groupService)
                    .groupDetail(anyLong());

            // when

            // then
            mockMvc.perform(get("/api/v1/group/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result.groupId").value(1))
                    .andExpect(jsonPath("$.result.siggArea.sgg").value("11110"))
                    .andExpect(jsonPath("$.result.sidoArea.sido").value("11"))
                    .andExpect(jsonPath("$.result.sport.id").value(1))
                    .andExpect(jsonPath("$.result.sportCategory.id").value(1))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ????????? ??????")
        public void detailGroupFailAlreadyDelete() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP))
                    .when(groupService).groupDetail(anyLong());

            // then
            mockMvc.perform(get("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void detailGroupFailNotFoundGroup() throws Exception {
            // given

            // when
            doThrow(new GroupException(GROUP_NOT_FOUND))
                    .when(groupService).groupDetail(anyLong());

            // then
            mockMvc.perform(get("/api/v1/group/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class groupLike {

        @Test
        @DisplayName("?????? ?????????")
        public void groupLike() throws Exception {
            // given
            doNothing().when(groupService)
                    .groupLike(anyLong());

            // when

            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ?????? ?????? ??????")
        public void groupLikeFailNotFoundUser() throws Exception {
            // given


            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                            .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ?????? ?????? ??????")
        public void groupLikeFailNotFoundGroup() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                    .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ?????? ???????????? ?????? ??????")
        public void groupLikeFailAlreadyLike() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_LIKE)).when(groupService)
                    .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_LIKE.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ????????? ????????? ??????")
        public void groupLikeFailAlreadyDelete() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP)).when(groupService)
                    .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ????????? ?????? ????????? ?????? ??????")
        public void groupLikeFailNotFoundGroupUser() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_NOT_FOUND_USER)).when(groupService)
                    .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND_USER.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? - ????????? ????????? ????????? ??????")
        public void groupLikeFailAlreadyWithdraw() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_WITHDRAW)).when(groupService)
                    .groupLike(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_WITHDRAW.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class groupUnLike {

        @Test
        @DisplayName("?????? ????????? ??????")
        public void groupUnLike() throws Exception {
            // given
            doNothing().when(groupService)
                    .groupUnLike(anyLong());

            // when

            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? ?????? - ?????? ?????? ??????")
        public void groupUnLikeFailNotFoundUser() throws Exception {
            // given


            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                    .groupUnLike(anyLong());
            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? ?????? - ?????? ?????? ??????")
        public void groupUnLikeFailNotFoundGroup() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                    .groupUnLike(anyLong());
            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? ?????? - ????????? ????????? ??????")
        public void groupUnLikeFailAlreadyDelete() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP)).when(groupService)
                    .groupUnLike(anyLong());
            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? ?????? - ????????? ?????? ????????? ?????? ??????")
        public void groupUnLikeFailNotFoundGroupUser() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_NOT_FOUND_USER)).when(groupService)
                    .groupUnLike(anyLong());
            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND_USER.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ????????? ?????? ?????? - ????????? ????????? ????????? ??????")
        public void groupUnLikeFailAlreadyWithdraw() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_WITHDRAW)).when(groupService)
                    .groupUnLike(anyLong());
            // then
            mockMvc.perform(delete("/api/v1/group/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_WITHDRAW.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class groupParticipant {

        @Test
        @DisplayName("?????? ??????")
        public void groupParticipant() throws Exception {
            // given
            doNothing().when(groupService)
                    .groupParticipant(anyLong());

            // when

            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void groupParticipantFailNotFoundUser() throws Exception {
            // given


            // when
            doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                    .groupParticipant(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
        public void groupParticipantFailNotFoundGroup() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                    .groupParticipant(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ????????? ????????? ??????")
        public void participantFailAlreadyParticipant() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_PARTICIPANT)).when(groupService)
                    .groupParticipant(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_PARTICIPANT.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ????????? ????????? ??????")
        public void participantFailAlreadyDelete() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP)).when(groupService)
                    .groupParticipant(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                    .andDo(print());
        }

        @Test
        @DisplayName("?????? ?????? ?????? - ?????? ???????????? ?????? ????????? ??????")
        public void groupUnLikeFailNotFoundGroupUser() throws Exception {
            // given


            // when
            doThrow(new GroupException(GROUP_MAXIMUM_PARTICIPANT)).when(groupService)
                    .groupParticipant(anyLong());
            // then
            mockMvc.perform(post("/api/v1/group/1/participant")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(jsonPath("$.success").value("F"))
                    .andExpect(jsonPath("$.errorMessage").value(GROUP_MAXIMUM_PARTICIPANT.getMessage()))
                    .andDo(print());
        }
    }

    @Nested
    public class groupList {

        @Test
        @DisplayName("?????? ????????? ??????")
        public void groupList() throws Exception {
            // given
            SidoAreaDto sidoAreaDto = SidoAreaDto.fromEntity(sidoArea);
            SiggAreaDto siggAreaDto = SiggAreaDto.fromEntity(siggArea);
            SportCategoryDto sportCategoryDto = SportCategoryDto.fromEntity(sportCategory);
            SportDto sportDto = SportDto.fromEntity(sport);

            GroupDto groupDto = GroupDto.builder()
                    .groupId(1L)
                    .sidoArea(sidoAreaDto)
                    .siggArea(siggAreaDto)
                    .sport(sportDto)
                    .sportCategory(sportCategoryDto)
                    .introduce("test")
                    .limitPerson(10)
                    .likes(10L)
                    .participants(20L)
                    .thumbnailPath("test")
                    .name("group1")
                    .build();

            List<GroupDto> groupDtoList = new ArrayList<>(List.of(groupDto));
            Page<GroupDto> groupDtos = new PageImpl<>(groupDtoList);

            doReturn(groupDtos).when(groupService)
                    .groupList(any(), any());

            // when

            // then
            mockMvc.perform(get("/api/v1/group")
                            .param("page", "0")
                            .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Nested
        public class groupParticipantList {

            @Test
            @DisplayName("?????? ????????? ????????? ??????")
            public void groupParticipantList() throws Exception {
                // given
                GroupParticipantsDto groupParticipantsDto = GroupParticipantsDto.builder()
                        .userId(1L)
                        .username("test")
                        .nickname("test")
                        .profileImg("test")
                        .groupRole(GROUP_ROLE_LEADER)
                        .build();
                List<GroupParticipantsDto> participantsDtoList =
                        new ArrayList<>(List.of(groupParticipantsDto));
                Page<GroupParticipantsDto> participantsDtos = new PageImpl<>(participantsDtoList);

                doReturn(participantsDtos).when(groupService)
                        .groupParticipantList(any(), anyLong());

                // when

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andDo(print());
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? - ?????? ?????? ??????")
            public void groupParticipantListFailNotFoundUser() throws Exception {
                // given

                // when
                doThrow(new MemberException(MEMBER_EMAIL_ERROR)).when(groupService)
                                .groupParticipantList(any(), anyLong());

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value("F"))
                        .andExpect(jsonPath("$.errorMessage").value(MEMBER_EMAIL_ERROR.getMessage()))
                        .andDo(print());
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? - ?????? ??????")
            public void groupParticipantListFailNotFoundGroup() throws Exception {
                // given

                // when
                doThrow(new GroupException(GROUP_NOT_FOUND)).when(groupService)
                        .groupParticipantList(any(), anyLong());

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value("F"))
                        .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND.getMessage()))
                        .andDo(print());
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? - ????????? ?????? ?????? ??????")
            public void groupParticipantListFailNotFoundGroupUser() throws Exception {
                // given

                // when
                doThrow(new GroupException(GROUP_NOT_FOUND_USER)).when(groupService)
                        .groupParticipantList(any(), anyLong());

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value("F"))
                        .andExpect(jsonPath("$.errorMessage").value(GROUP_NOT_FOUND_USER.getMessage()))
                        .andDo(print());
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? - ?????? ????????? ??????")
            public void groupParticipantListFailAlreadyDeleteGroup() throws Exception {
                // given

                // when
                doThrow(new GroupException(GROUP_ALREADY_DELETE_GROUP)).when(groupService)
                        .groupParticipantList(any(), anyLong());

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value("F"))
                        .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_DELETE_GROUP.getMessage()))
                        .andDo(print());
            }

            @Test
            @DisplayName("?????? ????????? ????????? ?????? - ?????? ????????? ??????")
            public void groupParticipantListFailAlreadyWithdrawGroup() throws Exception {
                // given

                // when
                doThrow(new GroupException(GROUP_ALREADY_WITHDRAW)).when(groupService)
                        .groupParticipantList(any(), anyLong());

                // then
                mockMvc.perform(get("/api/v1/group/participant/1")
                                .param("page", "0")
                                .param("size", "5")
                                .contentType(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value("F"))
                        .andExpect(jsonPath("$.errorMessage").value(GROUP_ALREADY_WITHDRAW.getMessage()))
                        .andDo(print());
            }
        }
    }
}
