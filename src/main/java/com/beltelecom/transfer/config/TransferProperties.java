package com.beltelecom.transfer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "transfer")
public class TransferProperties {

    private String inputDirectory;
    private String processedDirectory;
    private String errorDirectory;
    private String reportSuffix = "r";
    private String fileExtension = ".045";
    private String dataFilePrefix = "epb";
    private int defaultCustCode = 1;
    private int defaultTypeServA = 326;
    private int defaultCodOper = 45;
    private int summaScale = 4;
    private Scheduled scheduled = new Scheduled();

    @Getter
    @Setter
    public static class Scheduled {
        private boolean enabled = false;
        private String cron = "0 */5 * * * *";
    }
}
