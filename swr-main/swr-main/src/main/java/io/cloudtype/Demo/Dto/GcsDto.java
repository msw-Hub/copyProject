package io.cloudtype.Demo.Dto;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class GcsDto {
    private String name; // 업로드할 파일의 이름
    private MultipartFile file; // 업로드할 파일 데이터
}
