package org.camunda.optimize.service.util.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.ReportType;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;

import java.io.IOException;

import static org.camunda.optimize.dto.optimize.query.report.ReportType.DECISION;
import static org.camunda.optimize.dto.optimize.query.report.ReportType.PROCESS;
import static org.camunda.optimize.dto.optimize.query.report.ReportType.valueOf;
import static org.camunda.optimize.service.es.schema.type.AbstractReportType.COMBINED;
import static org.camunda.optimize.service.es.schema.type.AbstractReportType.REPORT_TYPE;

public class CustomReportDefinitionDeserializer extends StdDeserializer<ReportDefinitionDto> {

  private ObjectMapper objectMapper;

  public CustomReportDefinitionDeserializer(Class<?> vc, ObjectMapper objectMapper) {
    super(vc);
    this.objectMapper = objectMapper;
  }

  public CustomReportDefinitionDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ReportDefinitionDto deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    ensureCombinedReportFieldIsProvided(jp, node);
    ensureReportTypeFieldIsProvided(jp, node);

    boolean isCombined = node.get(COMBINED).booleanValue();
    String reportTypeAsString = node.get(REPORT_TYPE).asText();
    ReportType reportType = valueOf(reportTypeAsString.toUpperCase());

    String json = node.toString();
    if (isCombined) {
      return objectMapper.readValue(json, CombinedReportDefinitionDto.class);
    } else {
      if (reportType.equals(PROCESS)) {
        return objectMapper.readValue(json, SingleProcessReportDefinitionDto.class);
      } else if (reportType.equals(DECISION)) {
        return objectMapper.readValue(json, SingleDecisionReportDefinitionDto.class);
      }
    }
    String errorMessage = String.format(
      "Could not create report definition since the report " +
        "with type [%s] and is combined [%s] is unknown", reportTypeAsString, isCombined);
    throw new JsonParseException(jp, "Could not create report definition since the t");
  }

  private void ensureCombinedReportFieldIsProvided(JsonParser jp, JsonNode node) throws JsonParseException {
    if (!node.hasNonNull(COMBINED)) {
      throw new JsonParseException(jp, "Could not create report definition since no combined field was provided!");
    }
  }

  private void ensureReportTypeFieldIsProvided(JsonParser jp, JsonNode node) throws JsonParseException {
    if (!node.hasNonNull(REPORT_TYPE)) {
      throw new JsonParseException(jp, "Could not create report definition since no report type field was provided!");
    }
  }
}
