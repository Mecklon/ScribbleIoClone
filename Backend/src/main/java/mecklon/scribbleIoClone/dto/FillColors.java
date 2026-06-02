package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FillColors {
    private Integer[] oldColor;
    private Integer[] newColor;

}
