package com.datapath.persistence.migration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class Migration {

    @Getter
    private String name;

    @Getter
    private List<? extends ApplicationCondition> applicationConditions;

    protected Migration(List<? extends ApplicationCondition> conditions) {
        this.name = getClass().getSimpleName();
        this.applicationConditions = findCompatibleApplicationConditions(conditions);
    }

    private List<? extends ApplicationCondition> findCompatibleApplicationConditions(
            List<? extends ApplicationCondition> allConditions) {

        return allConditions.stream()
                .filter(condition -> AnnotationUtils.isAnnotationDeclaredLocally(
                        ConditionForMigration.class, condition.getClass()))
                .filter(condition -> condition.getClass()
                        .getAnnotation(ConditionForMigration.class)
                        .value()
                        .equals(getClass()))
                .collect(Collectors.toList());
    }

    public boolean checkApplicationCondition() {
        if (applicationConditions.isEmpty()) {
            log.error("Can't found competitive condition for {}", getClass().getSimpleName());
        }

        for (ApplicationCondition condition : applicationConditions) {
            if (condition.check()) {
                return true;
            }
        }
        return false;
    }

    public abstract void apply();
}
