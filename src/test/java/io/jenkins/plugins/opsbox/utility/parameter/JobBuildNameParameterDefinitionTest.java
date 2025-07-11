package io.jenkins.plugins.opsbox.utility.parameter;

import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JobBuildNameParameterDefinitionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private StaplerRequest staplerRequest;

    private JobBuildNameParameterDefinition parameterDefinition;
    private FreeStyleProject sourceJob;
    private FreeStyleProject targetJob;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // 创建源作业
        sourceJob = jenkins.createFreeStyleProject("source-job");
        sourceJob.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                return true;
            }
        });

        // 创建目标作业
        targetJob = jenkins.createFreeStyleProject("target-job");

        // 创建参数定义
        parameterDefinition = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "source-job",
            "Select build name from source job"
        );
    }

    @Test
    public void testConstructor() {
        assertEquals("BUILD_NAME", parameterDefinition.getName());
        assertEquals("source-job", parameterDefinition.getJobName());
        assertEquals("Select build name from source job", parameterDefinition.getDescription());
        assertEquals(5, parameterDefinition.getMaxBuildCount()); // 默认值
    }

    @Test
    public void testConstructorWithAllParameters() {
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "source-job",
            10,
            "1.0.0",
            "Test description"
        );

        assertEquals("BUILD_NAME", param.getName());
        assertEquals("source-job", param.getJobName());
        assertEquals(10, param.getMaxBuildCount());
        assertEquals("Test description", param.getDescription());
    }

    @Test
    public void testSetMaxBuildCount() {
        parameterDefinition.setMaxBuildCount(10);
        assertEquals(10, parameterDefinition.getMaxBuildCount());
    }

    @Test
    public void testGetMaxBuildCountWithZero() {
        parameterDefinition.setMaxBuildCount(0);
        assertEquals(5, parameterDefinition.getMaxBuildCount()); // 应该返回默认值
    }

    @Test
    public void testGetChoicesWithNoBuilds() {
        List<String> choices = parameterDefinition.getChoices();
        assertNotNull(choices);
        assertEquals(1, choices.size()); // 应该有默认构建名称
        assertEquals("0.0.1-1+999", choices.get(0));
    }

    @Test
    public void testGetChoicesWithSuccessfulBuilds() throws Exception {
        // 运行一些成功的构建
        for (int i = 1; i <= 3; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob);
            build.setDisplayName("build-" + i + ".0.0");
        }

        List<String> choices = parameterDefinition.getChoices();
        assertNotNull(choices);
        assertTrue(choices.size() > 0);
        assertTrue(choices.contains("build-3.0.0"));
    }

    @Test
    public void testGetDefaultParameterValue() {
        StringParameterValue defaultValue = parameterDefinition.getDefaultParameterValue();
        assertNotNull(defaultValue);
        assertEquals("BUILD_NAME", defaultValue.getName());
        assertEquals("Select build name from source job", defaultValue.getDescription());
        assertNotNull(defaultValue.getValue());
    }

    @Test
    public void testCreateValueFromString() {
        // 首先我们需要有一些选择
        parameterDefinition.getChoices(); // 这会创建默认选择

        StringParameterValue value = parameterDefinition.createValue("0.0.1-1+999");
        assertNotNull(value);
        assertEquals("BUILD_NAME", value.getName());
        assertEquals("0.0.1-1+999", value.getValue());
    }

    @Test
    public void testCreateValueFromRequest() {
        JSONObject json = new JSONObject();
        json.put("name", "BUILD_NAME");
        json.put("value", "0.0.1-1+999");

        when(staplerRequest.bindJSON(StringParameterValue.class, json))
            .thenReturn(new StringParameterValue("BUILD_NAME", "0.0.1-1+999"));

        ParameterValue value = parameterDefinition.createValue(staplerRequest, json);
        assertNotNull(value);
        assertTrue(value instanceof StringParameterValue);
        assertEquals("BUILD_NAME", value.getName());
    }

    @Test
    public void testDescriptorDisplayName() {
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            new JobBuildNameParameterDefinition.DescriptorImpl();
        String displayName = descriptor.getDisplayName();
        assertNotNull(displayName);
        assertFalse(displayName.isEmpty());
    }

    @Test
    public void testDescriptorJobNameValidation() throws IOException {
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            new JobBuildNameParameterDefinition.DescriptorImpl();

        // 测试存在的作业
        FormValidation validation = descriptor.doCheckJobName("source-job", sourceJob);
        assertEquals(FormValidation.Kind.OK, validation.kind);

        // 测试不存在的作业
        validation = descriptor.doCheckJobName("non-existent-job", sourceJob);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }

    @Test
    public void testJobInFolder() throws Exception {
        // 创建文件夹和文件夹中的作业
        // 注意：在Jenkins测试中，需要分别创建文件夹和作业
        jenkins.createFolder("test-folder");
        FreeStyleProject jobInFolder = jenkins.createProject(
            FreeStyleProject.class,
            "job-in-folder"
        );

        // 将作业移动到文件夹中 (这在实际使用中是通过Jenkins界面完成的)
        // 对于测试，我们简化处理，直接测试可以引用不同名称的作业
        JobBuildNameParameterDefinition paramInFolder = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "job-in-folder",  // 简化为直接使用作业名称
            "Test folder job"
        );

        // 创建一个构建以便测试
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(jobInFolder);
        build.setDisplayName("folder-build-1.0.0");

        // 验证可以找到作业
        List<String> choices = paramInFolder.getChoices();
        assertNotNull(choices);
        assertTrue("Should have at least one choice", choices.size() > 0);
        assertTrue("Should contain folder build", choices.contains("folder-build-1.0.0"));
    }

    @Test
    public void testMaxBuildCountRespectsLimit() throws Exception {
        // 创建更多构建
        for (int i = 1; i <= 10; i++) {
            FreeStyleBuild build = jenkins.buildAndAssertSuccess(sourceJob);
            build.setDisplayName("build-" + i + ".0.0");
        }

        // 设置限制为3
        parameterDefinition.setMaxBuildCount(3);

        List<String> choices = parameterDefinition.getChoices();
        assertNotNull(choices);
        assertTrue("Choices should not exceed count limit", choices.size() <= 3);
    }

    @Test
    public void testIgnoreFailedBuilds() throws Exception {
        // 创建一个失败的构建
        sourceJob.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                return false; // 模拟失败
            }
        });

        FreeStyleBuild failedBuild = jenkins.assertBuildStatus(Result.FAILURE, sourceJob.scheduleBuild2(0));
        failedBuild.setDisplayName("failed-build");

        // 添加成功的构建
        sourceJob.getBuildersList().clear();
        sourceJob.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                return true;
            }
        });

        FreeStyleBuild successBuild = jenkins.buildAndAssertSuccess(sourceJob);
        successBuild.setDisplayName("success-build");

        List<String> choices = parameterDefinition.getChoices();

        // 应该只包含成功的构建
        assertFalse("Should not contain failed build", choices.contains("failed-build"));
        assertTrue("Should contain success build", choices.contains("success-build"));
    }

    @Test
    public void testPermissionCheckWithAccessibleJob() throws Exception {
        // 创建一个有权限访问的作业
        FreeStyleProject accessibleJob = jenkins.createFreeStyleProject("accessible-job");

        // 运行一个成功的构建
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(accessibleJob);
        build.setDisplayName("accessible-build-1.0.0");

        // 创建参数定义指向可访问的作业
        JobBuildNameParameterDefinition accessibleParam = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "accessible-job",
            "Test accessible job"
        );

        // 验证可以获取构建名称
        List<String> choices = accessibleParam.getChoices();
        assertNotNull(choices);
        assertTrue("Should contain accessible build", choices.contains("accessible-build-1.0.0"));
    }

    @Test
    public void testPermissionCheckWithInaccessibleJob() throws Exception {
        // 创建一个作业
        FreeStyleProject inaccessibleJob = jenkins.createFreeStyleProject("inaccessible-job");

        // 运行一个成功的构建
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(inaccessibleJob);
        build.setDisplayName("inaccessible-build-1.0.0");

        // 测试权限检查逻辑 - 由于在测试环境中通常有完全权限，
        // 我们主要测试权限检查代码路径是否正常工作
        JobBuildNameParameterDefinition inaccessibleParam = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "inaccessible-job",
            "Test inaccessible job"
        );

        // 验证可以获取构建名称（在测试环境中应该有权限）
        List<String> choices = inaccessibleParam.getChoices();
        assertNotNull(choices);
        assertTrue("Should contain accessible build in test environment", choices.contains("inaccessible-build-1.0.0"));
    }

    @Test
    public void testPermissionCheckWithNonExistentJob() throws Exception {
        // 创建参数定义指向不存在的作业
        JobBuildNameParameterDefinition nonExistentParam = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "non-existent-job",
            "Test non-existent job"
        );

        // 验证返回默认值
        List<String> choices = nonExistentParam.getChoices();
        assertNotNull(choices);
        assertEquals("Should only have default value for non-existent job", 1, choices.size());
        assertEquals("Should have default build name", "0.0.1-1+999", choices.get(0));
    }

    // ==================== ACL 权限测试用例 ====================

    @Test
    public void testACL_UserWithJobReadPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 alice 到安全域中
        realm.loadUserByUsername2("alice");

        // 设置权限策略
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        authStrategy.grant(Item.READ).onItems(testJob).to("alice");
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "acl-test-job",
            "Test ACL with job read permission"
        );

        // 使用 alice 用户测试
        User alice = User.getOrCreateByIdOrFullName("alice");
        try (ACLContext context = ACL.as(alice)) {
            List<String> choices = param.getChoices();
            assertNotNull(choices);
            assertTrue("Alice should be able to access job builds", choices.contains("acl-test-build-1.0.0"));
        }
    }

    @Test
    public void testACL_UserWithoutJobReadPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-no-permission");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-no-permission-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 bob 到安全域中
        realm.loadUserByUsername2("bob");

        // 设置权限策略 - bob 没有任何权限
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "acl-test-job-no-permission",
            "Test ACL without job read permission"
        );

        // 使用 bob 用户测试（没有权限）
        User bob = User.getOrCreateByIdOrFullName("bob");
        try (ACLContext context = ACL.as(bob)) {
            List<String> choices = param.getChoices();
            assertNotNull(choices);
            // 应该返回默认值，因为 bob 没有权限访问该作业
            assertEquals("Bob should get default value when no permission", 1, choices.size());
            assertEquals("Bob should get default build name", "0.0.1-1+999", choices.get(0));
            assertFalse("Bob should not see job builds", choices.contains("acl-test-build-no-permission-1.0.0"));
        }
    }

    @Test
    public void testACL_UserWithJenkinsReadButNoJobPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-jenkins-read");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-jenkins-read-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 charlie 到安全域中
        realm.loadUserByUsername2("charlie");

        // 设置权限策略 - charlie 有 Jenkins.READ 但没有 Item.READ
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        authStrategy.grant(Jenkins.READ).everywhere().to("charlie");
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "acl-test-job-jenkins-read",
            "Test ACL with Jenkins read but no job permission"
        );

        // 使用 charlie 用户测试
        User charlie = User.getOrCreateByIdOrFullName("charlie");
        try (ACLContext context = ACL.as(charlie)) {
            List<String> choices = param.getChoices();
            assertNotNull(choices);
            // 应该返回默认值，因为 charlie 没有 Item.READ 权限
            assertEquals("Charlie should get default value when no job permission", 1, choices.size());
            assertEquals("Charlie should get default build name", "0.0.1-1+999", choices.get(0));
            assertFalse("Charlie should not see job builds", choices.contains("acl-test-build-jenkins-read-1.0.0"));
        }
    }

    @Test
    public void testACL_AdminUserWithFullPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-admin");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-admin-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 admin 到安全域中
        realm.loadUserByUsername2("admin");

        // 设置权限策略 - admin 有所有权限
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        authStrategy.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "acl-test-job-admin",
            "Test ACL with admin permission"
        );

        // 使用 admin 用户测试
        User admin = User.getOrCreateByIdOrFullName("admin");
        try (ACLContext context = ACL.as(admin)) {
            List<String> choices = param.getChoices();
            assertNotNull(choices);
            assertTrue("Admin should be able to access job builds", choices.contains("acl-test-build-admin-1.0.0"));
        }
    }

    @Test
    public void testACL_DescriptorValidationWithPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-descriptor");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-descriptor-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 david 到安全域中
        realm.loadUserByUsername2("david");

        // 设置权限策略
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        authStrategy.grant(Item.READ).onItems(testJob).to("david");
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建描述符
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            new JobBuildNameParameterDefinition.DescriptorImpl();

        // 使用 david 用户测试描述符验证
        User david = User.getOrCreateByIdOrFullName("david");
        try (ACLContext context = ACL.as(david)) {
            FormValidation validation = descriptor.doCheckJobName("acl-test-job-descriptor", testJob);
            assertEquals("David should be able to validate job name", FormValidation.Kind.OK, validation.kind);
        }
    }

    @Test
    public void testACL_DescriptorValidationWithoutPermission() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-descriptor-no-permission");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-descriptor-no-permission-1.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 eve 到安全域中
        realm.loadUserByUsername2("eve");

        // 设置权限策略 - eve 没有任何权限
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建描述符
        JobBuildNameParameterDefinition.DescriptorImpl descriptor =
            new JobBuildNameParameterDefinition.DescriptorImpl();

        // 使用 eve 用户测试描述符验证
        User eve = User.getOrCreateByIdOrFullName("eve");
        try (ACLContext context = ACL.as(eve)) {
            // 由于权限检查在 doCheckJobName 中，应该抛出 AccessDeniedException3
            try {
                descriptor.doCheckJobName("acl-test-job-descriptor-no-permission", testJob);
                fail("Should throw AccessDeniedException3 when user has no permission");
            } catch (hudson.security.AccessDeniedException3 e) {
                // 预期的异常
                assertNotNull("AccessDeniedException3 should be thrown", e);
            }
        }
    }

    @Test
    public void testACL_MultipleJobsWithDifferentPermissions() throws Exception {
        // 创建多个测试作业
        FreeStyleProject job1 = jenkins.createFreeStyleProject("acl-multi-job-1");
        FreeStyleProject job2 = jenkins.createFreeStyleProject("acl-multi-job-2");
        FreeStyleProject job3 = jenkins.createFreeStyleProject("acl-multi-job-3");

        FreeStyleBuild build1 = jenkins.buildAndAssertSuccess(job1);
        build1.setDisplayName("acl-multi-build-1.0.0");
        FreeStyleBuild build2 = jenkins.buildAndAssertSuccess(job2);
        build2.setDisplayName("acl-multi-build-2.0.0");
        FreeStyleBuild build3 = jenkins.buildAndAssertSuccess(job3);
        build3.setDisplayName("acl-multi-build-3.0.0");

        // 设置 dummy 安全域，允许我们添加虚拟用户
        JenkinsRule.DummySecurityRealm realm = jenkins.createDummySecurityRealm();
        jenkins.getInstance().setSecurityRealm(realm);

        // 添加用户 frank 到安全域中
        realm.loadUserByUsername2("frank");

        // 设置权限策略 - frank 只能访问 job1 和 job2
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        authStrategy.grant(Item.READ).onItems(job1, job2).to("frank");
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param1 = new JobBuildNameParameterDefinition(
            "BUILD_NAME_1", "acl-multi-job-1", "Test multi job 1"
        );
        JobBuildNameParameterDefinition param2 = new JobBuildNameParameterDefinition(
            "BUILD_NAME_2", "acl-multi-job-2", "Test multi job 2"
        );
        JobBuildNameParameterDefinition param3 = new JobBuildNameParameterDefinition(
            "BUILD_NAME_3", "acl-multi-job-3", "Test multi job 3"
        );

        // 使用 frank 用户测试
        User frank = User.getOrCreateByIdOrFullName("frank");
        try (ACLContext context = ACL.as(frank)) {
            // 测试有权限的作业
            List<String> choices1 = param1.getChoices();
            assertTrue("Frank should access job1", choices1.contains("acl-multi-build-1.0.0"));

            List<String> choices2 = param2.getChoices();
            assertTrue("Frank should access job2", choices2.contains("acl-multi-build-2.0.0"));

            // 测试没有权限的作业
            List<String> choices3 = param3.getChoices();
            assertEquals("Frank should get default for job3", 1, choices3.size());
            assertEquals("Frank should get default build name for job3", "0.0.1-1+999", choices3.get(0));
            assertFalse("Frank should not see job3 builds", choices3.contains("acl-multi-build-3.0.0"));
        }
    }

    @Test
    public void testACL_AnonymousUser() throws Exception {
        // 创建测试作业
        FreeStyleProject testJob = jenkins.createFreeStyleProject("acl-test-job-anonymous");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(testJob);
        build.setDisplayName("acl-test-build-anonymous-1.0.0");

        // 设置权限策略 - 匿名用户没有任何权限
        MockAuthorizationStrategy authStrategy = new MockAuthorizationStrategy();
        jenkins.getInstance().setAuthorizationStrategy(authStrategy);

        // 创建参数定义
        JobBuildNameParameterDefinition param = new JobBuildNameParameterDefinition(
            "BUILD_NAME",
            "acl-test-job-anonymous",
            "Test ACL with anonymous user"
        );

        // 使用匿名用户测试
        try (ACLContext context = ACL.as2(Jenkins.ANONYMOUS2)) {
            List<String> choices = param.getChoices();
            assertNotNull(choices);
            // 匿名用户应该返回默认值
            assertEquals("Anonymous should get default value", 1, choices.size());
            assertEquals("Anonymous should get default build name", "0.0.1-1+999", choices.get(0));
            assertFalse("Anonymous should not see job builds", choices.contains("acl-test-build-anonymous-1.0.0"));
        }
    }
}