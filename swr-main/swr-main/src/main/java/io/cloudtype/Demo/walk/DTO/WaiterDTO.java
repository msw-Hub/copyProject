package io.cloudtype.Demo.walk.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WaiterDTO {
    private int id; //waiterList의 id
    private int userId;
    private String name;
    private String profileImage;
    private double rating;
    private int reviewCount;
}
