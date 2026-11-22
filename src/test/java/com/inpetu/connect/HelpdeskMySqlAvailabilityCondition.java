package com.inpetu.connect;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.DriverManager;

public class HelpdeskMySqlAvailabilityCondition implements ExecutionCondition {
    private static final String DATABASE_URL =
        "jdbc:mysql://localhost:3307/inpetu_helpdesk_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String user = HelpdeskMySqlIT.DATABASE_USER;
        String password = HelpdeskMySqlIT.DATABASE_PASSWORD;
        if (user == null || user.isBlank() || password == null) {
            return ConditionEvaluationResult.disabled(
                "não verificado: credenciais do MySQL local de teste indisponíveis");
        }
        try (var connection = DriverManager.getConnection(DATABASE_URL, user, password)) {
            if (!"inpetu_helpdesk_test".equalsIgnoreCase(connection.getCatalog())) {
                return ConditionEvaluationResult.disabled(
                    "não verificado: schema de integração inesperado");
            }
            return ConditionEvaluationResult.enabled(
                "MySQL local disponível no schema isolado de Help Desk");
        } catch (Exception unavailable) {
            return ConditionEvaluationResult.disabled(
                "não verificado: MySQL local indisponível na porta 3307");
        }
    }
}
