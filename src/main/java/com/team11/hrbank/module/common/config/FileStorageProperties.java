package com.team11.hrbank.module.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {
  private String rootPath;
  private String backupFiles;
  private String errorLogs;
}
