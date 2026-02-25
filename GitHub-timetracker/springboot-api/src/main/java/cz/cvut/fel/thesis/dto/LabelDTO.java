package cz.cvut.fel.thesis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LabelDTO(
        Long id,
        String name,
        String color
) { }
