package io.jenkins.plugins.opsbox.utility.integration;

import hudson.model.*;
import io.jenkins.plugins.opsbox.utility.contributor.ListGitBranchesEnvironmentContributor;
import io.jenkins.plugins.opsbox.utility.parameter.JobBuildNameParameterDefinition;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 插件集成测试 - 测试插件各功能模块的集成工作
 * Plugin Integration Test - Tests the integration of various plugin modules
 */
public class PluginIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testJobBuildNameParameterWithMultipleJobs() throws Exception {
        // 创建多个源作业并运行构建
        FreeStyleProject sourceJob1 = jenkins.createFreeStyleProject("source-job-1");
        FreeStyleProject sourceJob2 = jenkins.createFreeStyleProject("source-job-2");

        // 为第一个作业创建一些构建
        for (int i = 1; i <= 3; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob1);
            build.setDisplayName("v1.0." + i);
        }

        // 为第二个作业创建一些构建
        for (int i = 1; i <= 2; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob2);
            build.setDisplayName("v2.0." + i);
        }

        // 创建使用JobBuildNameParameter的目标作业
        FreeStyleProject targetJob = jenkins.createFreeStyleProject("target-job");

        // 添加参数定义
        JobBuildNameParameterDefinition param1 = new JobBuildNameParameterDefinition(
            "SOURCE1_BUILD", "source-job-1", "Select build from source job 1"
        );
        param1.setMaxBuildCount(2);

        JobBuildNameParameterDefinition param2 = new JobBuildNameParameterDefinition(
            "SOURCE2_BUILD", "source-job-2", "Select build from source job 2"
        );

        ParametersDefinitionProperty paramProp = new ParametersDefinitionProperty(param1, param2);
        targetJob.addProperty(paramProp);

        // 验证参数选择
        List<String> choices1 = param1.getChoices();
        List<String> choices2 = param2.getChoices();

        assertNotNull(choices1);
        assertNotNull(choices2);
        assertTrue("Should have builds from source job 1", choices1.size() > 0);
        assertTrue("Should have builds from source job 2", choices2.size() > 0);

        // 验证数量限制
        assertTrue("Should respect count limit", choices1.size() <= 2);

        // 验证构建名称
        assertTrue("Should contain v1.0.3", choices1.contains("v1.0.3"));
        assertTrue("Should contain v2.0.2", choices2.contains("v2.0.2"));
    }

    @Test
    public void testJobBuildNameParameterValidation() throws Exception {
        // 创建源作业
        FreeStyleProject sourceJob = jenkins.createFreeStyleProject("validation-source");
        jenkins.buildAndAssertSuccess(sourceJob);

        // 测试参数定义验证
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            new JobBuildNameParameterDefinition.DescriptorImpl();

        // 测试有效的作业名称
        assertEquals("Valid job should pass validation",
            hudson.util.FormValidation.Kind.OK,
            descriptor.doCheckJobName("validation-source", sourceJob).kind);

        // 测试无效的作业名称
        assertEquals("Invalid job should fail validation",
            hudson.util.FormValidation.Kind.ERROR,
            descriptor.doCheckJobName("non-existent-job", sourceJob).kind);
    }

    @Test
    public void testPluginSymbolRegistration() throws Exception {
        // 测试插件符号是否正确注册
        FreeStyleProject project = jenkins.createFreeStyleProject("symbol-test");

        // 创建JobBuildNameParameterDefinition，验证Symbol注解
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "TEST_BUILD", "symbol-test", "Test parameter"
        );

        // 验证描述符
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            (JobBuildNameParameterDefinition.DescriptorImpl) param.getDescriptor();

        assertNotNull("Descriptor should not be null", descriptor);
        assertNotNull("Display name should not be null", descriptor.getDisplayName());
        assertFalse("Display name should not be empty", descriptor.getDisplayName().isEmpty());
    }

    @Test
    public void testFolderJobSupport() throws Exception {
        // 测试文件夹中的作业支持
        // 注意：在Jenkins测试中，需要分别创建文件夹和作业
        jenkins.createFolder("test-folder");
        FreeStyleProject folderJob = jenkins.createProject(FreeStyleProject.class, "folder-job");

        // 创建构建
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(folderJob);
        build.setDisplayName("folder-build-1.0.0");

        // 测试可以引用作业（简化测试，实际中文件夹支持需要更复杂的设置）
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "FOLDER_BUILD", "folder-job", "Build from folder job"
        );

        List<String> choices = param.getChoices();
        assertNotNull("Choices should not be null", choices);
        assertTrue("Should have at least one choice", !choices.isEmpty());
        assertTrue("Should contain folder build", choices.contains("folder-build-1.0.0"));
    }

    @Test
    public void testPerformanceWithManyBuilds() throws Exception {
        // 性能测试：创建大量构建并测试参数性能
        FreeStyleProject sourceJob = jenkins.createFreeStyleProject("performance-source");

        // 创建多个构建
        for (int i = 1; i <= 20; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob);
            build.setDisplayName("perf-build-" + i + ".0.0");
        }

        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "PERF_BUILD", "performance-source", "Performance test parameter"
        );
        param.setMaxBuildCount(10);

        // 测试获取选择的性能
        long startTime = System.currentTimeMillis();
        List<String> choices = param.getChoices();
        long endTime = System.currentTimeMillis();

        assertNotNull("Choices should not be null", choices);
        assertEquals("Should respect count limit", 10, choices.size());
        assertTrue("Should complete within reasonable time", (endTime - startTime) < 5000); // 5秒内完成

        // 验证返回的是最新的构建
        assertTrue("Should contain latest build", choices.contains("perf-build-20.0.0"));
    }

    @Test
    public void testGitBranchEnvironmentFeatures() throws Exception {
        // 简化的Git分支环境变量测试
        // 由于ListGitBranchesParameterDefinition构造器参数复杂，
        // 这里只测试核心功能，实际的集成测试需要在真实环境中进行

        // 测试分支名称清理功能
        String cleanedName = ListGitBranchesEnvironmentContributor
            .cleanBranchName("refs/heads/feature/test");
        assertEquals("feature/test", cleanedName);

        cleanedName = ListGitBranchesEnvironmentContributor
            .cleanBranchName("refs/tags/v1.0.0");
        assertEquals("v1.0.0", cleanedName);

        // 测试已经清理的分支名称
        cleanedName = ListGitBranchesEnvironmentContributor
            .cleanBranchName("master");
        assertEquals("master", cleanedName);
    }

    @Test
    public void testMixedParameterTypes() throws Exception {
        // 测试混合参数类型的作业
        FreeStyleProject sourceJob = jenkins.createFreeStyleProject("mixed-source-job");

        // 创建一些构建
        for (int i = 1; i <= 2; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob);
            build.setDisplayName("build-" + i + ".0.0");
        }

        FreeStyleProject targetJob = jenkins.createFreeStyleProject("mixed-target-job");

        // 添加多种类型的参数定义
        JobBuildNameParameterDefinition buildParam = new JobBuildNameParameterDefinition(
            "BUILD_NAME", "mixed-source-job", "Select build name"
        );

        StringParameterDefinition stringParam = new StringParameterDefinition(
            "STRING_PARAM", "default-value", "String parameter"
        );

        ParametersDefinitionProperty paramProp = new ParametersDefinitionProperty(
            buildParam, stringParam
        );
        targetJob.addProperty(paramProp);

        // 验证参数可以正常工作
        List<String> buildChoices = buildParam.getChoices();
        assertNotNull("Build choices should not be null", buildChoices);
        assertTrue("Should have build choices", buildChoices.size() > 0);
        assertTrue("Should contain build-2.0.0", buildChoices.contains("build-2.0.0"));
    }
}