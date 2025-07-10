package io.jenkins.plugins.opsbox.utility.contributor;

import com.google.common.collect.Maps;
import com.syhuang.hudson.plugins.listgitbranchesparameter.ListGitBranchesParameterDefinition;
import com.syhuang.hudson.plugins.listgitbranchesparameter.ListGitBranchesParameterValue;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Extension
public class ListGitBranchesEnvironmentContributor extends EnvironmentContributor {

    private static final Logger LOGGER = Logger.getLogger(ListGitBranchesEnvironmentContributor.class.getName());
    @Override
    public void buildEnvironmentFor(Run run, EnvVars envVars, TaskListener listener) {
        Map<String, String> configs = getParametersConfigs(run);
        if (!configs.isEmpty()) {
            envVars.putAll(configs);
        }

        Map<String, String> values = getParametersValue(run);
        if (!values.isEmpty()) {
            envVars.putAll(values);
        }
    }

    public static String cleanBranchName(String branch) {
        if (branch == null || branch.isEmpty()) {
            return branch;
        }

        if (branch.startsWith("refs/heads/")) {
            return branch.replace("refs/heads/", "");
        } else if (branch.startsWith("refs/tags/")) {
            return branch.replace("refs/tags/", "");
        }

        return branch;
    }

    private Map<String, String> getParametersValue(Run<?, ?> run) {
        // 获取参数化构建中的参数值
        Map<String, String> params = Maps.newHashMap();

        ParametersAction parametersAction = run.getAction(ParametersAction.class);
        if (parametersAction != null) {
            for (ParameterValue param : parametersAction.getParameters()) {
                if (param instanceof ListGitBranchesParameterValue) {
                    params.put(param.getName(), cleanBranchName(param.getValue().toString()));
                }
            }
        }
        return params;
    }

    private Map<String, String> getParametersConfigs(Run<?, ?> run) {
        Map<String, String> params = Maps.newHashMap();

        Job<?, ?> job = run.getParent();

        ParametersDefinitionProperty paramProp = job.getProperty(ParametersDefinitionProperty.class);
        if (paramProp == null) {
            LOGGER.fine("Job does not have any parameter definitions.");
            return params;
        }

        List<ParameterDefinition> parameterDefinitions = paramProp.getParameterDefinitions();
        if (parameterDefinitions == null || parameterDefinitions.isEmpty()) {
            LOGGER.fine("No parameters defined in the job.");
            return params;
        }

        for (ParameterDefinition pd : parameterDefinitions) {
            if (pd instanceof ListGitBranchesParameterDefinition gitParamDef) {

                String remoteUrl = gitParamDef.getRemoteURL();
                String credentialsId = gitParamDef.getCredentialsId();

                LOGGER.fine("Found Git remote URL: " + remoteUrl);

                // 只有非null值才添加到环境变量中
                if (remoteUrl != null) {
                    params.put(String.format("PARAMS__%s__REMOTE_URL", gitParamDef.getName()), remoteUrl);
                }
                if (credentialsId != null && !credentialsId.trim().isEmpty()) {
                    params.put(String.format("PARAMS__%s__CREDENTIALS_ID", gitParamDef.getName()), credentialsId);
                }
            }
        }

        return params;
    }
}
