package edu.pnu.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class Epc {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long epcId; // epcCode가 길어서 auto-increment가 유리
	
	private String epcCode;
	
	private String epcHeader;
	
	private String epcLot;
	
	private String epcSerial;

	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "location_id")
	private CsvLocation csvLocation;
	
	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "product_id", referencedColumnName = "product_id")
	private CsvProduct csvProduct;
	
	private LocalDateTime manufactureDate;
	private LocalDate expiryDate;
}
