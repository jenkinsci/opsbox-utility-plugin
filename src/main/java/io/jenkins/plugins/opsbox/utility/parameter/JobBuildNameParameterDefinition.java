package io.jenkins.plugins.opsbox.utility.parameter;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.verb.POST;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class JobBuildNameParameterDefinition extends SimpleParameterDefinition {

    private static final String DEFAULT_BUILD_NAME = "0.0.1-1+999";
    private static final int DEFAULT_COUNT_LIMIT = 5;

    @Getter
    private String jobName;
    private int countLimit;

    private final String defaultValue;

    @DataBoundConstructor
    public JobBuildNameParameterDefinition(String name, String jobName, String description) {
        super(name, description);
        this.jobName = jobName;
        this.countLimit = DEFAULT_COUNT_LIMIT;
        this.defaultValue = DEFAULT_BUILD_NAME;
    }

    public int getCountLimit() {
        return countLimit == 0 ? DEFAULT_COUNT_LIMIT: countLimit;
    }

    @DataBoundSetter
    public void setCountLimit(int countLimit) {
        this.countLimit = countLimit;
    }

    public JobBuildNameParameterDefinition(String name, String jobName, int countLimit, String defaultValue, String description) {
        super(name, description);
        this.jobName = jobName;
        this.countLimit = countLimit;
        this.defaultValue = defaultValue;
    }

    private static <T extends Item> T find(String jobName, Class<T> type) {
        Jenkins jenkins = Jenkins.getInstance();
        // direct search, can be used to find folder based items <folder>/<folder>/<jobName>
        T item = jenkins.getItemByFullName(jobName, type);
        if (item == null) {
            // not found in a direct search, search in all items since the item might be in a folder but given without folder structure
            // (to keep it backwards compatible)
            for (T allItem : jenkins.getAllItems(type)) {
                if (allItem.getName().equals(jobName)) {
                    item = allItem;
                    break;
                }
            }
        }
        return item;
    }

    private static List<String> getBuildNames(String jobName, int countLimit) {
        Job job = find(jobName, Job.class);
        if (job == null) {
            return new ArrayList<>();
        }

        List<String> buildNames = new ArrayList<>();
        RunList<Run> runList = job.getBuilds().newBuilds();

        for (Run run: runList) {
            if (buildNames.size() >= countLimit) {
                break;
            }

            if (run.isBuilding()) {
                continue;
            }

            Result result = run.getResult();

            if (result != null && result.isBetterOrEqualTo(Result.SUCCESS)) {
                buildNames.add(run.getDisplayName());
            }
        }

        return buildNames;
    }

    @Exported
    public List<String> getChoices() {
        List<String> choices = getBuildNames(this.jobName, this.countLimit);
        if (choices.isEmpty()) {
            choices.add(DEFAULT_BUILD_NAME);
        }

        return choices;
    }

    @Override
    public StringParameterValue getDefaultParameterValue() {
        List<String> choices = getChoices();
        return new StringParameterValue(getName(), defaultValue == null ? choices.get(0) : defaultValue, getDescription());
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        StringParameterValue value = req.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public StringParameterValue createValue(String value) {
        return new StringParameterValue(getName(), value, getDescription());
    }

    @Extension
    @Symbol({"jobBuildNameParam"})
    public static class DescriptorImpl extends ParameterDescriptor {

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.JobBuildNameParameterDefinition_DisplayName();
        }

        @POST
        public FormValidation doCheckJobName(@QueryParameter String jobName, @AncestorInPath Item item) {
            String errorMsg = Messages.JobBuildNameParameterDefinition_jobNotExists();

            Job job = JobBuildNameParameterDefinition.find(jobName, Job.class);
            if (job == null) {
                return FormValidation.error(errorMsg);
            }

            return FormValidation.ok();
        }

        public AutoCompletionCandidates doAutoCompleteJobName(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();

            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return candidates;
            }

            // 获取所有可用的作业
            List<Job> allJobs = jenkins.getAllItems(Job.class);

            for (Job job : allJobs) {
                String jobFullName = job.getFullName();

                // 如果用户输入了值，进行过滤匹配
                if (value != null && !value.trim().isEmpty()) {
                    if (jobFullName.toLowerCase().contains(value.toLowerCase())) {
                        candidates.add(jobFullName);
                    }
                } else {
                    // 如果没有输入值，添加所有作业
                    candidates.add(jobFullName);
                }
            }

            return candidates;
        }
    }
}
