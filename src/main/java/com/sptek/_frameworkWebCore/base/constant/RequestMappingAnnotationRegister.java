package com.sptek._frameworkWebCore.base.constant;

import com.sptek._frameworkWebCore.util.LoggingUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

// todo: --> 코드 개선이 필요한지 살펴보자
@Slf4j

// 각 RequestMapping 핸들러의 클래스와 메소드에 선언된 프레임워크 어노테이션 및 속성 정보를 HTTP 메소드 + URL 패턴 기준으로 보관한다.
public class RequestMappingAnnotationRegister {
    private static Map<String, Map<String, Map<String, Object>>> requestAnnotationRegister = Collections.emptyMap();
    //OPTIONS, HEAD(body는 없음)는 메소드를 구현하지 않아도 spring 내부에서 자동 생성.
    private final List<String> ALL_HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"/*, "OPTIONS", "HEAD"*/);
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public RequestMappingAnnotationRegister(ApplicationContext applicationContext) {
        synchronized (RequestMappingAnnotationRegister.class) {
            if (!requestAnnotationRegister.isEmpty()) return;

            RequestMappingHandlerMapping requestMappingHandlerMapping = (RequestMappingHandlerMapping) applicationContext.getBean("requestMappingHandlerMapping");
            Map<String, Map<String, Map<String, Object>>> tempRequestAnnotationRegister = new HashMap<>();
            StringBuilder logBodyForPathEmpty = new StringBuilder();
            StringBuilder logBodyForNonSpecificMapping = new StringBuilder();

            requestMappingHandlerMapping.getHandlerMethods().forEach((requestMappingInfo, handlerMethod) -> {
                Set<String> methods;
                if (requestMappingInfo.getMethodsCondition().getMethods().isEmpty()) {
                    methods = new HashSet<>(ALL_HTTP_METHODS);
                    logBodyForNonSpecificMapping.append(requestMappingInfo).append("\n");
                } else {
                    methods = requestMappingInfo.getMethodsCondition().getMethods().stream().map(Enum::name).collect(Collectors.toSet());
                }

                Set<String> patterns = requestMappingInfo.getPathPatternsCondition() != null ?
                        requestMappingInfo.getPathPatternsCondition().getPatternValues() :
                        requestMappingInfo.getDirectPaths();

                Map<String, Map<String, Object>> annotations = getAnnotationsWithAttributes(handlerMethod);

                if (!patterns.isEmpty()) {
                    patterns.forEach(urlPattern -> {
                        methods.forEach(method -> tempRequestAnnotationRegister.put(method + ":" + urlPattern, annotations));
                    });
                } else {
                    logBodyForPathEmpty.append(requestMappingInfo).append("\n");
                }
            });

            requestAnnotationRegister = Map.copyOf(tempRequestAnnotationRegister);
            log.info(LoggingUtil.makeBaseForm(CommonConstants.FW_START_LOG_TAG, "RequestMapping Annotation Register", requestAnnotationRegister.toString()));
            log.info(LoggingUtil.makeBaseForm(CommonConstants.FW_START_LOG_TAG, "No URL Pattern (Potential Mapping Error)", logBodyForPathEmpty.isEmpty() ? "No Error (Good)" : logBodyForPathEmpty.toString()));
            log.info(LoggingUtil.makeBaseForm(CommonConstants.FW_START_LOG_TAG, "Not Specific HTTP Method (Not Recommended)", logBodyForNonSpecificMapping.isEmpty() ? "All Handlers are mapped with Specific HTTP Method (Good)" : logBodyForNonSpecificMapping.toString()));
        }
    }

    // 핸들러 메소드에서 어노테이션과 속성 정보를 가져옴
    private Map<String, Map<String, Object>> getAnnotationsWithAttributes(HandlerMethod handlerMethod) {
        Map<String, Map<String, Object>> annotationData = new HashMap<>();

        // 클래스에 달린 어노테이션 처리
        for (Annotation annotation : handlerMethod.getBeanType().getAnnotations()) {
            if (annotation.annotationType().getPackageName().startsWith(CommonConstants.FRAMEWORK_ANNOTATION_PACKAGE_NAME)) {
                annotationData.put(annotation.annotationType().getName(), extractAnnotationAttributes(annotation));
            }
        }

        // 메소드에 달린 어노테이션 처리 (메소드 부분을 후 처리 함으로 메소드 적용된 내용이 최종 남게 됨, 메소드 적용이 우선순위가 높음으로..)
        for (Annotation annotation : handlerMethod.getMethod().getAnnotations()) {
            if (annotation.annotationType().getPackageName().startsWith(CommonConstants.FRAMEWORK_ANNOTATION_PACKAGE_NAME)) {
                annotationData.put(annotation.annotationType().getName(), extractAnnotationAttributes(annotation));
            }
        }

        return annotationData;
    }

    // 어노테이션의 속성 정보를 추출
    private Map<String, Object> extractAnnotationAttributes(Annotation annotation) {
        Map<String, Object> attributes = new HashMap<>();
        Method[] methods = annotation.annotationType().getDeclaredMethods();

        for (Method method : methods) {
            try {
                attributes.put(method.getName(), method.invoke(annotation));
            } catch (Exception e) {
                log.error("Failed to extract attribute '{}' from annotation '{}'", method.getName(), annotation.annotationType().getName(), e);
            }
        }
        return attributes;
    }

    public static boolean hasAnnotation(HttpServletRequest request, Class<? extends Annotation> annotation) {
        return hasAnnotation(request.getMethod() + ":" + request.getRequestURI(), annotation);
    }

    public static boolean hasAnnotation(String methodAndUrl, Class<? extends Annotation> annotation) {
        // 우선 단순 매칭 후, 패턴 매칭
        if (requestAnnotationRegister.containsKey(methodAndUrl)) {
            return requestAnnotationRegister.get(methodAndUrl).containsKey(annotation.getName());
        } else {
            // request된 url과 완벽히 일치하지 않을 경우 패턴 형식의 결과 일수 있음으로 패턴 매칭을 진행 함 (ex: 요청 /user/sungilry 가 /user/{id} 의 매핑 일수 있음으로)
            return requestAnnotationRegister.entrySet().stream()
                    .anyMatch(entry -> antPathMatcher.match(entry.getKey(), methodAndUrl) && entry.getValue().containsKey(annotation.getName()));
        }
    }

    public static Map<String, Object> getAnnotationAttributes(HttpServletRequest request, Class<? extends Annotation> annotation) {
        return getAnnotationAttributes(request.getMethod() + ":" + request.getRequestURI(), annotation);
    }

    public static Map<String, Object> getAnnotationAttributes(String methodAndUrl, Class<? extends Annotation> annotation) {
        if (requestAnnotationRegister.containsKey(methodAndUrl)) {
            return requestAnnotationRegister.get(methodAndUrl).getOrDefault(annotation.getName(), Map.of());
        } else {
            return requestAnnotationRegister.entrySet().stream()
                    .filter(entry -> antPathMatcher.match(entry.getKey(), methodAndUrl) && entry.getValue().containsKey(annotation.getName()))
                    .findFirst()
                    .map(entry -> entry.getValue().getOrDefault(annotation.getName(), Map.of()))
                    .orElse(Map.of());
        }
    }

    // 현재 요청의 HTTP Method + URI 조합이 등록된 매핑인지 확인한다.
    // URL 자체가 없거나, URL은 있지만 HTTP Method가 맞지 않는 경우 모두 false가 된다
    public static boolean hasRequestMapping(HttpServletRequest request) {
        return hasRequestMapping(request.getMethod(), request.getRequestURI());
    }

    public static boolean hasRequestMapping(String httpMethod, String requestUri) {
        String key = httpMethod + ":" + requestUri;
        if (requestAnnotationRegister.containsKey(key)) {
            return true;
        }
        // 패턴 매칭 (예: GET:/users/{id})
        for (String patternKey : requestAnnotationRegister.keySet()) {
            if (antPathMatcher.match(patternKey, key)) {
                return true;
            }
        }
        return false;
    }
}