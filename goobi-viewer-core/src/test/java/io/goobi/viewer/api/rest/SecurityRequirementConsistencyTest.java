/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.HttpMethod;

import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.filters.AuthorizationFilter;
import io.goobi.viewer.api.rest.filters.UserLoggedInFilter;
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.integration.SwaggerConfiguration;

/**
 * Verifies that the protection enforced by the binding annotations and the requirement published in the OpenAPI spec via
 * {@link SecurityRequirement} stay in sync, along two axes: {@link AuthorizationBinding} against the API token scheme, and the
 * logged-in bindings against the user bearer scheme. Both sides are maintained by hand at the same sites, so nothing but a test keeps
 * them from drifting apart: a new protected endpoint that forgets the requirement is undocumented, and a requirement without
 * protection is a false promise in the published spec.
 *
 * <p>Resource classes are collected with the Swagger scanner rather than by loading class files, because loading initializes them:
 * {@code CORSHeaderFilter} builds a production {@code Configuration} in its static initializer and leaves it in the
 * {@code DataManager} singleton, which would silently corrupt every later test in the same JVM fork.
 */
class SecurityRequirementConsistencyTest {

    /**
     * Resource methods that legitimately publish the token requirement without carrying {@link AuthorizationBinding}, because they
     * enforce it programmatically.
     *
     * <p>{@code TasksResource#addTask} checks per task type: {@code Task.getAccessibility()} demands the token for seven types, an
     * admin session for the rest. Only the token variant is expressible in OpenAPI, so the requirement is declared and the operation
     * description carries the caveat. Its two sibling GETs are deliberately absent here: they filter their result or answer 404
     * instead of rejecting, so a requirement would be a false promise.
     */
    private static final Set<String> TOKEN_REQUIREMENT_WITHOUT_BINDING =
            Set.of("io.goobi.viewer.api.rest.v1.tasks.TasksResource#addTask");

    /**
     * The same exception on the bearer axis: {@code AnnotationResource#deleteAnnotation} rejects anonymous callers
     * (ServiceNotAllowedException, mapped to 403) without carrying a logged-in binding. The sibling endpoints that read the bearer
     * only to identify an optional user are not listed, because they publish no requirement either.
     */
    private static final Set<String> BEARER_REQUIREMENT_WITHOUT_BINDING =
            Set.of("io.goobi.viewer.api.rest.v1.annotations.AnnotationResource#deleteAnnotation");

    /** Binding annotations that enforce the bearer scheme; either grants access, so they share one requirement. */
    private static final Set<String> BEARER_BINDINGS = Set.of("UserLoggedInBinding", "AdminLoggedInBinding");

    private static Set<Class<?>> scanResourceClasses() {
        JaxrsAnnotationScanner<?> scanner = new JaxrsAnnotationScanner<>();
        scanner.setConfiguration(new SwaggerConfiguration().resourcePackages(Set.of("io.goobi.viewer.api.rest")));
        return scanner.classes();
    }

    /** A JAX-RS resource method is one carrying an annotation that is itself meta-annotated with {@link HttpMethod}. */
    private static boolean isResourceMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean requires(SecurityRequirement[] requirements, String scheme) {
        for (SecurityRequirement requirement : requirements) {
            if (scheme.equals(requirement.name())) {
                return true;
            }
        }
        return false;
    }

    private static boolean carriesBinding(Class<?> clazz, Method method, Set<String> bindingNames) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (bindingNames.contains(annotation.annotationType().getSimpleName())) {
                return true;
            }
        }
        for (Annotation annotation : method.getAnnotations()) {
            if (bindingNames.contains(annotation.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> findDisagreements(Set<String> bindingNames, String scheme, Set<String> allowlist) {
        List<String> offenders = new ArrayList<>();
        for (Class<?> clazz : scanResourceClasses()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isResourceMethod(method)) {
                    continue;
                }
                // Effective protection, not annotation placement: binding on the class and requirement on the method is equivalent
                boolean binding = carriesBinding(clazz, method, bindingNames);
                boolean requirement = requires(clazz.getAnnotationsByType(SecurityRequirement.class), scheme)
                        || requires(method.getAnnotationsByType(SecurityRequirement.class), scheme);
                String id = clazz.getName() + "#" + method.getName();
                if (binding == requirement || (!binding && allowlist.contains(id))) {
                    continue;
                }
                offenders.add(id + (binding ? " is protected but publishes no " + scheme + " requirement"
                        : " publishes a " + scheme + " requirement but is not protected"));
            }
        }
        return offenders;
    }

    /**
     * @see AuthorizationBinding
     * @verifies declare the token requirement exactly where the authorization binding applies
     */
    @Test
    void resourceMethods_shouldDeclareTheTokenRequirementExactlyWhereTheBindingApplies() {
        List<String> offenders = findDisagreements(Set.of(AuthorizationBinding.class.getSimpleName()),
                AuthorizationFilter.SECURITY_SCHEME_TOKEN, TOKEN_REQUIREMENT_WITHOUT_BINDING);
        assertTrue(offenders.isEmpty(), "Token protection and published requirement disagree: " + new TreeSet<>(offenders));
    }

    /**
     * @see UserLoggedInFilter
     * @verifies declare the bearer requirement exactly where a logged in binding applies
     */
    @Test
    void resourceMethods_shouldDeclareTheBearerRequirementExactlyWhereALoggedInBindingApplies() {
        List<String> offenders = findDisagreements(BEARER_BINDINGS, UserLoggedInFilter.SECURITY_SCHEME_BEARER,
                BEARER_REQUIREMENT_WITHOUT_BINDING);
        assertTrue(offenders.isEmpty(), "Identity protection and published requirement disagree: " + new TreeSet<>(offenders));
    }

    /**
     * @see AuthorizationBinding
     * @verifies not list obsolete entries in the requirement without binding allowlist
     */
    @Test
    void requirementWithoutBindingAllowlist_shouldNotListObsoleteEntries() {
        Set<String> resourceMethods = new TreeSet<>();
        for (Class<?> clazz : scanResourceClasses()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (isResourceMethod(method)) {
                    resourceMethods.add(clazz.getName() + "#" + method.getName());
                }
            }
        }
        Set<String> allowlisted = new TreeSet<>(TOKEN_REQUIREMENT_WITHOUT_BINDING);
        allowlisted.addAll(BEARER_REQUIREMENT_WITHOUT_BINDING);
        assertTrue(resourceMethods.containsAll(allowlisted),
                "Allowlist entries no longer match a resource method: " + allowlisted + " not all in " + resourceMethods.size()
                        + " scanned methods");
    }
}
