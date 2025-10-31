package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "csvFile") // LAZY 순환/로그폭탄 방지
@Entity
@Builder
@Table(name = "epc",
        uniqueConstraints = {
		@UniqueConstraint(name="uq_epc_file_id_epc_code", columnNames={"file_id","epc_code"}), // Prevent duplication
	    @UniqueConstraint(name="uq_epc_file_id_epc_id",   columnNames={"file_id","epc_id"}) // EventHistory 복합 FK 타깃
  })
public class Epc {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "epc_id") // UniqueConstraint와 컬럼명 일치
	private Long epcId; // epcCode가 길어서 auto-increment가 유리
	
	@Column(name = "epc_code", nullable = false, length = 64) // UniqueConstraint와 컬럼명 일치
	private String epcCode;
	
	private String epcHeader;
	
	private String epcLot;
	
	private String epcSerial;

	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "file_id", nullable = false)
	private CsvFile csvFile;
	
	private LocalDateTime manufactureDate;
	private LocalDate expiryDate;
}
