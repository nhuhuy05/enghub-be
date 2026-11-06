package com.nhuhuy05.enghub.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.config.CloudinaryProperties;
import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import com.nhuhuy05.enghub.listening.repository.QuestionGroupAudioRepository;
import com.nhuhuy05.enghub.media.dto.MediaAssetResponse;
import com.nhuhuy05.enghub.media.dto.MediaAutoLinkResponse;
import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.media.repository.MediaAssetRepository;
import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import com.nhuhuy05.enghub.reading.repository.QuestionGroupPassageRepository;
import com.nhuhuy05.enghub.test.entity.Question;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import com.nhuhuy05.enghub.test.entity.QuestionGroupImage;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.repository.QuestionGroupImageRepository;
import com.nhuhuy05.enghub.test.repository.QuestionGroupRepository;
import com.nhuhuy05.enghub.test.repository.QuestionRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MediaAssetService {
    Cloudinary cloudinary;
    CloudinaryProperties cloudinaryProperties;
    MediaAssetRepository mediaAssetRepository;
    TestRepository testRepository;
    QuestionGroupImageRepository questionGroupImageRepository;
    QuestionGroupAudioRepository questionGroupAudioRepository;
    QuestionGroupPassageRepository questionGroupPassageRepository;
    QuestionGroupRepository questionGroupRepository;
    QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getMedia(Long testId) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        return mediaAssetRepository.findAllByTestIdOrderByCreatedAtAsc(testId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MediaAssetResponse uploadMedia(Long testId, MultipartFile file, String label, String mediaType) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        String normalizedType = normalizeMediaType(mediaType);
        validateFile(file, normalizedType);

        String normalizedLabel = label == null ? "" : label.trim();
        if (normalizedLabel.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (mediaAssetRepository.existsByTestIdAndLabelAndMediaType(testId, normalizedLabel, normalizedType)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_EXISTED);
        }

        String folder = buildAssetFolder(testId, normalizedType);
        String publicId = buildPublicId(folder, normalizedLabel);
        Map uploadResult = uploadToCloudinary(file, publicId, folder);

        MediaAsset mediaAsset = MediaAsset.builder()
                .test(test)
                .label(normalizedLabel)
                .mediaType(normalizedType)
                .cloudinaryPublicId(String.valueOf(uploadResult.get("public_id")))
                .url(String.valueOf(uploadResult.get("secure_url")))
                .durationMs(resolveDurationMs(uploadResult))
                .originalFilename(file.getOriginalFilename())
                .build();

        MediaAsset saved = mediaAssetRepository.save(mediaAsset);
        tryAutoLink(testId);
        return toResponse(saved);
    }

    @Transactional
    public MediaAssetResponse replaceMedia(Long testId, Long mediaAssetId, MultipartFile file) {
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndTestId(mediaAssetId, testId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));

        validateFile(file, mediaAsset.getMediaType());

        String folder = extractFolder(mediaAsset.getCloudinaryPublicId());
        Map uploadResult = uploadToCloudinary(file, mediaAsset.getCloudinaryPublicId(), folder, true);
        mediaAsset.setUrl(String.valueOf(uploadResult.get("secure_url")));
        mediaAsset.setDurationMs(resolveDurationMs(uploadResult));
        mediaAsset.setOriginalFilename(file.getOriginalFilename());

        MediaAsset saved = mediaAssetRepository.save(mediaAsset);
        tryAutoLink(testId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteMedia(Long testId, Long mediaAssetId) {
        MediaAsset mediaAsset = mediaAssetRepository.findByIdAndTestId(mediaAssetId, testId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_ASSET_NOT_EXISTED));

        ensureMediaAssetNotInUse(mediaAssetId);
        mediaAssetRepository.delete(mediaAsset);
        mediaAssetRepository.flush();
        destroyCloudinaryAsset(mediaAsset);
    }

    private void ensureMediaAssetNotInUse(Long mediaAssetId) {
        if (questionGroupImageRepository.existsByMediaAssetId(mediaAssetId)
                || questionGroupPassageRepository.existsByMediaAssetId(mediaAssetId)
                || questionGroupAudioRepository.existsByMediaAssetId(mediaAssetId)) {
            throw new AppException(ErrorCode.MEDIA_ASSET_IN_USE);
        }
    }

    private Map uploadToCloudinary(MultipartFile file, String publicId, String folder) {
        return uploadToCloudinary(file, publicId, folder, false);
    }

    private Map uploadToCloudinary(MultipartFile file, String publicId, String folder, boolean overwrite) {
        try {
            return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "asset_folder", folder,
                    "public_id", publicId,
                    "overwrite", overwrite,
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary upload failed for publicId={}", publicId, exception);
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private void destroyCloudinaryAsset(MediaAsset mediaAsset) {
        try {
            cloudinary.uploader().destroy(mediaAsset.getCloudinaryPublicId(), ObjectUtils.asMap(
                    "resource_type", cloudinaryResourceType(mediaAsset.getMediaType()),
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary destroy failed for publicId={}", mediaAsset.getCloudinaryPublicId(), exception);
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private String cloudinaryResourceType(String mediaType) {
        return "audio".equals(mediaType) ? "video" : "image";
    }

    private String buildAssetFolder(Long testId, String mediaType) {
        return String.format("%s/%s/tests/%d/%s",
                trimSlashes(cloudinaryProperties.getFolderRoot()),
                trimSlashes(cloudinaryProperties.getFolderEnv()),
                testId,
                cloudinaryFolderName(mediaType));
    }

    private String buildPublicId(String folder, String label) {
        return folder + "/" + sanitizeLabel(label);
    }

    private String cloudinaryFolderName(String mediaType) {
        return "audio".equals(mediaType) ? "audios" : "images";
    }

    private String extractFolder(String publicId) {
        int slashIndex = publicId == null ? -1 : publicId.lastIndexOf('/');
        if (slashIndex <= 0) {
            return trimSlashes(cloudinaryProperties.getFolderRoot());
        }
        return publicId.substring(0, slashIndex);
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        String normalized = mediaType.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("image") && !normalized.equals("audio")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        return normalized;
    }

    private void validateFile(MultipartFile file, String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        if (mediaType.equals("image") && !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        if (mediaType.equals("audio") && !contentType.startsWith("audio/")) {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
    }

    private Integer resolveDurationMs(Map uploadResult) {
        Object duration = uploadResult.get("duration");
        if (duration instanceof Number number) {
            return (int) Math.round(number.doubleValue() * 1000);
        }
        return null;
    }

    private String sanitizeLabel(String label) {
        return label.trim()
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            return "enghub";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private void tryAutoLink(Long testId) {
        try {
            autoLinkMedia(testId);
        } catch (Exception exception) {
            log.warn("Auto-linking media for testId={} skipped or encountered warning: {}", testId, exception.getMessage());
        }
    }

    @Transactional
    public MediaAutoLinkResponse autoLinkMedia(Long testId) {
        testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        List<QuestionGroup> groups = questionGroupRepository
                .findAllByTestPartTestIdOrderByTestPartPartNumberAscOrderIndexAsc(testId);

        if (groups.isEmpty()) {
            return MediaAutoLinkResponse.builder()
                    .linkedAudios(0)
                    .linkedImages(0)
                    .linkedPassages(0)
                    .totalGroups(0)
                    .build();
        }

        int linkedAudios = 0;
        int linkedImages = 0;
        int linkedPassages = 0;

        for (QuestionGroup group : groups) {
            List<Question> questions = questionRepository
                    .findAllByQuestionGroupIdOrderByQuestionNumberAsc(group.getId());
            if (questions.isEmpty()) {
                continue;
            }

            int part = group.getTestPart().getPartNumber();
            int start = questions.get(0).getQuestionNumber();
            int end = questions.get(questions.size() - 1).getQuestionNumber();
            List<String> baseLabels = buildGroupLabelCandidates(part, start, end, group.getOrderIndex());

            // 1. Part 1, 3, 4 images (graphics)
            if (part == 1 || part == 3 || part == 4) {
                List<QuestionGroupImage> existingImages = questionGroupImageRepository
                        .findAllByQuestionGroupIdOrderByOrderIndexAsc(group.getId());
                if (existingImages.isEmpty()) {
                    Optional<MediaAsset> image = findMedia(testId, "image", baseLabels);
                    if (image.isPresent()) {
                        questionGroupImageRepository.save(QuestionGroupImage.builder()
                                .questionGroup(group)
                                .mediaAsset(image.get())
                                .orderIndex(0)
                                .build());
                        linkedImages++;
                    }
                }
            }

            // 2. Part 1, 2, 3, 4 audios
            if (part >= 1 && part <= 4) {
                Optional<QuestionGroupAudio> existingAudio = questionGroupAudioRepository
                        .findByQuestionGroupIdAndOrderIndex(group.getId(), 0);
                if (existingAudio.isEmpty()) {
                    List<String> audioLabels = new ArrayList<>(baseLabels);
                    audioLabels.add("audio_main");
                    Optional<MediaAsset> audio = findMedia(testId, "audio", audioLabels);
                    if (audio.isPresent()) {
                        questionGroupAudioRepository.save(QuestionGroupAudio.builder()
                                .questionGroup(group)
                                .mediaAsset(audio.get())
                                .startMs(0)
                                .endMs(null)
                                .orderIndex(0)
                                .build());
                        linkedAudios++;
                    }
                }
            }

            // 3. Part 6, 7 passages
            if (part == 6 || part == 7) {
                List<QuestionGroupPassage> existingPassages = questionGroupPassageRepository
                        .findAllByQuestionGroupIdOrderByOrderIndexAsc(group.getId());

                List<MediaAsset> pageImages = new ArrayList<>();
                for (int page = 1; page <= 20; page++) {
                    Optional<MediaAsset> pageImage = findPageMedia(testId, buildPageLabelCandidates(baseLabels, page));
                    if (pageImage.isEmpty()) {
                        break;
                    }
                    pageImages.add(pageImage.get());
                }

                if (pageImages.isEmpty()) {
                    findMedia(testId, "image", baseLabels).ifPresent(pageImages::add);
                }

                if (!pageImages.isEmpty()) {
                    if (existingPassages.isEmpty()) {
                        for (int index = 0; index < pageImages.size(); index++) {
                            MediaAsset image = pageImages.get(index);
                            questionGroupPassageRepository.save(QuestionGroupPassage.builder()
                                    .questionGroup(group)
                                    .title(extractPassageTitle(image.getLabel()))
                                    .passageType("image")
                                    .contentFormat("image")
                                    .mediaAsset(image)
                                    .orderIndex(index)
                                    .build());
                            linkedPassages++;
                        }
                    } else {
                        Set<Long> attachedMediaIds = existingPassages.stream()
                                .map(QuestionGroupPassage::getMediaAsset)
                                .filter(Objects::nonNull)
                                .map(MediaAsset::getId)
                                .collect(Collectors.toSet());

                        int nextOrder = existingPassages.size();
                        for (MediaAsset image : pageImages) {
                            if (!attachedMediaIds.contains(image.getId())) {
                                questionGroupPassageRepository.save(QuestionGroupPassage.builder()
                                        .questionGroup(group)
                                        .title(extractPassageTitle(image.getLabel()))
                                        .passageType("image")
                                        .contentFormat("image")
                                        .mediaAsset(image)
                                        .orderIndex(nextOrder++)
                                        .build());
                                linkedPassages++;
                            }
                        }
                    }
                }
            }
        }

        return MediaAutoLinkResponse.builder()
                .linkedAudios(linkedAudios)
                .linkedImages(linkedImages)
                .linkedPassages(linkedPassages)
                .totalGroups(groups.size())
                .build();
    }

    private List<String> buildGroupLabelCandidates(int part, int start, int end, int groupOrder) {
        List<String> labels = new ArrayList<>();
        labels.add(canonicalLabel(part, start, end));
        labels.add(plainRangeLabel(start, end));
        return labels;
    }

    private List<String> buildPageLabelCandidates(List<String> baseLabels, int page) {
        List<String> labels = new ArrayList<>();
        for (String baseLabel : baseLabels) {
            labels.add(baseLabel + "-" + String.format("%02d", page));
            labels.add(baseLabel + "-" + page);
            labels.add(baseLabel + "(" + page + ")");
        }
        return labels;
    }

    private String canonicalLabel(int part, int start, int end) {
        if (start == end) {
            return String.format("p%02d-q%03d", part, start);
        }
        return String.format("p%02d-q%03d-%03d", part, start, end);
    }

    private String plainRangeLabel(int start, int end) {
        if (start == end) {
            return String.valueOf(start);
        }
        return start + "-" + end;
    }

    private Optional<MediaAsset> findMedia(Long testId, String mediaType, List<String> labels) {
        return labels.stream()
                .filter(label -> label != null && !label.isBlank())
                .distinct()
                .map(label -> mediaAssetRepository.findByTestIdAndLabelAndMediaType(testId, label, mediaType))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<MediaAsset> findPageMedia(Long testId, List<String> labels) {
        Optional<MediaAsset> exactMatch = findMedia(testId, "image", labels);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        return labels.stream()
                .filter(label -> label != null && !label.isBlank())
                .map(label -> label + "_")
                .distinct()
                .map(prefix -> mediaAssetRepository
                        .findFirstByTestIdAndMediaTypeAndLabelStartingWithOrderByLabelAsc(testId, "image", prefix))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private String extractPassageTitle(String label) {
        if (label == null || label.isBlank()) {
            return label;
        }

        int titleStart = label.indexOf('_');
        if (titleStart < 0 || titleStart == label.length() - 1) {
            return label;
        }

        String title = label.substring(titleStart + 1)
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (title.isEmpty()) {
            return label;
        }

        return title;
    }

    private MediaAssetResponse toResponse(MediaAsset mediaAsset) {
        return MediaAssetResponse.builder()
                .id(mediaAsset.getId())
                .testId(mediaAsset.getTest().getId())
                .label(mediaAsset.getLabel())
                .mediaType(mediaAsset.getMediaType())
                .cloudinaryPublicId(mediaAsset.getCloudinaryPublicId())
                .url(mediaAsset.getUrl())
                .durationMs(mediaAsset.getDurationMs())
                .originalFilename(mediaAsset.getOriginalFilename())
                .createdAt(mediaAsset.getCreatedAt())
                .build();
    }
}
