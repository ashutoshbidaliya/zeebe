/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.zeebeimport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import io.zeebe.tasklist.util.TasklistZeebeIntegrationTest;
import io.zeebe.tasklist.util.ZeebeTestUtil;
import io.zeebe.tasklist.webapp.es.cache.WorkflowCache;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WorkflowCacheIT extends TasklistZeebeIntegrationTest {

  @SpyBean
  private WorkflowCache workflowCache;

  @Autowired
  @Qualifier("workflowIsDeployedCheck")
  private Predicate<Object[]> workflowIsDeployedCheck;

  @After
  public void after() {
    //clean the cache
    try {
      FieldSetter.setField(workflowCache, WorkflowCache.class.getDeclaredField("cache"), new ConcurrentHashMap<>());
    } catch (NoSuchFieldException e) {
      fail("Failed to inject cache into some of the beans");
    }
    super.after();
  }

  @Test
  public void testWorkflowDoesNotExist() {
    final String processName = workflowCache.getWorkflowName("2");
    assertThat(processName).isNull();
  }

  @Test
  public void testWorkflowNameAndTaskNameReturnedAndReused() {
    String workflowId1 = ZeebeTestUtil.deployWorkflow(zeebeClient, "simple_workflow.bpmn");
    String workflowId2 = ZeebeTestUtil.deployWorkflow(zeebeClient, "simple_workflow_2.bpmn");

    elasticsearchTestRule.processAllRecordsAndWait(workflowIsDeployedCheck, workflowId1);
    elasticsearchTestRule.processAllRecordsAndWait(workflowIsDeployedCheck, workflowId2);

    String demoProcessName = workflowCache.getWorkflowName(workflowId1);
    assertThat(demoProcessName).isNotNull();

    //request task name, must be already in cache
    String taskName = workflowCache.getTaskName(workflowId1, "taskA");
    assertThat(taskName).isNotNull();
    //request once again, the cache should be used
    demoProcessName = workflowCache.getWorkflowName(workflowId1);
    assertThat(demoProcessName).isNotNull();
    taskName = workflowCache.getTaskName(workflowId1, "taskA");
    assertThat(taskName).isNotNull();

    verify(workflowCache, times(1)).putToCache(any(), any());
  }

}
