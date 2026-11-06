package com.nhuhuy05.enghub.media.service;

import com.cloudinary.Cloudinary;
import com.nhuhuy05.enghub.config.CloudinaryProperties;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.media.dto.MediaAutoLinkResponse;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceTest {

    @Mock
    Cloudinary cloudinary;
    @Mock
    CloudinaryProperties cloudinaryProperties;
    @Mock
    MediaAssetRepository mediaAssetRepository;
    @Mock
    TestRepository testRepository;
    @Mock
    QuestionGroupImageRepository questionGroupImageRepository;
    @Mock
    QuestionGroupAudioRepository questionGroupAudioRepository;
    @Mock
    QuestionGroupPassageRepository questionGroupPassageRepository;
    @Mock
    QuestionGroupRepository questionGroupRepository;
    @Mock
    QuestionRepository questionRepository;

    MediaAssetService mediaAssetService;

    @BeforeEach
    void setUp() {
        mediaAssetService = new MediaAssetService(
                cloudinary,
                cloudinaryProperties,
                mediaAssetRepository,
                testRepository,
                questionGroupImageRepository,
                questionGroupAudioRepository,
                questionGroupPassageRepository,
                questionGroupRepository,
                questionRepository
        );
    }

    @Test
    void autoLinkMedia_whenQuestionGroupsExist_linksMedia() {
        Long testId = 1L;
        com.nhuhuy05.enghub.test.entity.Test exam = com.nhuhuy05.enghub.test.entity.Test.builder()
                .id(testId)
                .title("Test 1")
                .build();
        when(testRepository.findById(testId)).thenReturn(Optional.of(exam));

        TestPart part1 = TestPart.builder().id(10L).partNumber(1).build();
        QuestionGroup group1 = QuestionGroup.builder().id(100L).testPart(part1).orderIndex(1).build();
        when(questionGroupRepository.findAllByTestPartTestIdOrderByTestPartPartNumberAscOrderIndexAsc(testId))
                .thenReturn(List.of(group1));

        Question q1 = Question.builder().id(1000L).questionNumber(1).questionGroup(group1).build();
        when(questionRepository.findAllByQuestionGroupIdOrderByQuestionNumberAsc(100L))
                .thenReturn(List.of(q1));

        MediaAsset audioAsset = MediaAsset.builder().id(201L).label("p01-q001").mediaType("audio").build();
        MediaAsset imageAsset = MediaAsset.builder().id(202L).label("p01-q001").mediaType("image").build();

        when(mediaAssetRepository.findByTestIdAndLabelAndMediaType(eq(testId), eq("p01-q001"), eq("audio")))
                .thenReturn(Optional.of(audioAsset));
        when(mediaAssetRepository.findByTestIdAndLabelAndMediaType(eq(testId), eq("p01-q001"), eq("image")))
                .thenReturn(Optional.of(imageAsset));

        when(questionGroupImageRepository.findAllByQuestionGroupIdOrderByOrderIndexAsc(100L))
                .thenReturn(List.of());
        when(questionGroupAudioRepository.findByQuestionGroupIdAndOrderIndex(100L, 0))
                .thenReturn(Optional.empty());

        MediaAutoLinkResponse response = mediaAssetService.autoLinkMedia(testId);

        assertThat(response.getLinkedAudios()).isEqualTo(1);
        assertThat(response.getLinkedImages()).isEqualTo(1);
        assertThat(response.getTotalGroups()).isEqualTo(1);

        verify(questionGroupAudioRepository).save(any());
        verify(questionGroupImageRepository).save(any());
    }
}
