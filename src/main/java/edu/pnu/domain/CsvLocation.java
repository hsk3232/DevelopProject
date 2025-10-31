package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(uniqueConstraints = {
	    @UniqueConstraint(
	    		name="uq_csvLocation_fileId_locationId", 
	    		columnNames = {"file_id", "csv_location_id"} // 파일 내 동일 위치명 중복 금지
	    		)
	    }) 
public class CsvLocation {
	@Id
	@Column(name="csv_location_id", nullable=false)
	private Long csvLocationId; //location_id
	
	private String scanLocation; //location name
	
	private Long operatorId; // 작업자 ID
	private Long deviceId; // 기기 ID
	
	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "file_id")
	private CsvFile csvFile;
}
