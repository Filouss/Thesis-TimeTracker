package cz.cvut.fel.thesis.dto;

import java.util.List;

public record ExportDTO(
    List<ExportItem> exportItems) {
    
}
