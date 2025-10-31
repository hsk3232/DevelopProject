package edu.pnu.domain;

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
public class AssetRoute {
	 @Id 
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long roadId;
	    private String fromScanLocation;
	    private String toScanLocation;
	    // 🔹 출발지 연관 매핑
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "from_location_id")
	    private AssetLocation fromLocationId;

	    // 🔹 도착지 연관 매핑
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "to_location_id")
	    private AssetLocation toLocationId;

	    private String fromBusinessStep;
	    private String toBusinessStep;
	    private String fromEventType;
	    private String toEventType;
}
