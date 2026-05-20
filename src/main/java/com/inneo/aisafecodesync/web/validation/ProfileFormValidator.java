package com.inneo.aisafecodesync.web.validation;

import com.inneo.aisafecodesync.core.validation.SyncConfigValidator;
import com.inneo.aisafecodesync.core.validation.ValidationResult;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ProfileFormValidator implements Validator {

    private final ProfileMapper profileMapper;
    private final SyncConfigValidator configValidator;

    public ProfileFormValidator(ProfileMapper profileMapper, SyncConfigValidator configValidator) {
        this.profileMapper = profileMapper;
        this.configValidator = configValidator;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ProfileForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        try {
            ValidationResult result = configValidator.validate(profileMapper.toConfig((ProfileForm) target));
            result.errors().forEach(error -> errors.reject("profile.invalid", error));
        } catch (RuntimeException ex) {
            errors.reject("profile.invalid", ex.getMessage());
        }
    }
}
