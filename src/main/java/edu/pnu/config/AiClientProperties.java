package edu.pnu.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.client")
public class AiClientProperties {
    @Min(1)
    private int batchSize;          // properties에 반드시 1 이상 지정
    @Min(0)
    private int retryMaxAttempts;   // 0 이상
    @Min(0)
    private long retryDelayMs;
    @Min(0)
    private long batchDelayMs;
    @Min(1)
    private int restConnectTimeout;
    @Min(1)
    private int restReadTimeout;
    @NotBlank
    private String aiApiUrl;
}
