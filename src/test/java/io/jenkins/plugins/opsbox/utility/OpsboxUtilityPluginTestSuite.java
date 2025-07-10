package io.jenkins.plugins.opsbox.utility;

import io.jenkins.plugins.opsbox.utility.contributor.ListGitBranchesEnvironmentContributorTest;
import io.jenkins.plugins.opsbox.utility.integration.PluginIntegrationTest;
import io.jenkins.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinitionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 测试套件 - 运行所有Opsbox Utility Plugin的测试用例
 * Test Suite - Runs all test cases for Opsbox Utility Plugin
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    JobBuildNameParameterDefinitionTest.class,
    ListGitBranchesEnvironmentContributorTest.class,
    PluginIntegrationTest.class
})
public class OpsboxUtilityPluginTestSuite {
    // 测试套件不需要实现体，注解配置即可
    // Test suite doesn't need implementation body, annotation configuration is sufficient
}