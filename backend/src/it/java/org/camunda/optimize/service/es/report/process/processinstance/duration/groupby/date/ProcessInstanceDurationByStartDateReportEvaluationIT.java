/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.processinstance.duration.groupby.date;

import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterStartDto;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterUnit;
import org.camunda.optimize.dto.optimize.query.report.single.group.GroupByDateUnit;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.StartDateFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.duration.ProcessDurationReportMapResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.junit.Test;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.camunda.optimize.test.util.DateModificationHelper.truncateToStartOfUnit;
import static org.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static org.camunda.optimize.test.util.ProcessReportDataType.PROC_INST_DUR_GROUP_BY_START_DATE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class ProcessInstanceDurationByStartDateReportEvaluationIT
  extends AbstractProcessInstanceDurationByDateReportEvaluationIT {

  @Override
  protected ProcessReportDataType getTestReportDataType() {
    return ProcessReportDataType.PROC_INST_DUR_GROUP_BY_START_DATE;
  }

  @Override
  protected ProcessGroupByType getGroupByType() {
    return ProcessGroupByType.START_DATE;
  }


  @Override
  protected void adjustProcessInstanceDates(String processInstanceId,
                                            OffsetDateTime refDate,
                                            long daysToShift,
                                            long durationInSec) {
    OffsetDateTime shiftedEndDate = refDate.plusDays(daysToShift);
    try {
      engineDatabaseRule.changeProcessInstanceStartDate(processInstanceId, shiftedEndDate.minusSeconds(durationInSec));
      engineDatabaseRule.changeProcessInstanceEndDate(processInstanceId, shiftedEndDate);
    } catch (SQLException e) {
      throw new OptimizeIntegrationTestException("Failed adjusting process instance dates", e);
    }
  }


  @Test
  public void processInstancesStartedAtSameIntervalAreGroupedTogether() {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
    String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();

    adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 1L);
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 9L);
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    adjustProcessInstanceDates(processInstanceDto.getId(), startDate, 0L, 2L);
    ProcessInstanceEngineDto processInstanceDto3 =
      engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    adjustProcessInstanceDates(processInstanceDto3.getId(), startDate, -1L, 1L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder.createReportData()
      .setReportDataType(PROC_INST_DUR_GROUP_BY_START_DATE)
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(processDefinitionVersion)
      .setDateInterval(GroupByDateUnit.DAY)
      .build();

    ProcessDurationReportMapResultDto result = evaluateDurationMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    ZonedDateTime startOfToday = truncateToStartOfUnit(startDate, ChronoUnit.DAYS);
    assertThat(resultData.get(0).getKey(), is(localDateTimeToString(startOfToday)));
    assertThat(
      resultData.get(0).getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(1000L, 9000L, 2000L))
    );
    assertThat(resultData.get(1).getKey(), is(localDateTimeToString(startOfToday.minusDays(1))));
    assertThat(resultData.get(1).getValue(), is(calculateExpectedValueGivenDurationsDefaultAggr(1000L)));
  }


  @Test
  public void testEmptyBucketsAreReturnedForStartDateFilterPeriod() {
    // given
    final OffsetDateTime startDate = OffsetDateTime.now();
    final ProcessInstanceEngineDto processInstanceDto1 = deployAndStartSimpleServiceTaskProcess();
    final String processDefinitionId = processInstanceDto1.getDefinitionId();
    final String processDefinitionKey = processInstanceDto1.getProcessDefinitionKey();
    final String processDefinitionVersion = processInstanceDto1.getProcessDefinitionVersion();
    adjustProcessInstanceDates(processInstanceDto1.getId(), startDate, 0L, 1L);

    final ProcessInstanceEngineDto processInstanceDto2 = engineRule.startProcessInstance(processDefinitionId);
    adjustProcessInstanceDates(processInstanceDto2.getId(), startDate, -2L, 2L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final RelativeDateFilterDataDto dateFilterDataDto = new RelativeDateFilterDataDto();
    dateFilterDataDto.setStart(new RelativeDateFilterStartDto(
      4L,
      RelativeDateFilterUnit.DAYS
    ));
    final StartDateFilterDto startDateFilterDto = new StartDateFilterDto(dateFilterDataDto);

    final ProcessReportDataDto reportData = ProcessReportDataBuilder.createReportData()
      .setDateInterval(GroupByDateUnit.DAY)
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion(processDefinitionVersion)
      .setReportDataType(PROC_INST_DUR_GROUP_BY_START_DATE)
      .setFilter(startDateFilterDto)
      .build();
    final ProcessDurationReportMapResultDto result = evaluateDurationMapReport(reportData).getResult();


    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(5));

    assertThat(
      resultData.get(0).getKey(),
      is(embeddedOptimizeRule.formatToHistogramBucketKey(startDate, ChronoUnit.DAYS))
    );
    assertThat(resultData.get(0).getValue(), is(1000L));

    assertThat(
      resultData.get(1).getKey(),
      is(embeddedOptimizeRule.formatToHistogramBucketKey(startDate.minusDays(1), ChronoUnit.DAYS))
    );
    assertThat(resultData.get(1).getValue(), is(nullValue()));

    assertThat(
      resultData.get(2).getKey(),
      is(embeddedOptimizeRule.formatToHistogramBucketKey(startDate.minusDays(2), ChronoUnit.DAYS))
    );
    assertThat(resultData.get(2).getValue(), is(2000L));

    assertThat(
      resultData.get(3).getKey(),
      is(embeddedOptimizeRule.formatToHistogramBucketKey(startDate.minusDays(3), ChronoUnit.DAYS))
    );
    assertThat(resultData.get(3).getValue(), is(nullValue()));

    assertThat(
      resultData.get(4).getKey(),
      is(embeddedOptimizeRule.formatToHistogramBucketKey(startDate.minusDays(4), ChronoUnit.DAYS))
    );
    assertThat(resultData.get(4).getValue(), is(nullValue()));
  }

}

