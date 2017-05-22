/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package actors;

import com.google.common.io.Files;
import java.util.List;
import metadata.etl.models.EtlJobName;
import org.junit.Test;
import wherehows.common.Constant;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.fest.assertions.api.Assertions.*;

public class ConfigUtilTest {

  @Test
  public void shouldGenerateEtlJobDefaultCommand() {
    // when:
    ProcessBuilder pb =
        ConfigUtil.buildProcess(EtlJobName.HADOOP_DATASET_METADATA_ETL, 0L, null, new Properties());

    // then:
    assertThat(pb.command()).contains("java", "-cp", System.getProperty("java.class.path"),
        "-Dconfig=/var/tmp/wherehows/exec/0.properties", "-DCONTEXT=HADOOP_DATASET_METADATA_ETL",
        "-Dlogback.configurationFile=etl_logback.xml", "metadata.etl.Launcher");
  }

  @Test
  public void shouldGenerateEtlJobCommandWithConfiguredDirectory() {
    final String applicationDirectory = "./temp";

    // given:
    Properties etlJobProperties = new Properties();
    etlJobProperties.put(Constant.WH_APP_FOLDER_KEY, applicationDirectory);

    // when:
    ProcessBuilder pb = ConfigUtil.buildProcess(EtlJobName.LDAP_USER_ETL, 1L, " -a -b  ", etlJobProperties);

    // then:
    assertThat(pb.command()).contains("java", "-a", "-b", "-cp", System.getProperty("java.class.path"),
        "-Dconfig=" + applicationDirectory + "/exec/1.properties", "-DCONTEXT=LDAP_USER_ETL",
        "-DLOG_DIR=" + applicationDirectory, "-Dlogback.configurationFile=etl_logback.xml",
        "metadata.etl.Launcher");
    assertThat(pb.redirectError().file().getPath().equals("./temp/LDAP_USER_ETL.stderr"));
    assertThat(pb.redirectOutput().file().getPath().equals("./temp/LDAP_USER_ETL.stdout"));
  }

  @Test
  public void shouldGeneratePropertiesWithValues() throws IOException {
    final long whEtlExecId = System.currentTimeMillis();
    final Properties etlJobProperties = new Properties();
    etlJobProperties.put("p1", "v1");
    etlJobProperties.put("p2", "v2");
    etlJobProperties.put("p3", "v3");

    final File propertiesFile = createTemporaryPropertiesFile(whEtlExecId, etlJobProperties);

    // when:
    ConfigUtil.generateProperties(EtlJobName.HIVE_DATASET_METADATA_ETL, 2, whEtlExecId, etlJobProperties);

    // then:
    final String content = Files.toString(propertiesFile, Charset.defaultCharset());
    assertThat(content)
            .contains("p1=v1")
            .contains("p2=v2")
            .contains("p3=v3");
  }

  @Test
  public void shouldGeneratePropertiesFileAndDeleteIt() throws IOException {
    final long whEtlExecId = System.currentTimeMillis();
    final Properties etlJobProperties = new Properties();

    final File propertiesFile = createTemporaryPropertiesFile(whEtlExecId, etlJobProperties);

    // expect:
    assertThat(propertiesFile).doesNotExist();

    // when:
    final EtlJobName etlJobName = EtlJobName.valueOf("AZKABAN_EXECUTION_METADATA_ETL");
    ConfigUtil.generateProperties(etlJobName, 2, whEtlExecId, etlJobProperties);

    // then:
    assertThat(propertiesFile).exists();

    // when:
    ConfigUtil.deletePropertiesFile(etlJobProperties, whEtlExecId);

    // then:
    assertThat(propertiesFile).doesNotExist();
  }

  private File createTemporaryPropertiesFile(long whEtlExecId, Properties etlJobProperties) {
    final File tempDir = new File(Files.createTempDir(), "non-exsiting-dir");
    tempDir.deleteOnExit();
    final String tempDirPath = tempDir.getAbsolutePath();

    etlJobProperties.put(Constant.WH_APP_FOLDER_KEY, tempDirPath);

    return new File(tempDirPath + "/exec", whEtlExecId + ".properties");
  }
}