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
@Table(name ="csvlocation")
public class CsvLocation {
	@Id
	private Long locationId; //location_id
	
	private String scanLocation; //location name
	
	private Long operatorId; // 작업자 ID
	private Long deviceId; // 기기 ID
}
