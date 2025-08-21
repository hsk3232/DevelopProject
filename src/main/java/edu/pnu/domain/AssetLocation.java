package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Table(name ="assetlocation")
public class AssetLocation {
	@Id
	private Long locationId; //location_id
	
	private String scanLocation; //location name
	private double latitude; // 위도
	private double longitude; // 경도

}
