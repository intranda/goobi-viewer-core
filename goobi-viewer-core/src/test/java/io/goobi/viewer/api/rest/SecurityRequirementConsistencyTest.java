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
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.integration.SwaggerConfiguration;

/**
 * Verifies that the token protection enforced by {@link AuthorizationBinding} and the token requirement published in the OpenAPI spec
 * via {@link SecurityRequirement} stay in sync. Both are maintained by hand at the same sites, so nothing but a test keeps them from
 * drifting apart: a new protected endpoint that forgets the requirement is undocumented, and a requirement without protection is a
 * false promise in the published spec.
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
    private static final Set<String> REQUIREMENT_WITHOUT_BINDING =
            Set.of("io.goobi.viewer.api.rest.v1.tasks.TasksResource#addTask");

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

    private static boolean requiresToken(SecurityRequirement[] requirements) {
        for (SecurityRequirement requirement : requirements) {
            if (AuthorizationFilter.SECURITY_SCHEME_TOKEN.equals(requirement.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see AuthorizationBinding
     * @verifies declare the token requirement exactly where the authorization binding applies
     */
    @Test
    void resourceMethods_shouldDeclareTheTokenRequirementExactlyWhereTheBindingApplies() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> clazz : scanResourceClasses()) {
            boolean classBinding = clazz.isAnnotationPresent(AuthorizationBinding.class);
            boolean classRequirement = requiresToken(clazz.getAnnotationsByType(SecurityRequirement.class));
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isResourceMethod(method)) {
                    continue;
                }
                // Effective protection, not annotation placement: binding on the class and requirement on the method is equivalent
                boolean binding = classBinding || method.isAnnotationPresent(AuthorizationBinding.class);
                boolean requirement = classRequirement || requiresToken(method.getAnnotationsByType(SecurityRequirement.class));
                String id = clazz.getName() + "#" + method.getName();
                if (binding == requirement || (!binding && REQUIREMENT_WITHOUT_BINDING.contains(id))) {
                    continue;
                }
                offenders.add(id + (binding ? " is protected but publishes no token requirement"
                        : " publishes a token requirement but is not protected"));
            }
        }
        assertTrue(offenders.isEmpty(), "Token protection and published requirement disagree: " + new TreeSet<>(offenders));
    }

    /**
     * @see AuthorizationBinding
     * @verifies not list obsolete entries in the requirement without binding allowlist
     */
    @Test
    void requirementWithoutBindingAllowlist_shouldNotListObsoleteEntries() {
        Set<String> found = new TreeSet<>();
        for (Class<?> clazz : scanResourceClasses()) {
            for (Method method : clazz.getDeclaredMethods()) {
                String id = clazz.getName() + "#" + method.getName();
                if (isResourceMethod(method) && REQUIREMENT_WITHOUT_BINDING.contains(id)
                        && !clazz.isAnnotationPresent(AuthorizationBinding.class)
                        && !method.isAnnotationPresent(AuthorizationBinding.class)) {
                    found.add(id);
                }
            }
        }
        assertTrue(found.containsAll(REQUIREMENT_WITHOUT_BINDING),
                "Allowlist entries no longer match an unbound resource method: " + REQUIREMENT_WITHOUT_BINDING + " vs. found " + found);
    }
}
