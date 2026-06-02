package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CanvasEvent {
    private String type;
    private String color;
    private String lineWidth;
    private Point from;
    private Point to;
    private FillColors fillColors;
}
