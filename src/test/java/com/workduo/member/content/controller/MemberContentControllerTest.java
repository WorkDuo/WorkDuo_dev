package com.workduo.member.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.workduo.common.CommonRequestContext;
import com.workduo.configuration.jwt.JwtAuthenticationFilter;
import com.workduo.configuration.jwt.TokenProvider;
import com.workduo.member.content.dto.*;
import com.workduo.member.content.entity.MemberContent;
import com.workduo.member.content.entity.MemberContentComment;
import com.workduo.member.content.repository.MemberContentRepository;
import com.workduo.member.content.service.MemberContentService;
import com.workduo.member.member.dto.MemberLogin;
import com.workduo.member.member.dto.auth.MemberAuthenticateDto;
import com.workduo.member.member.repository.MemberRepository;
import com.workduo.member.member.service.MemberService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberContentController.class)
@Import(
        {
                TokenProvider.class,
                CommonRequestContext.class,
                JwtAuthenticationFilter.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MEMBER Content API ?????????")
class MemberContentControllerTest {
    @MockBean
    private TokenProvider tokenProvider;
    @MockBean
    private MemberContentService memberContentService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    public static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    public static void close() {
        factory.close();
    }

    @Nested
    @DisplayName("?????? ?????? ?????? API ?????????")
    class CreateContentApi{

        @Test
        @DisplayName("?????? ?????? ?????? ?????? [???????????? ?????? ?????????]")
        public void failCreateMemberContent() throws Exception{
            List<String> errors = new ArrayList<>(
                    List.of("?????? ??? ?????? ?????? ?????? ?????????.",
                            "?????? ??? ?????? ?????? ?????? ?????????.",
                            "???????????? ?????? 0 ?????????."));
            //given
            ContentCreate.Request reqeust = ContentCreate.Request.builder().build();
            Set<ConstraintViolation<ContentCreate.Request>> violations
                    = validator.validate(reqeust);
            violations.forEach(
                    (error) -> {
                        System.out.println(error.getMessage());
                        assertThat(error.getMessage()).isIn(errors);
                    }
            );

            assertThat(violations.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("?????? ?????? ?????? ??????")
        public void successCreateMemberContent() throws Exception{

            ContentCreate.Request req = ContentCreate.Request.builder()
                    .title("abc")
                    .content("abcd")
                    .build();
            List<MultipartFile> arr = new ArrayList<>();
            MockMultipartFile image = new MockMultipartFile(
                    "multipartFiles",
                    "imagefile.jpeg",
                    "image/jpeg",
                    "<<jpeg data>>".getBytes()
            );
            //when
            doNothing().when(memberContentService).createContent(req,arr);
            //when
            mockMvc.perform(multipart("/api/v1/member/content")
                            .file(image)
                            .param("title", "abc")
                            .param("content", "abcd")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                    )
                    .andExpect(status().isOk())
                    .andDo(print());
        }
    }
    @Nested
    @DisplayName("?????? ?????? ?????? API ?????????")
    class getLists{
        // get api ??? ???????????? ????????? ?????? ???????????? ... ????????? ???????????? ??????????????? ?????? ????????? ?????????..
        @Test
        @DisplayName("?????? ?????? ?????? ??????")
        public void success() throws Exception{

            MemberContentListDto c = MemberContentListDto.builder()
                    .id(13L)
                    .title("test title")
                    .content("test content")
                    .memberId(1L)
                    .username("user")
                    .profileImg("aws/s3/somewhere")
                    .deletedYn(false)
                    .createdAt(LocalDateTime.now())
                    .count(3L)
                    .memberContentImages(new ArrayList<>())
                    .build();
            List<MemberContentListDto> list = new ArrayList<>(List.of(c));
            Page<MemberContentListDto> plist = new PageImpl<>(list);
            given(memberContentService.getContentList(any())).willReturn(
                plist
            );

            mockMvc.perform(get("/api/v1/member/content/list")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result.content.size()").value(1))
                    .andDo(print());

        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? API ?????????")
    class getDetail{
        @Test
        @DisplayName("?????? ?????? ?????? ??????")
        public void success() throws Exception{
            MemberContentListDto c = MemberContentListDto.builder()
                    .id(13L)
                    .title("test title")
                    .content("test content")
                    .memberId(1L)
                    .username("user")
                    .profileImg("aws/s3/somewhere")
                    .deletedYn(false)
                    .createdAt(LocalDateTime.now())
                    .count(3L)
                    .memberContentImages(new ArrayList<>())
                    .build();

            MemberContentCommentDto mcc = MemberContentCommentDto.builder()
                    .content("This is test comment!")
                    .id(1L)
                    .likeCnt(123467688L)
                    .build();
            List<MemberContentCommentDto> cList = new ArrayList<>(List.of(mcc));
            Page<MemberContentCommentDto> pList = new PageImpl<>(cList);

            given(memberContentService.getContentDetail(any())).willReturn(
                    MemberContentDetailDto.from(c,pList)
            );

            mockMvc.perform(get("/api/v1/member/content/3")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result.size()").value(12))
                    .andExpect(jsonPath("$.result.comments.content[0].likeCnt").value(123467688))
                    .andDo(print());

        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? API ?????????")
    class update{
        ContentUpdate.Request req = ContentUpdate.Request.builder().build();
        @Test
        @DisplayName("?????? ?????? ?????? ?????? [???????????? ?????? ?????????]")
        public void failUpdateMemberContent() throws Exception{
            List<String> errors = new ArrayList<>(
                    List.of("????????? ?????? ?????? ???????????????.",
                            "????????? ?????? ?????? ???????????????.",
                            "???????????? ?????? 0 ?????????."));
            //given
            Set<ConstraintViolation<ContentUpdate.Request>> violations
                    = validator.validate(req);
            violations.forEach(
                    (error) -> {
                        System.out.println(error.getMessage());
                        assertThat(error.getMessage()).isIn(errors);
                    }
            );
            // ???????????? ???????????? ?????? ??? ??????
            assertThat(violations.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("?????? ?????? ?????? ??????")
        public void successUpdate() throws Exception{
            //given
            doNothing().when(memberContentService).contentUpdate(any(),any());
            //when
            mockMvc.perform(patch("/api/v1/member/content/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                            ContentUpdate.Request.builder()
                                                    .title("testing")
                                                    .content("content")
                                                    .build()
                                    )
                            )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? API ?????????")
    class delete{
        @Test
        @DisplayName("?????? ?????? ?????? ??????")
        public void successUpdate() throws Exception{
            //when
            mockMvc.perform(delete("/api/v1/member/content/3")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ????????? API ?????????")
    class like{
        @Test
        @DisplayName("?????? ?????? ????????? ??????")
        public void successUpdate() throws Exception{
            //when
            mockMvc.perform(post("/api/v1/member/content/3/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ????????? ?????? API ?????????")
    class likeCancel{
        @Test
        @DisplayName("?????? ?????? ????????? ?????? ?????? ")
        public void successFeed() throws Exception{
            //given
            //when
            mockMvc.perform(delete("/api/v1/member/content/3/like")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andDo(print());
            //then
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ?????? API ?????????")
    class contentCommentCreate{
        @Test
        @DisplayName("?????? ?????? ?????? ?????? ?????? [???????????? ?????? ?????????]")
        public void NotBlankTest() throws Exception{
            ContentCommentCreate.Request req =
                    ContentCommentCreate.Request.builder().comment("Test").build();

            var test1
                    = validator.validate(req);
            assertThat(test1.size()).isEqualTo(0);

            req.setComment("");
            var test2 = validator.validate(req);
            assertThat(test2.size()).isEqualTo(1);

            req.setComment(" ");
            var test3 = validator.validate(req);
            assertThat(test3.size()).isEqualTo(1);

            req.setComment(null);
            var test4 = validator.validate(req);
            assertThat(test4.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("?????? ?????? ?????? ?????? ??????")
        public void contentCommentCreate() throws Exception{
            ContentCommentCreate.Request req
                    = ContentCommentCreate.Request.builder().comment("test").build();

            mockMvc.perform(post("/api/v1/member/content/3/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.success")
                                    .value("T")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ????????? API ?????????")
    class contentCommentList{
        @Test
        @DisplayName("?????? ?????? ?????? ????????? ??????")
        public void successCommentList() throws Exception{

            MemberContentCommentDto test = MemberContentCommentDto.builder()
                    .id(1L)
                    .content("test")
                    .build();

            PageImpl<MemberContentCommentDto> memberContentCommentDtos = new PageImpl<>(List.of(test));
            given(memberContentService.getContentCommentList(any(),any()))
                    .willReturn(memberContentCommentDtos);

            mockMvc.perform(get("/api/v1/member/content/3/comment?page=3&size=10&sort=test")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("T"))
                    .andExpect(jsonPath("$.result.content.size()").value(1))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ???????????? API ?????????")
    class contentCommentUpdate{

        @Test
        @DisplayName("?????? ?????? ?????? ???????????? ?????? [???????????? ?????? ?????????]")
        public void NotBlankTest() throws Exception{
            ContentCommentUpdate.Request req =
                    ContentCommentUpdate.Request.builder().comment("Test").build();

            var test1
                    = validator.validate(req);
            assertThat(test1.size()).isEqualTo(0);

            req.setComment("");
            var test2 = validator.validate(req);
            assertThat(test2.size()).isEqualTo(1);

            req.setComment(" ");
            var test3 = validator.validate(req);
            assertThat(test3.size()).isEqualTo(1);

            req.setComment(null);
            var test4 = validator.validate(req);
            assertThat(test4.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("?????? ?????? ?????? ???????????? ??????")
        public void successCommentList() throws Exception{

            ContentCommentUpdate.Request req
                    = ContentCommentUpdate.Request.builder().comment("test").build();

            mockMvc.perform(patch("/api/v1/member/content/3/comment/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.success")
                                    .value("T")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ?????? API ?????????")
    class contentCommentDelete{
        @Test
        @DisplayName("?????? ?????? ?????? ?????? ??????")
        public void successCommentList() throws Exception{
            mockMvc.perform(delete("/api/v1/member/content/3/comment/1")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.success")
                                    .value("T")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ????????? API ?????????")
    class contentCommentLike{
        @Test
        @DisplayName("?????? ?????? ?????? ????????? ??????")
        public void successCommentList() throws Exception{
            mockMvc.perform(post("/api/v1/member/content/3/comment/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.success")
                                    .value("T")
                    )
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("?????? ?????? ?????? ????????? ?????? API ?????????")
    class contentCommentLikeCancel{
        @Test
        @DisplayName("?????? ?????? ?????? ????????? ?????? ??????")
        public void successCommentList() throws Exception{
            mockMvc.perform(delete("/api/v1/member/content/3/comment/1/like")
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.success")
                                    .value("T")
                    )
                    .andDo(print());
        }
    }
}