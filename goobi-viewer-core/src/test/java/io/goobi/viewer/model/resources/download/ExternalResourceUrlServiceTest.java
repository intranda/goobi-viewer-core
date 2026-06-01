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
package io.goobi.viewer.model.resources.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.variables.VariableReplacer;

class ExternalResourceUrlServiceTest {

    /**
     * @verifies return empty map if pi not found
     */
    @Test
    void getAllowedUrls_shouldReturnEmptyMapIfPiNotFound() throws Exception {
        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> null, List.of("https://example.com/{PI}"));
        assertTrue(service.getAllowedUrls("UNKNOWN_PI").isEmpty());
    }

    /**
     * @verifies return empty map if no templates configured
     */
    @Test
    void getAllowedUrls_shouldReturnEmptyMapIfNoTemplatesConfigured() throws Exception {
        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> mockVr, List.of());
        assertTrue(service.getAllowedUrls("PPN123").isEmpty());
    }

    /**
     * @verifies return expanded url to template mapping for known pi
     */
    @Test
    void getAllowedUrls_shouldReturnExpandedUrlToTemplateMappingForKnownPi() throws Exception {
        String template = "https://example.com/resource/{PI}";
        String expanded = "https://example.com/resource/PPN123";

        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        Mockito.when(mockVr.replace(template)).thenReturn(List.of(expanded));

        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> mockVr, List.of(template));
        Map<String, String> result = service.getAllowedUrls("PPN123");

        assertEquals(1, result.size());
        assertEquals(template, result.get(expanded));
    }

    /**
     * @verifies expand templates using provided variable replacer
     */
    @Test
    void getAllowedUrls_withVariableReplacer_shouldExpandTemplatesUsingProvidedVariableReplacer() throws Exception {
        String template = "https://example.com/resource/{PI}";
        String expanded = "https://example.com/resource/PPN123";

        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        Mockito.when(mockVr.replace(template)).thenReturn(List.of(expanded));

        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> mockVr, List.of(template));
        Map<String, String> result = service.getAllowedUrls(mockVr);

        assertEquals(1, result.size());
        assertEquals(template, result.get(expanded));
    }

    /**
     * @verifies call factory only once per pi across multiple calls
     */
    @Test
    void getAllowedUrls_shouldCallFactoryOnlyOncePerPiAcrossMultipleCalls() throws Exception {
        String template = "https://example.com/resource/{PI}";
        String expanded = "https://example.com/resource/PPN123";

        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        Mockito.when(mockVr.replace(template)).thenReturn(List.of(expanded));

        ExternalResourceUrlService.VariableReplacerFactory mockFactory =
                Mockito.mock(ExternalResourceUrlService.VariableReplacerFactory.class);
        Mockito.when(mockFactory.create("PPN123")).thenReturn(mockVr);

        ExternalResourceUrlService service = new ExternalResourceUrlService(mockFactory, List.of(template));
        service.getAllowedUrls("PPN123");
        service.getAllowedUrls("PPN123");
        service.getAllowedUrls("PPN123");

        Mockito.verify(mockFactory, Mockito.times(1)).create("PPN123");
    }

    /**
     * @verifies filter urls by existence check
     */
    @Test
    void getExistingUrls_shouldFilterUrlsByExistenceCheck() throws Exception {
        String template = "https://example.com/resource/{PI}";
        String existingUrl = "https://example.com/resource/PPN123";
        String missingUrl = "https://example.com/resource/PPN456";

        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        Mockito.when(mockVr.replace(template)).thenReturn(List.of(existingUrl, missingUrl));

        @SuppressWarnings("unchecked")
        BiPredicate<String, String> existenceChecker = Mockito.mock(BiPredicate.class);
        Mockito.when(existenceChecker.test(existingUrl, template)).thenReturn(true);
        Mockito.when(existenceChecker.test(missingUrl, template)).thenReturn(false);

        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> mockVr, List.of(template));
        service.setExistenceChecker(existenceChecker);

        Map<String, String> result = service.getExistingUrls(mockVr);

        assertEquals(1, result.size());
        assertEquals(template, result.get(existingUrl));
    }

    /**
     * @verifies cache existence check result across multiple calls
     */
    @Test
    void getExistingUrls_shouldCacheExistenceCheckResultAcrossMultipleCalls() throws Exception {
        String template = "https://example.com/resource/{PI}";
        String existingUrl = "https://example.com/resource/PPN123";

        VariableReplacer mockVr = Mockito.mock(VariableReplacer.class);
        Mockito.when(mockVr.replace(template)).thenReturn(List.of(existingUrl));

        @SuppressWarnings("unchecked")
        BiPredicate<String, String> existenceChecker = Mockito.mock(BiPredicate.class);
        Mockito.when(existenceChecker.test(existingUrl, template)).thenReturn(true);

        ExternalResourceUrlService service = new ExternalResourceUrlService(pi -> mockVr, List.of(template));
        service.setExistenceChecker(existenceChecker);

        service.getExistingUrls(mockVr);
        service.getExistingUrls(mockVr);
        service.getExistingUrls(mockVr);

        Mockito.verify(existenceChecker, Mockito.times(1)).test(existingUrl, template);
    }

}
