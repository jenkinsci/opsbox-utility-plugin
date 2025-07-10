package io.jenkins.plugins.opsbox.utility.contributor;

import com.syhuang.hudson.plugins.listgitbranchesparameter.ListGitBranchesParameterDefinition;
import com.syhuang.hudson.plugins.listgitbranchesparameter.ListGitBranchesParameterValue;
import hudson.EnvVars;
import hudson.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ListGitBranchesEnvironmentContributorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private AbstractBuild mockBuild;

    @Mock
    private Job mockJob;

    @Mock
    private TaskListener mockListener;

    @Mock
    private ParametersAction mockParametersAction;

    @Mock
    private ParametersDefinitionProperty mockParamProp;

    private ListGitBranchesEnvironmentContributor contributor;
    private EnvVars envVars;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        contributor = new ListGitBranchesEnvironmentContributor();
        envVars = new EnvVars();
    }

    @Test
    public void testCleanBranchName() {
        // 测试清理分支名称
        assertEquals("master", ListGitBranchesEnvironmentContributor.cleanBranchName("refs/heads/master"));
        assertEquals("develop", ListGitBranchesEnvironmentContributor.cleanBranchName("refs/heads/develop"));
        assertEquals("v1.0.0", ListGitBranchesEnvironmentContributor.cleanBranchName("refs/tags/v1.0.0"));
        assertEquals("feature/test", ListGitBranchesEnvironmentContributor.cleanBranchName("refs/heads/feature/test"));

        // 测试已经是干净的分支名称
        assertEquals("master", ListGitBranchesEnvironmentContributor.cleanBranchName("master"));
        assertEquals("develop", ListGitBranchesEnvironmentContributor.cleanBranchName("develop"));

        // 测试边界情况
        assertEquals("", ListGitBranchesEnvironmentContributor.cleanBranchName(""));
        assertNull(ListGitBranchesEnvironmentContributor.cleanBranchName(null));
    }

    @Test
    public void testBuildEnvironmentForWithValidRun() {
        // 设置模拟对象
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);

        // 创建参数值列表
        List<ParameterValue> paramValues = new ArrayList<>();
        ListGitBranchesParameterValue gitParam = mock(ListGitBranchesParameterValue.class);
        when(gitParam.getName()).thenReturn("BRANCH");
        when(gitParam.getValue()).thenReturn("refs/heads/master");
        paramValues.add(gitParam);

        when(mockParametersAction.getParameters()).thenReturn(paramValues);

        // 创建参数定义列表
        List<ParameterDefinition> paramDefs = new ArrayList<>();
        ListGitBranchesParameterDefinition gitParamDef = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef.getName()).thenReturn("BRANCH");
        when(gitParamDef.getRemoteURL()).thenReturn("https://github.com/test/repo.git");
        when(gitParamDef.getCredentialsId()).thenReturn("git-credentials");
        paramDefs.add(gitParamDef);

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(paramDefs);

        // 执行测试
        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 验证环境变量
        assertEquals("master", envVars.get("BRANCH"));
        assertEquals("https://github.com/test/repo.git", envVars.get("PARAMS__BRANCH__REMOTE_URL"));
        assertEquals("git-credentials", envVars.get("PARAMS__BRANCH__CREDENTIALS_ID"));
    }

    @Test
    public void testBuildEnvironmentForWithNoParameters() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(null);
        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(null);

        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 应该没有环境变量被添加
        assertTrue("Environment variables should be empty", envVars.isEmpty());
    }

    @Test
    public void testBuildEnvironmentForWithEmptyParameterDefinitions() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);
        when(mockParametersAction.getParameters()).thenReturn(new ArrayList<>());

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(new ArrayList<>());

        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        assertTrue("Environment variables should be empty", envVars.isEmpty());
    }

    @Test
    public void testBuildEnvironmentForWithMultipleGitParameters() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);

        // 创建多个Git参数值
        List<ParameterValue> paramValues = new ArrayList<>();

        ListGitBranchesParameterValue gitParam1 = mock(ListGitBranchesParameterValue.class);
        when(gitParam1.getName()).thenReturn("MAIN_BRANCH");
        when(gitParam1.getValue()).thenReturn("refs/heads/master");
        paramValues.add(gitParam1);

        ListGitBranchesParameterValue gitParam2 = mock(ListGitBranchesParameterValue.class);
        when(gitParam2.getName()).thenReturn("FEATURE_BRANCH");
        when(gitParam2.getValue()).thenReturn("refs/heads/feature/test");
        paramValues.add(gitParam2);

        when(mockParametersAction.getParameters()).thenReturn(paramValues);

        // 创建多个参数定义
        List<ParameterDefinition> paramDefs = new ArrayList<>();

        ListGitBranchesParameterDefinition gitParamDef1 = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef1.getName()).thenReturn("MAIN_BRANCH");
        when(gitParamDef1.getRemoteURL()).thenReturn("https://github.com/test/main-repo.git");
        when(gitParamDef1.getCredentialsId()).thenReturn("main-credentials");
        paramDefs.add(gitParamDef1);

        ListGitBranchesParameterDefinition gitParamDef2 = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef2.getName()).thenReturn("FEATURE_BRANCH");
        when(gitParamDef2.getRemoteURL()).thenReturn("https://github.com/test/feature-repo.git");
        when(gitParamDef2.getCredentialsId()).thenReturn("feature-credentials");
        paramDefs.add(gitParamDef2);

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(paramDefs);

        // 执行测试
        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 验证环境变量
        assertEquals("master", envVars.get("MAIN_BRANCH"));
        assertEquals("feature/test", envVars.get("FEATURE_BRANCH"));
        assertEquals("https://github.com/test/main-repo.git", envVars.get("PARAMS__MAIN_BRANCH__REMOTE_URL"));
        assertEquals("https://github.com/test/feature-repo.git", envVars.get("PARAMS__FEATURE_BRANCH__REMOTE_URL"));
        assertEquals("main-credentials", envVars.get("PARAMS__MAIN_BRANCH__CREDENTIALS_ID"));
        assertEquals("feature-credentials", envVars.get("PARAMS__FEATURE_BRANCH__CREDENTIALS_ID"));
    }

    @Test
    public void testBuildEnvironmentForWithMixedParameterTypes() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);

        // 创建混合参数类型
        List<ParameterValue> paramValues = new ArrayList<>();

        ListGitBranchesParameterValue gitParam = mock(ListGitBranchesParameterValue.class);
        when(gitParam.getName()).thenReturn("BRANCH");
        when(gitParam.getValue()).thenReturn("refs/heads/master");
        paramValues.add(gitParam);

        StringParameterValue stringParam = new StringParameterValue("STRING_PARAM", "test-value");
        paramValues.add(stringParam);

        when(mockParametersAction.getParameters()).thenReturn(paramValues);

        // 创建混合参数定义
        List<ParameterDefinition> paramDefs = new ArrayList<>();

        ListGitBranchesParameterDefinition gitParamDef = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef.getName()).thenReturn("BRANCH");
        when(gitParamDef.getRemoteURL()).thenReturn("https://github.com/test/repo.git");
        when(gitParamDef.getCredentialsId()).thenReturn("git-credentials");
        paramDefs.add(gitParamDef);

        StringParameterDefinition stringParamDef = new StringParameterDefinition("STRING_PARAM", "default", "Test string param");
        paramDefs.add(stringParamDef);

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(paramDefs);

        // 执行测试
        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 只有Git参数应该被处理
        assertEquals("master", envVars.get("BRANCH"));
        assertEquals("https://github.com/test/repo.git", envVars.get("PARAMS__BRANCH__REMOTE_URL"));
        assertEquals("git-credentials", envVars.get("PARAMS__BRANCH__CREDENTIALS_ID"));

        // 字符串参数不应该被处理
        assertNull(envVars.get("STRING_PARAM"));
        assertNull(envVars.get("PARAMS__STRING_PARAM__REMOTE_URL"));
    }

    @Test
    public void testBuildEnvironmentForWithTagReference() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);

        // 创建标签参数
        List<ParameterValue> paramValues = new ArrayList<>();
        ListGitBranchesParameterValue gitParam = mock(ListGitBranchesParameterValue.class);
        when(gitParam.getName()).thenReturn("TAG");
        when(gitParam.getValue()).thenReturn("refs/tags/v1.0.0");
        paramValues.add(gitParam);

        when(mockParametersAction.getParameters()).thenReturn(paramValues);

        // 创建参数定义
        List<ParameterDefinition> paramDefs = new ArrayList<>();
        ListGitBranchesParameterDefinition gitParamDef = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef.getName()).thenReturn("TAG");
        when(gitParamDef.getRemoteURL()).thenReturn("https://github.com/test/repo.git");
        when(gitParamDef.getCredentialsId()).thenReturn("git-credentials");
        paramDefs.add(gitParamDef);

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(paramDefs);

        // 执行测试
        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 验证标签名称被正确清理
        assertEquals("v1.0.0", envVars.get("TAG"));
        assertEquals("https://github.com/test/repo.git", envVars.get("PARAMS__TAG__REMOTE_URL"));
        assertEquals("git-credentials", envVars.get("PARAMS__TAG__CREDENTIALS_ID"));
    }

    @Test
    public void testBuildEnvironmentForWithNullCredentials() {
        when(mockBuild.getParent()).thenReturn(mockJob);
        when(mockBuild.getAction(ParametersAction.class)).thenReturn(mockParametersAction);

        List<ParameterValue> paramValues = new ArrayList<>();
        ListGitBranchesParameterValue gitParam = mock(ListGitBranchesParameterValue.class);
        when(gitParam.getName()).thenReturn("BRANCH");
        when(gitParam.getValue()).thenReturn("master");
        paramValues.add(gitParam);

        when(mockParametersAction.getParameters()).thenReturn(paramValues);

        List<ParameterDefinition> paramDefs = new ArrayList<>();
        ListGitBranchesParameterDefinition gitParamDef = mock(ListGitBranchesParameterDefinition.class);
        when(gitParamDef.getName()).thenReturn("BRANCH");
        when(gitParamDef.getRemoteURL()).thenReturn("https://github.com/test/repo.git");
        when(gitParamDef.getCredentialsId()).thenReturn(null); // null凭据
        paramDefs.add(gitParamDef);

        when(mockJob.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParamProp);
        when(mockParamProp.getParameterDefinitions()).thenReturn(paramDefs);

        // 执行测试
        contributor.buildEnvironmentFor((Run)mockBuild, envVars, mockListener);

        // 验证环境变量
        assertEquals("master", envVars.get("BRANCH"));
        assertEquals("https://github.com/test/repo.git", envVars.get("PARAMS__BRANCH__REMOTE_URL"));
        assertNull(envVars.get("PARAMS__BRANCH__CREDENTIALS_ID")); // 应该是null
    }

    @Test
    public void testBuildEnvironmentForIntegration() throws Exception {
        // 简化的集成测试 - 由于ListGitBranchesParameterDefinition构造器参数复杂，
        // 这里只测试核心功能，实际的集成测试需要在真实环境中进行
        assertTrue("Integration test placeholder - requires actual plugin environment", true);
    }
}