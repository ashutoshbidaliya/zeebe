/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.exporter;

import static io.zeebe.exporter.ElasticsearchExporter.ZEEBE_RECORD_TEMPLATE_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.zeebe.exporter.context.Configuration;
import io.zeebe.exporter.context.Context;
import io.zeebe.exporter.context.Controller;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.RecordMetadata;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.util.ZbLogger;
import java.time.Duration;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class ElasticsearchExporterTest {

  private ElasticsearchExporterConfiguration config;
  private ElasticsearchClient esClient;

  private long lastExportedRecordPosition;

  @Before
  public void setUp() {
    config = new ElasticsearchExporterConfiguration();
    esClient = mockElasticsearchClient();
  }

  @Test
  public void shouldCreateIndexTemplates() {
    // given
    config.index.prefix = "foo-bar";
    config.index.createTemplate = true;
    config.index.deployment = true;
    config.index.incident = true;
    config.index.job = true;
    config.index.jobBatch = true;
    config.index.message = true;
    config.index.messageSubscription = true;
    config.index.raft = true;
    config.index.workflowInstance = true;
    config.index.workflowInstanceSubscription = true;

    // when
    createExporter(config);

    // then
    verify(esClient).putIndexTemplate("foo-bar", ZEEBE_RECORD_TEMPLATE_JSON);

    verify(esClient).putIndexTemplate(ValueType.DEPLOYMENT);
    verify(esClient).putIndexTemplate(ValueType.INCIDENT);
    verify(esClient).putIndexTemplate(ValueType.JOB);
    verify(esClient).putIndexTemplate(ValueType.JOB_BATCH);
    verify(esClient).putIndexTemplate(ValueType.MESSAGE);
    verify(esClient).putIndexTemplate(ValueType.MESSAGE_SUBSCRIPTION);
    verify(esClient).putIndexTemplate(ValueType.RAFT);
    verify(esClient).putIndexTemplate(ValueType.WORKFLOW_INSTANCE);
    verify(esClient).putIndexTemplate(ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION);
  }

  @Test
  public void shouldExportEnabledValueTypes() {
    // given
    config.index.event = true;
    config.index.deployment = true;
    config.index.incident = true;
    config.index.job = true;
    config.index.jobBatch = true;
    config.index.message = true;
    config.index.messageSubscription = true;
    config.index.raft = true;
    config.index.workflowInstance = true;
    config.index.workflowInstanceSubscription = true;

    final ElasticsearchExporter exporter = createExporter(config);

    final ValueType[] valueTypes =
        new ValueType[] {
          ValueType.DEPLOYMENT,
          ValueType.INCIDENT,
          ValueType.JOB,
          ValueType.JOB_BATCH,
          ValueType.MESSAGE,
          ValueType.MESSAGE_SUBSCRIPTION,
          ValueType.RAFT,
          ValueType.WORKFLOW_INSTANCE,
          ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION
        };

    // when - then
    for (ValueType valueType : valueTypes) {
      final Record record = mockRecord(valueType, RecordType.EVENT);
      exporter.export(record);
      verify(esClient).index(record);
    }
  }

  @Test
  public void shouldNotExportDisabledValueTypes() {
    // given
    config.index.event = true;
    config.index.deployment = false;
    config.index.incident = false;
    config.index.job = false;
    config.index.jobBatch = false;
    config.index.message = false;
    config.index.messageSubscription = false;
    config.index.raft = false;
    config.index.workflowInstance = false;
    config.index.workflowInstanceSubscription = false;

    final ElasticsearchExporter exporter = createExporter(config);

    final ValueType[] valueTypes =
        new ValueType[] {
          ValueType.DEPLOYMENT,
          ValueType.INCIDENT,
          ValueType.JOB,
          ValueType.JOB_BATCH,
          ValueType.MESSAGE,
          ValueType.MESSAGE_SUBSCRIPTION,
          ValueType.RAFT,
          ValueType.WORKFLOW_INSTANCE,
          ValueType.WORKFLOW_INSTANCE_SUBSCRIPTION
        };

    // when - then
    for (ValueType valueType : valueTypes) {
      final Record record = mockRecord(valueType, RecordType.EVENT);
      exporter.export(record);
      verify(esClient, never()).index(record);
    }
  }

  @Test
  public void shouldIgnoreUnknownValueType() {
    // given
    config.index.event = true;
    final ElasticsearchExporter exporter = createExporter(config);
    final Record record = mockRecord(ValueType.SBE_UNKNOWN, RecordType.EVENT);

    // when
    exporter.export(record);

    // then
    verify(esClient, never()).index(record);
  }

  @Test
  public void shouldExportEnabledRecordTypes() {
    // given
    config.index.command = true;
    config.index.event = true;
    config.index.rejection = true;
    config.index.deployment = true;

    final ElasticsearchExporter exporter = createExporter(config);

    final RecordType[] recordTypes =
        new RecordType[] {RecordType.COMMAND, RecordType.EVENT, RecordType.COMMAND_REJECTION};

    // when - then
    for (RecordType recordType : recordTypes) {
      final Record record = mockRecord(ValueType.DEPLOYMENT, recordType);
      exporter.export(record);
      verify(esClient).index(record);
    }
  }

  @Test
  public void shouldNotExportDisabledRecordTypes() {
    // given
    config.index.command = false;
    config.index.event = false;
    config.index.rejection = false;
    config.index.deployment = true;

    final ElasticsearchExporter exporter = createExporter(config);

    final RecordType[] recordTypes =
        new RecordType[] {RecordType.COMMAND, RecordType.EVENT, RecordType.COMMAND_REJECTION};

    // when - then
    for (RecordType recordType : recordTypes) {
      final Record record = mockRecord(ValueType.DEPLOYMENT, recordType);
      exporter.export(record);
      verify(esClient, never()).index(record);
    }
  }

  @Test
  public void shouldIgnoreUnknownRecordType() {
    // given
    config.index.deployment = true;
    final ElasticsearchExporter exporter = createExporter(config);
    final Record record = mockRecord(ValueType.DEPLOYMENT, RecordType.SBE_UNKNOWN);

    // when
    exporter.export(record);

    // then
    verify(esClient, never()).index(record);
  }

  @Test
  public void shouldUpdateLastPositionOnFlush() {
    // given
    final ElasticsearchExporter exporter = createExporter(config);
    when(esClient.shouldFlush()).thenReturn(true);

    final long position = 1234L;
    final Record record = mockRecord(ValueType.WORKFLOW_INSTANCE, RecordType.EVENT);
    when(record.getPosition()).thenReturn(position);

    // when
    exporter.export(record);

    // then
    assertThat(lastExportedRecordPosition).isEqualTo(position);
  }

  @Test
  public void shouldFlushOnClose() {
    // given
    final ElasticsearchExporter exporter = createExporter(config);

    // when
    exporter.close();

    // then
    verify(esClient).flush();
  }

  private ElasticsearchExporter createExporter(
      final ElasticsearchExporterConfiguration configuration) {
    final ElasticsearchExporter exporter =
        new ElasticsearchExporter() {
          @Override
          protected ElasticsearchClient createClient() {
            return esClient;
          }
        };
    exporter.configure(createContext(configuration));
    exporter.open(createController());
    return exporter;
  }

  private Context createContext(final ElasticsearchExporterConfiguration configuration) {
    return new Context() {
      @Override
      public Logger getLogger() {
        return new ZbLogger("io.zeebe.exporter.elasticsearch");
      }

      @Override
      public Configuration getConfiguration() {
        return new Configuration() {
          @Override
          public String getId() {
            return "elasticsearch";
          }

          @Override
          public Map<String, Object> getArguments() {
            throw new UnsupportedOperationException("not supported in test case");
          }

          @Override
          @SuppressWarnings("unchecked")
          public <T> T instantiate(Class<T> configClass) {
            return (T) configuration;
          }
        };
      }
    };
  }

  private Controller createController() {
    return new Controller() {
      @Override
      public void updateLastExportedRecordPosition(long position) {
        lastExportedRecordPosition = position;
      }

      @Override
      public void scheduleTask(Duration delay, Runnable task) {
        // ignore
      }
    };
  }

  private ElasticsearchClient mockElasticsearchClient() {
    final ElasticsearchClient client = mock(ElasticsearchClient.class);
    when(client.flush()).thenReturn(true);
    when(client.putIndexTemplate(any(ValueType.class))).thenReturn(true);
    when(client.putIndexTemplate(anyString(), anyString())).thenReturn(true);
    return client;
  }

  private Record mockRecord(final ValueType valueType, final RecordType recordType) {
    final RecordMetadata metadata = mock(RecordMetadata.class);
    when(metadata.getValueType()).thenReturn(valueType);
    when(metadata.getRecordType()).thenReturn(recordType);

    final Record record = mock(Record.class);
    when(record.getMetadata()).thenReturn(metadata);

    return record;
  }
}
