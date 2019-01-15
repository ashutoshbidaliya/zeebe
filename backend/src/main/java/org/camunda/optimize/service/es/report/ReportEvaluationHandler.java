package org.camunda.optimize.service.es.report;

import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.ReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedProcessReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.DecisionReportNumberResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.DecisionReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportMapResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportNumberResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportResultDto;
import org.camunda.optimize.service.es.reader.ReportReader;
import org.camunda.optimize.service.es.report.command.util.ReportUtil;
import org.camunda.optimize.service.exceptions.OptimizeException;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.service.exceptions.OptimizeValidationException;
import org.camunda.optimize.service.exceptions.evaluation.ReportEvaluationException;
import org.camunda.optimize.service.util.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public abstract class ReportEvaluationHandler {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ReportReader reportReader;

  @Autowired
  private ReportEvaluator reportEvaluator;

  public ReportResultDto evaluateSavedReport(String userId, String reportId) {
    ReportDefinitionDto reportDefinition = reportReader.getReport(reportId);
    return evaluateReport(userId, reportDefinition);
  }

  protected ReportResultDto evaluateReport(String userId, ReportDefinitionDto reportDefinition) {
    final ReportResultDto result;
    if (!reportDefinition.getCombined()) {
      switch (reportDefinition.getReportType()) {
        case PROCESS:
          SingleProcessReportDefinitionDto processDefinition =
            (SingleProcessReportDefinitionDto) reportDefinition;
          result = evaluateSingleProcessReport(userId, processDefinition);
          break;
        case DECISION:
          SingleDecisionReportDefinitionDto decisionDefinition =
            (SingleDecisionReportDefinitionDto) reportDefinition;
          result = evaluateSingleDecisionReport(userId, decisionDefinition);
          break;
        default:
          throw new IllegalStateException("Unsupported reportType: " + reportDefinition.getReportType());
      }
    } else {
      CombinedReportDefinitionDto combinedReportDefinition =
        (CombinedReportDefinitionDto) reportDefinition;
      result = evaluateCombinedReport(userId, combinedReportDefinition);
    }
    return result;
  }

  private CombinedProcessReportResultDto evaluateCombinedReport(String userId,
                                                                CombinedReportDefinitionDto combinedReportDefinition) {

    ValidationHelper.validateCombinedReportDefinition(combinedReportDefinition);
    List<ReportResultDto> resultList = evaluateListOfReportIds(
      userId, combinedReportDefinition.getData().getReportIds()
    );
    return transformToCombinedReportResult(combinedReportDefinition, resultList);
  }

  private CombinedProcessReportResultDto transformToCombinedReportResult(
    CombinedReportDefinitionDto combinedReportDefinition,
    List<ReportResultDto> singleReportResultList) {
    final AtomicReference<Class> singleReportType = new AtomicReference<>();
    final Map<String, ProcessReportResultDto> reportIdToMapResult = singleReportResultList
      .stream()
      .filter(t -> t instanceof ProcessReportNumberResultDto || t instanceof ProcessReportMapResultDto)
      .map(t -> (ProcessReportResultDto) t)
      .filter(singleReportResult -> singleReportResult.getClass().equals(singleReportType.get())
        || singleReportType.compareAndSet(null, singleReportResult.getClass()))
      .collect(Collectors.toMap(
        ReportResultDto::getId,
        singleReportResultDto -> singleReportResultDto,
        (u, v) -> {
          throw new IllegalStateException(String.format("Duplicate key %s", u));
        },
        LinkedHashMap::new
      ));
    final CombinedProcessReportResultDto<ProcessReportResultDto> combinedReportResult =
      new CombinedProcessReportResultDto<>();
    combinedReportResult.setResult(reportIdToMapResult);
    ReportUtil.copyCombinedReportMetaData(combinedReportDefinition, combinedReportResult);
    return combinedReportResult;
  }

  private List<ReportResultDto> evaluateListOfReportIds(String userId, List<String> singleReportIds) {
    List<ReportResultDto> resultList = new ArrayList<>();
    for (String reportId : singleReportIds) {
      SingleProcessReportDefinitionDto singleReportDefinition = reportReader.getSingleProcessReport(reportId);
      Optional<ReportResultDto> singleReportResult = evaluateReportForCombinedReport(
        userId, singleReportDefinition
      );
      singleReportResult.ifPresent(resultList::add);
    }
    return resultList;
  }

  private Optional<ReportResultDto> evaluateReportForCombinedReport(String userId,
                                                                    SingleProcessReportDefinitionDto reportDefinition) {
    Optional<ReportResultDto> result = Optional.empty();
    if (isAuthorizedToSeeReport(userId, reportDefinition)) {
      try {
        ReportResultDto singleResult = reportEvaluator.evaluate(reportDefinition.getData());
        ReportUtil.copyMetaData(reportDefinition, (ReportDefinitionDto) singleResult);
        result = Optional.of(singleResult);
      } catch (OptimizeException | OptimizeValidationException ignored) {
        // we just ignore reports that cannot be evaluated in
        // a combined report
      }
    }
    return result;
  }

  /**
   * Checks if the user is allowed to see the given report.
   */
  protected abstract boolean isAuthorizedToSeeReport(String userId, ReportDefinitionDto report);

  private ReportResultDto evaluateSingleProcessReport(final String userId,
                                                      final SingleProcessReportDefinitionDto reportDefinition) {

    if (!isAuthorizedToSeeReport(userId, reportDefinition)) {
      ProcessReportDataDto reportData = reportDefinition.getData();
      throw new ForbiddenException(
        "User [" + userId + "] is not authorized to evaluate report ["
          + reportDefinition.getName() + "] with process definition [" + reportData.getProcessDefinitionKey() + "]."
      );
    }

    ReportResultDto result = evaluateSingleReportWithErrorCheck(reportDefinition);

    if (result instanceof ProcessReportResultDto) {
      ReportUtil.copyMetaData(reportDefinition, (ProcessReportResultDto) result);
    } else {
      String errorMessage =
        String.format("Evaluation of process report with id [%s] return wrong data structure!",
                      reportDefinition.getId());
      logger.error(errorMessage);
      throw new OptimizeRuntimeException(errorMessage);
    }

    return result;
  }

  private ReportResultDto evaluateSingleReportWithErrorCheck(SingleProcessReportDefinitionDto reportDefinition) {
    ProcessReportDataDto reportData = reportDefinition.getData();
    try {
      return reportEvaluator.evaluate(reportData);
    } catch (OptimizeException | OptimizeValidationException e) {
      ProcessReportNumberResultDto definitionWrapper = new ProcessReportNumberResultDto();
      definitionWrapper.setData(reportData);
      definitionWrapper.setName(reportDefinition.getName());
      definitionWrapper.setId(reportDefinition.getId());
      throw new ReportEvaluationException(definitionWrapper, e);
    }
  }

  private ReportResultDto evaluateSingleDecisionReport(final String userId,
                                                       final SingleDecisionReportDefinitionDto reportDefinition) {

    if (!isAuthorizedToSeeReport(userId, reportDefinition)) {
      DecisionReportDataDto reportData = reportDefinition.getData();
      throw new ForbiddenException(
        "User [" + userId + "] is not authorized to evaluate report ["
          + reportDefinition.getName() + "] with decision definition [" + reportData.getDecisionDefinitionKey() + "]."
      );
    }

    ReportResultDto result = evaluateSingleReportWithErrorCheck(reportDefinition);

    if (result instanceof DecisionReportResultDto) {
      ReportUtil.copyMetaData(reportDefinition, (DecisionReportResultDto) result);
    } else {
      String errorMessage =
        String.format("Evaluation of decision report with id [%s] return wrong data structure!",
                      reportDefinition.getId());
      logger.error(errorMessage);
      throw new OptimizeRuntimeException(errorMessage);
    }

    return result;
  }

  private ReportResultDto evaluateSingleReportWithErrorCheck(SingleDecisionReportDefinitionDto reportDefinition) {
    DecisionReportDataDto reportData = reportDefinition.getData();
    try {
      return reportEvaluator.evaluate(reportData);
    } catch (OptimizeException | OptimizeValidationException e) {
      DecisionReportNumberResultDto definitionWrapper = new DecisionReportNumberResultDto();
      definitionWrapper.setData(reportData);
      definitionWrapper.setName(reportDefinition.getName());
      definitionWrapper.setId(reportDefinition.getId());
      throw new ReportEvaluationException(definitionWrapper, e);
    }
  }

}
