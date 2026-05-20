package com.inneo.aisafecodesync.core.transform;

import com.inneo.aisafecodesync.core.config.ApplyTarget;
import com.inneo.aisafecodesync.core.config.ReplacementRule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReplacementEngine {

    public ReplacementOutcome replace(String input, List<ReplacementRule> rules, ApplyTarget target) {
        String current = input == null ? "" : input;
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (rules == null || rules.isEmpty()) {
            return new ReplacementOutcome(current, counts);
        }
        for (ReplacementRule rule : rules) {
            if (rule.search().isBlank() || !rule.appliesTo(target)) {
                continue;
            }
            ReplacementStep step = rule.regex()
                    ? replaceRegex(current, rule)
                    : replacePlain(current, rule);
            current = step.value();
            if (step.count() > 0) {
                counts.merge(rule.id(), step.count(), Integer::sum);
            }
        }
        return new ReplacementOutcome(current, counts);
    }

    private ReplacementStep replaceRegex(String input, ReplacementRule rule) {
        int flags = rule.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Matcher matcher = Pattern.compile(rule.search(), flags).matcher(input);
        int count = 0;
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            count++;
            matcher.appendReplacement(buffer, rule.replacement());
        }
        matcher.appendTail(buffer);
        return new ReplacementStep(buffer.toString(), count);
    }

    private ReplacementStep replacePlain(String input, ReplacementRule rule) {
        if (rule.caseSensitive()) {
            return replacePlainCaseSensitive(input, rule.search(), rule.replacement());
        }
        Pattern pattern = Pattern.compile(Pattern.quote(rule.search()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(input);
        int count = 0;
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            count++;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rule.replacement()));
        }
        matcher.appendTail(buffer);
        return new ReplacementStep(buffer.toString(), count);
    }

    private ReplacementStep replacePlainCaseSensitive(String input, String search, String replacement) {
        int index = input.indexOf(search);
        if (index < 0) {
            return new ReplacementStep(input, 0);
        }
        StringBuilder builder = new StringBuilder(input.length());
        int count = 0;
        int cursor = 0;
        while (index >= 0) {
            builder.append(input, cursor, index).append(replacement);
            cursor = index + search.length();
            count++;
            index = input.indexOf(search, cursor);
        }
        builder.append(input, cursor, input.length());
        return new ReplacementStep(builder.toString(), count);
    }

    private record ReplacementStep(String value, int count) {
    }
}
