package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.cvut.fel.thesis.model.Label;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LabelDTO(
        Long id,
        String name,
        String color
) {
    public static LabelDTO fromEntity(Label label) {
        return new LabelDTO(
                label.getId(),
                label.getTitle(),
                label.getColorHEX()
        );
    }
}
