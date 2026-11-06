package com.nhuhuy05.enghub.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaAutoLinkResponse {
    @JsonProperty("linked_audios")
    int linkedAudios;

    @JsonProperty("linked_images")
    int linkedImages;

    @JsonProperty("linked_passages")
    int linkedPassages;

    @JsonProperty("total_groups")
    int totalGroups;
}
