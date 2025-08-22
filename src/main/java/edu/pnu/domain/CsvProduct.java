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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Table(name ="csvproduct", uniqueConstraints = {
	    @UniqueConstraint(
	    	      name="uq_csvProduct",
	    	      columnNames = {"file_id", "epc_company", "epc_product", "product_name"}) // ★ 파일 내 원문 조합 유니크    	  
	  })
public class CsvProduct {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
	private String epcProduct; // 상품코드
	private String epcCompany; // 제조사 코드
	private String productName; // 상품명
	
	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "file_id")
	private Csv csv;
}
