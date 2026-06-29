---
name: spt-code-analysis
description: Analyze SPT Framework Web Core repository code consistently. Use when investigating controllers, services, custom @Enable_* annotations, filters, interceptors, security matchers, profile configuration, resources, request/response behavior, startup errors, or when explaining code based on this repository's AGENT.md and Base code conventions.
---

# SPT Code Analysis

Use this skill for source analysis in the SPT Framework Web Core repository.

## Core Rules

1. Read `AGENT.md` first and follow the Base code priority rules.
2. Start with `git status --short` and preserve unrelated user changes.
3. Use `rg` for classes, URLs, annotations, properties, and call sites. Do not rely on memory for repository facts.
4. Separate conclusions into code-backed facts, explicit inferences, and general guidance.
5. For project-code answers, start the response with `SPT Framework 를 기준으로한 설명입니다`.

## Analysis Workflow

1. Identify the user-facing surface: URL, class, method, annotation, profile key, template, SQL, or log line.
2. Find all direct definitions and callers with `rg`.
3. Open the smallest relevant files with UTF-8 encoding.
4. Trace framework connection points before concluding:
   - `SptWfwApplication` activation annotations
   - custom `@Enable_*` annotations and conditions
   - `_frameworkWebCoreResources/_frameworkApplicationProperties`
   - `_projectCommonResources/_projectApplicationProperties`
   - `_projectCommon` extension implementations
   - `_example` usage examples
5. Report the concrete file and line references that support the answer.

## Controller and Request Analysis

When analyzing a controller, endpoint, or view:

1. Check request mapping, HTTP method, and produced media type.
2. Check controller and method `@Enable_*` annotations.
3. Check common API success/error response wrapping.
4. Check `SecurityFilterChainConfig` and framework security matcher order.
5. Check DTO validation and binding behavior.
6. Check related Thymeleaf templates or static assets when the endpoint returns a view.
7. Check `http-client` examples when the API contract changes.

## Configuration Analysis

When analyzing settings or startup behavior:

1. Read `application.yml` and all relevant `application-{profile}.yml` files.
2. Follow `spring.config.import` into `_frameworkWebCoreResources` and `_projectCommonResources`.
3. For profile-specific changes, review `local`, `dev`, `stg`, and `prd` structure together.
4. Do not guess production or staging secrets.
5. For datasource changes, check `SptWfwApplication`, Gradle DB dependencies, and datasource profile properties together.

## Security and Sensitive Files

1. Do not print contents of `http-client/http-client.private.env.json`.
2. Do not print or replace `src/main/resources/_frameworkWebCoreResources/keystore/keystore.p12`.
3. Treat `infra/h2DB`, DB init SQL, and `infra/mysql-replication` as data-loss-sensitive.

## Output

Keep the answer concise and cite files when the conclusion depends on repository code. If evidence is incomplete, state what was checked and what remains unverified.
